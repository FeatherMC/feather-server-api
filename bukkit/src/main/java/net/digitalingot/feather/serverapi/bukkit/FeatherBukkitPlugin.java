package net.digitalingot.feather.serverapi.bukkit;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.bukkit.event.BukkitEventService;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.mod.BukkitFeatherModService;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitPlayerService;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import org.bukkit.plugin.java.JavaPlugin;

public class FeatherBukkitPlugin extends JavaPlugin {
  @Override
  public void onDisable() {
    super.onDisable();
  }

  @Override
  public void onEnable() {
    BukkitEventService eventService = new BukkitEventService(this);
    BukkitPlayerService playerService = new BukkitPlayerService(this);

    RpcService rpcService = new RpcService(this);
    BukkitMessagingService messagingService =
        new BukkitMessagingService(this, playerService, rpcService);
    BukkitUIService uiService = new BukkitUIService(messagingService, rpcService);

    BukkitFeatherModService featherModService = new BukkitFeatherModService(messagingService);

    BukkitFeatherService bukkitFeatherService =
        new BukkitFeatherService(eventService, playerService, uiService, featherModService);
    FeatherAPI.register(bukkitFeatherService);

    super.onEnable();
  }
}
