package net.digitalingot.feather.serverapi.velocity.update;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.digitalingot.feather.serverapi.messaging.MessageConstants;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UpdateNotifier {

  private static final String NOTIFY_PERMISSION = "feather-server-api.notify";
  private static final long NOTIFICATION_DELAY_SECONDS = 3;
  private static final Component NOTIFY_MESSAGE_PREFIX = Component.text(
      "[Feather Server API] Is potentially out of date. Found protocol version: ",
      NamedTextColor.YELLOW);
  private static final Component NOTIFY_MESSAGE_MIDDLE = Component.text(
      ". Plugin protocol version: ", NamedTextColor.YELLOW);

  private final FeatherVelocityPlugin plugin;
  private final ProxyServer server;
  private boolean potentiallyOutOfDate = false;
  private Component outOfDateMessage;

  public UpdateNotifier(FeatherVelocityPlugin plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;
    this.server.getEventManager().register(plugin, this);
  }

  public void setPotentiallyOutOfDate(int protocolVersion) {
    if (!this.potentiallyOutOfDate) {
      this.potentiallyOutOfDate = true;
      this.outOfDateMessage = Component.empty()
          .append(NOTIFY_MESSAGE_PREFIX)
          .append(Component.text(protocolVersion, NamedTextColor.GOLD))
          .append(NOTIFY_MESSAGE_MIDDLE)
          .append(Component.text(MessageConstants.VERSION, NamedTextColor.GRAY));
    }
  }

  @Subscribe
  public void onPlayerJoin(PostLoginEvent event) {
    if (this.potentiallyOutOfDate) {
      Player joiningPlayer = event.getPlayer();
      if (joiningPlayer.hasPermission(NOTIFY_PERMISSION)) {
        final UUID joiningPlayerId = joiningPlayer.getUniqueId();
        server.getScheduler()
            .buildTask(plugin, () -> {
              server.getPlayer(joiningPlayerId)
                  .ifPresent(player -> player.sendMessage(this.outOfDateMessage));
            })
            .delay(NOTIFICATION_DELAY_SECONDS, TimeUnit.SECONDS)
            .schedule();
      }
    }
  }
}
