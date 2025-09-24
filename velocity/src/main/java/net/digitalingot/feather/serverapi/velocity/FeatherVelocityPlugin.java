package net.digitalingot.feather.serverapi.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.velocity.event.VelocityEventService;
import net.digitalingot.feather.serverapi.velocity.messaging.VelocityMessagingService;
import net.digitalingot.feather.serverapi.velocity.meta.VelocityMetaService;
import net.digitalingot.feather.serverapi.velocity.player.VelocityPlayerService;
import net.digitalingot.feather.serverapi.velocity.ui.VelocityUIService;
import net.digitalingot.feather.serverapi.velocity.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.velocity.update.UpdateNotifier;
import org.slf4j.Logger;

public class FeatherVelocityPlugin {

  private final ProxyServer server;
  private final Logger logger;

  @Inject
  public FeatherVelocityPlugin(ProxyServer server, Logger logger) {
    this.server = server;
    this.logger = logger;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    VelocityEventService eventService = new VelocityEventService(this, server);
    VelocityPlayerService playerService = new VelocityPlayerService(this, server);

    RpcService rpcService = new RpcService(this, server);
    UpdateNotifier updateNotifier = new UpdateNotifier(this, server);
    VelocityMessagingService messagingService =
        new VelocityMessagingService(this, server, playerService, rpcService, updateNotifier);
    VelocityUIService uiService = new VelocityUIService(messagingService, rpcService, server);

    VelocityMetaService metaService = new VelocityMetaService(this);
    VelocityFeatherService velocityFeatherService =
        new VelocityFeatherService(eventService, playerService, uiService, metaService);
    FeatherAPI.register(velocityFeatherService);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {

  }

  public ProxyServer getServer() {
    return server;
  }

  public Logger getLogger() {
    return logger;
  }
}
