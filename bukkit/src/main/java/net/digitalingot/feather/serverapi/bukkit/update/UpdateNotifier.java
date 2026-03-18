package net.digitalingot.feather.serverapi.bukkit.update;

import java.util.UUID;
import net.digitalingot.feather.serverapi.bukkit.FeatherBukkitPlugin;
import net.digitalingot.feather.serverapi.messaging.MessageConstants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifier implements Listener {
  private static final String NOTIFY_PERMISSION = "feather-server-api.notify";
  private static final int TICKS_PER_SECOND = 20;
  private static final long NOTIFICATION_DELAY_IN_TICKS = 3 * TICKS_PER_SECOND;
  private static final String NOTIFY_MESSAGE_FORMAT =
      ChatColor.YELLOW
          + "[Feather Server API] Is potentially out of date. Found protocol version: "
          + ChatColor.GOLD
          + "%d"
          + ChatColor.YELLOW
          + ". Plugin protocol version: "
          + ChatColor.GRAY
          + "%d";

  private final FeatherBukkitPlugin plugin;
  private volatile boolean potentiallyOutOfDate = false;
  private volatile String outOfDateMessage;

  public UpdateNotifier(FeatherBukkitPlugin plugin) {
    this.plugin = plugin;
    this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  public void setPotentiallyOutOfDate(int protocolVersion) {
    if (!this.potentiallyOutOfDate) {
      this.potentiallyOutOfDate = true;
      this.outOfDateMessage =
          String.format(NOTIFY_MESSAGE_FORMAT, protocolVersion, MessageConstants.VERSION);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (this.potentiallyOutOfDate) {
      Player joiningPlayer = event.getPlayer();
      if (joiningPlayer.hasPermission(NOTIFY_PERMISSION)) {
        final UUID joiningPlayerId = joiningPlayer.getUniqueId();
        this.plugin
            .getPlayerTaskScheduler()
            .executeLater(
                joiningPlayer,
                NOTIFICATION_DELAY_IN_TICKS,
                () -> {
                  Player player = Bukkit.getPlayer(joiningPlayerId);
                  if (player != null) {
                    player.sendMessage(this.outOfDateMessage);
                  }
                });
      }
    }
  }
}
