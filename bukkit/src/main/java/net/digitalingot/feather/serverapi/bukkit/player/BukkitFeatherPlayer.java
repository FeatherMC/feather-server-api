package net.digitalingot.feather.serverapi.bukkit.player;

import java.util.UUID;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class BukkitFeatherPlayer implements FeatherPlayer {
  @NotNull private final Player player;
  @NotNull private final BukkitMessagingService messagingService;
  @NotNull private final PlayerMessageHandler messageHandler;

  public BukkitFeatherPlayer(
      @NotNull Player player,
      @NotNull BukkitMessagingService messagingService,
      @NotNull RpcService rpcService) {
    this.player = player;
    this.messagingService = messagingService;
    this.messageHandler = new PlayerMessageHandler(this, rpcService);
  }

  public @NotNull Player getPlayer() {
    return this.player;
  }

  @Override
  public @NotNull UUID getId() {
    return this.player.getUniqueId();
  }

  @Override
  public @NotNull String getName() {
    return this.player.getName();
  }

  public void sendMessage(@NotNull Message<?> message) {
    this.messagingService.sendMessage(this.player, message);
  }

  public void handleMessage(@NotNull Message<ServerMessageHandler> message) {
    message.handle(this.messageHandler);
  }
  public void callEvent(Event event) {
    messagingService.callEvent(event);
  }

}
