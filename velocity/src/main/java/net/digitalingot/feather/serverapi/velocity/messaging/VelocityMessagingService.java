package net.digitalingot.feather.serverapi.velocity.messaging;

import com.google.common.collect.Maps;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.model.Platform;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageConstants;
import net.digitalingot.feather.serverapi.messaging.MessageDecoder;
import net.digitalingot.feather.serverapi.messaging.MessageEncoder;
import net.digitalingot.feather.serverapi.messaging.MessageFragmenter;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CHandshake;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SClientHello;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SHandshake;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.digitalingot.feather.serverapi.velocity.event.player.VelocityPlayerHelloEvent;
import net.digitalingot.feather.serverapi.velocity.player.VelocityFeatherPlayer;
import net.digitalingot.feather.serverapi.velocity.player.VelocityPlayerService;
import net.digitalingot.feather.serverapi.velocity.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.velocity.update.UpdateNotifier;
import org.jetbrains.annotations.NotNull;

public class VelocityMessagingService {

  static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(
      "feather:client");
  static final ChannelIdentifier CHANNEL_FRAGMENTED = MinecraftChannelIdentifier.from(
      "feather:client/frag");

  @NotNull
  final FeatherVelocityPlugin plugin;
  @NotNull
  final ProxyServer server;
  @NotNull
  final VelocityPlayerService playerService;
  @NotNull
  final RpcService rpcService;
  @NotNull
  final Handshaking handshaking;

  public VelocityMessagingService(
      @NotNull FeatherVelocityPlugin plugin,
      @NotNull ProxyServer server,
      @NotNull VelocityPlayerService playerService,
      @NotNull RpcService rpcService,
      @NotNull UpdateNotifier updateNotifier) {
    this.plugin = plugin;
    this.server = server;
    this.playerService = playerService;
    this.rpcService = rpcService;
    this.handshaking = new Handshaking(this, updateNotifier);
    server.getEventManager().register(plugin, this.handshaking);
    server.getChannelRegistrar().register(CHANNEL, CHANNEL_FRAGMENTED);
    server.getEventManager().register(plugin, this);
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    event.setResult(PluginMessageEvent.ForwardResult.handled());

    if (!(event.getSource() instanceof Player player)) {
      return;
    }

    if (!event.getIdentifier().equals(CHANNEL)) {
      return;
    }

    VelocityFeatherPlayer featherPlayer = this.playerService.getPlayer(player.getUniqueId());

    if (featherPlayer != null) {
      Message<ServerMessageHandler> decodedMessage;

      try {
        decodedMessage = MessageDecoder.SERVER_BOUND.decode(event.getData());
      } catch (Exception exception) {
        exception.printStackTrace();
        return;
      }

      handleMessage(featherPlayer, decodedMessage);
    } else {
      C2SClientHello hello = this.handshaking.handle(player, event.getData());

      if (hello != null) {
        handleHello(player, hello);
      }
    }
  }

  public void fireEvent(Object event) {
    this.server.getEventManager().fireAndForget(event);
  }

  private void handleHello(Player player, C2SClientHello hello) {
    VelocityFeatherPlayer featherPlayer = new VelocityFeatherPlayer(player, this, this.rpcService);
    this.playerService.register(featherPlayer);

    Platform platform = switch (hello.getPlatform()) {
      case FABRIC -> Platform.FABRIC;
      case FORGE -> Platform.FORGE;
    };

    VelocityPlayerHelloEvent helloEvent =
        new VelocityPlayerHelloEvent(
            featherPlayer,
            platform,
            hello.getFeatherMods().stream()
                .map(domain -> new FeatherMod(domain.getName()))
                .collect(Collectors.toList()));

    this.fireEvent(helloEvent);
  }

  private void handleMessage(VelocityFeatherPlayer player, Message<ServerMessageHandler> message) {
    player.handleMessage(message);
  }

  public void sendMessage(VelocityFeatherPlayer player, Message<?> message) {
    sendMessage(player.getPlayer(), message);
  }

  public void sendMessage(Collection<FeatherPlayer> recipients, Message<?> message) {
    if (recipients.isEmpty()) {
      return;
    }

    byte[] encoded = MessageEncoder.CLIENT_BOUND.encode(message);
    if (encoded.length > 32767) {
      List<byte[]> fragments = MessageFragmenter.CLIENT_BOUND.fragment(message);
      for (FeatherPlayer recipient : recipients) {
        for (byte[] data : fragments) {
          sendPluginMessage(
              ((VelocityFeatherPlayer) recipient).getPlayer(), CHANNEL_FRAGMENTED, data);
        }
      }
    } else {
      for (FeatherPlayer recipient : recipients) {
        sendPluginMessage(((VelocityFeatherPlayer) recipient).getPlayer(), CHANNEL, encoded);
      }
    }
  }

  public void sendMessage(Player player, Message<?> message) {
    byte[] encoded = MessageEncoder.CLIENT_BOUND.encode(message);

    if (encoded.length > 32767) {
      for (byte[] data : MessageFragmenter.CLIENT_BOUND.fragment(message)) {
        sendPluginMessage(player, CHANNEL_FRAGMENTED, data);
      }
    } else {
      sendPluginMessage(player, CHANNEL, encoded);
    }
  }

  private void sendPluginMessage(@NotNull Player player, @NotNull ChannelIdentifier channel,
      byte[] data) {
    player.sendPluginMessage(channel, data);
  }

  private static class Handshaking {

    private final VelocityMessagingService messagingService;
    private final Map<UUID, HandshakeState> handshakes = Maps.newHashMap();
    private final UpdateNotifier updateNotifier;

    public Handshaking(VelocityMessagingService messagingService, UpdateNotifier updateNotifier) {
      this.messagingService = messagingService;
      this.updateNotifier = updateNotifier;
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
      if (!(message instanceof C2SHandshake handshake)) {
        return false;
      }
      int protocolVersion = handshake.getProtocolVersion();
      if (protocolVersion > MessageConstants.VERSION) {
        this.updateNotifier.setPotentiallyOutOfDate(protocolVersion);
      }
      return true;
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
      finish(event.getPlayer());
    }

    private enum HandshakeState {
      EXPECTING_HANDSHAKE,
      EXPECTING_HELLO,
      REJECTED
    }
  }
}
