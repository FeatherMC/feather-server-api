package net.digitalingot.feather.serverapi.bukkit;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.bukkit.event.BukkitEventService;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.meta.BukkitMetaService;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitPlayerService;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.bukkit.update.UpdateNotifier;
import net.digitalingot.feather.serverapi.bukkit.waypoint.BukkitWaypointService;
import org.jetbrains.annotations.NotNull;
import org.bukkit.plugin.java.JavaPlugin;

public class FeatherBukkitPlugin extends JavaPlugin {
  private PlayerTaskScheduler playerTaskScheduler;

  @Override
  public void onDisable() {
    super.onDisable();
  }

  @Override
  public void onEnable() {
    this.playerTaskScheduler = new PlayerTaskScheduler(this);

    BukkitEventService eventService = new BukkitEventService(this);
    BukkitPlayerService playerService = new BukkitPlayerService(this);

    RpcService rpcService = new RpcService(this);
    BukkitMessagingService messagingService =
        new BukkitMessagingService(this, playerService, rpcService, new UpdateNotifier(this));
    BukkitUIService uiService = new BukkitUIService(messagingService, rpcService);

    BukkitWaypointService waypointService = new BukkitWaypointService(this, playerService);
    BukkitMetaService metaService = new BukkitMetaService(this);

    BukkitFeatherService bukkitFeatherService =
        new BukkitFeatherService(
            eventService, playerService, uiService, waypointService, metaService);
    FeatherAPI.register(bukkitFeatherService);

    super.onEnable();
  }

  public @NotNull PlayerTaskScheduler getPlayerTaskScheduler() {
    if (this.playerTaskScheduler == null) {
      throw new IllegalStateException("Player task scheduler not initialized");
    }
    return this.playerTaskScheduler;
  }
}
