package net.digitalingot.feather.serverapi.bukkit.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.model.Platform;
import net.digitalingot.feather.serverapi.api.model.PlatformMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.bukkit.FeatherBukkitPlugin;
import net.digitalingot.feather.serverapi.bukkit.event.player.BukkitPlayerHelloEvent;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitPlayerService;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageConstants;
import net.digitalingot.feather.serverapi.messaging.MessageDecoder;
import net.digitalingot.feather.serverapi.messaging.MessageEncoder;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CHandshake;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SClientHello;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SHandshake;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.Messenger;
import org.jetbrains.annotations.NotNull;

public class BukkitMessagingService implements Listener {
  private static final String CHANNEL = "feather:client";

  @NotNull private final FeatherBukkitPlugin plugin;

  @NotNull private final BukkitPlayerService playerService;
  @NotNull private final RpcService rpcService;
  @NotNull private final Handshaking handshaking;

  public BukkitMessagingService(
      @NotNull FeatherBukkitPlugin plugin,
      @NotNull BukkitPlayerService playerService,
      @NotNull RpcService rpcService) {
    this.plugin = plugin;
    this.playerService = playerService;
    this.rpcService = rpcService;
    this.handshaking = new Handshaking(this, plugin.getLogger());

    Bukkit.getPluginManager().registerEvents(this.handshaking, plugin);

    Messenger messenger = Bukkit.getMessenger();
    messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
    messenger.registerIncomingPluginChannel(plugin, CHANNEL, this::onPluginMessage);
  }

  private void onPluginMessage(String channel, Player player, byte[] message) {
    BukkitFeatherPlayer featherPlayer = this.playerService.getPlayer(player.getUniqueId());

    if (featherPlayer != null) {
      Message<ServerMessageHandler> decodedMessage;

      try {
        decodedMessage = MessageDecoder.SERVER_BOUND.decode(message);
      } catch (Exception exception) {
        exception.printStackTrace();
        return;
      }

      handleMessage(featherPlayer, decodedMessage);
    } else {
      C2SClientHello hello = this.handshaking.handle(player, message);

      if (hello != null) {
        handleHello(player, hello);
      }
    }
  }

  public void callEvent(Event event) {
    this.plugin.getServer().getPluginManager().callEvent(event);
  }

  private void handleHello(Player player, C2SClientHello hello) {
    BukkitFeatherPlayer featherPlayer =
        new BukkitFeatherPlayer(player, this, this.rpcService);
    this.playerService.register(featherPlayer);

    Platform platform;
    switch (hello.getPlatform()) {
      case FABRIC:
        platform = Platform.FABRIC;
        break;
      case FORGE:
        platform = Platform.FORGE;
        break;
      default:
        throw new AssertionError();
    }

    BukkitPlayerHelloEvent helloEvent =
        new BukkitPlayerHelloEvent(
            featherPlayer,
            platform,
            hello.getPlatformMods().stream()
                .map(domain -> new PlatformMod(domain.getName(), domain.getVersion()))
                .collect(Collectors.toList()),
            hello.getFeatherMods().stream()
                .map(domain -> new FeatherMod(domain.getName()))
                .collect(Collectors.toList()));

    this.callEvent(helloEvent);
  }

  private void handleMessage(BukkitFeatherPlayer player, Message<ServerMessageHandler> message) {
    player.handleMessage(message);
  }

  public void sendMessage(BukkitFeatherPlayer player, Message<?> message) {
    sendMessage(player.getPlayer(), message);
  }

  public void sendMessage(Collection<FeatherPlayer> recipients, Message<?> message) {
    if (recipients.isEmpty()) {
      return;
    }
    byte[] encoded = MessageEncoder.CLIENT_BOUND.encode(message);
    for (FeatherPlayer recipient : recipients) {
      ((BukkitFeatherPlayer) recipient)
          .getPlayer()
          .sendPluginMessage(this.plugin, CHANNEL, encoded);
    }
  }

  public void sendMessage(Player player, Message<?> message) {
    byte[] encoded = MessageEncoder.CLIENT_BOUND.encode(message);
    player.sendPluginMessage(this.plugin, CHANNEL, encoded);
  }

  private static class Handshaking implements Listener {
    private final BukkitMessagingService messagingService;
    private final IncompatibleProtocolNotifier incompatibleProtocolNotifier;
    private final Map<UUID, HandshakeState> handshakes = new HashMap<>();

    public Handshaking(BukkitMessagingService messagingService, Logger logger) {
      this.messagingService = messagingService;
      this.incompatibleProtocolNotifier = new IncompatibleProtocolNotifier(logger);
    }

    private HandshakeState getState(Player player) {
      return this.handshakes.getOrDefault(player.getUniqueId(), HandshakeState.EXPECTING_HANDSHAKE);
    }

    private void setState(UUID playerId, HandshakeState state) {
      this.handshakes.put(playerId, state);
    }

    private void accept(Player player) {
      setState(player.getUniqueId(), HandshakeState.EXPECTING_HELLO);
      this.messagingService.sendMessage(player, new S2CHandshake());
    }

    private void reject(Player player) {
      setState(player.getUniqueId(), HandshakeState.REJECTED);
    }

    private void finish(Player player) {
      this.handshakes.remove(player.getUniqueId());
    }

    private C2SClientHello handle(Player player, byte[] data) {
      HandshakeState state = getState(player);

      if (state == HandshakeState.REJECTED) {
        return null;
      }

      Message<ServerMessageHandler> message;
      try {
        message = MessageDecoder.SERVER_BOUND.decode(data);
      } catch (Exception exception) {
        reject(player);
        return null;
      }

      if (state == HandshakeState.EXPECTING_HANDSHAKE) {
        if (handleExpectingHandshake(message)) {
          accept(player);
        } else {
          reject(player);
        }
      } else if (state == HandshakeState.EXPECTING_HELLO) {
        if ((message instanceof C2SClientHello)) {
          finish(player);
          return (C2SClientHello) message;
        }
        reject(player);
      }

      return null;
    }

    private boolean handleExpectingHandshake(Message<ServerMessageHandler> message) {
      if (!(message instanceof C2SHandshake)) {
        return false;
      }
      C2SHandshake handshake = ((C2SHandshake) message);
      int protocolVersion = handshake.getProtocolVersion();
      if (protocolVersion == MessageConstants.VERSION) {
        return true;
      } else {
        if (protocolVersion > MessageConstants.VERSION) {
          this.incompatibleProtocolNotifier.notify(protocolVersion);
        }
        return false;
      }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
      finish(event.getPlayer());
    }

    private enum HandshakeState {
      EXPECTING_HANDSHAKE,
      EXPECTING_HELLO,
      REJECTED
    }

    private static class IncompatibleProtocolNotifier {
      private final Logger logger;
      private boolean notified = false;

      public IncompatibleProtocolNotifier(Logger logger) {
        this.logger = logger;
      }

      private static String formatIncompatibleProtocolMessage(int clientVersion) {
        return String.format(
            "Feather Server API might be out of date. Incompatible protocol version (%d > %d).",
            clientVersion, MessageConstants.VERSION);
      }

      public void notify(int clientVersion) {
        if (!this.notified) {
          this.logger.info(formatIncompatibleProtocolMessage(clientVersion));
          this.notified = true;
        }
      }
    }
  }
}
