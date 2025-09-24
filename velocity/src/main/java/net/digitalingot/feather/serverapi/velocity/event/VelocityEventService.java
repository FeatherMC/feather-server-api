package net.digitalingot.feather.serverapi.velocity.event;

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Map;
import java.util.function.Consumer;
import net.digitalingot.feather.serverapi.api.event.EventService;
import net.digitalingot.feather.serverapi.api.event.EventSubscription;
import net.digitalingot.feather.serverapi.api.event.FeatherEvent;
import net.digitalingot.feather.serverapi.api.event.player.PlayerHelloEvent;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.digitalingot.feather.serverapi.velocity.event.player.VelocityPlayerHelloEvent;
import org.jetbrains.annotations.NotNull;

public class VelocityEventService implements EventService {

  private static final Map<Class<? extends FeatherEvent>, Class<? extends VelocityFeatherEvent>>
      MAPPING =
      ImmutableMap.<Class<? extends FeatherEvent>, Class<? extends VelocityFeatherEvent>>builder()
          .put(PlayerHelloEvent.class, VelocityPlayerHelloEvent.class)
          .build();

  private final FeatherVelocityPlugin plugin;
  private final ProxyServer server;

  public VelocityEventService(FeatherVelocityPlugin plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;
  }

  @Override
  public @NotNull <T extends FeatherEvent> EventSubscription<T> subscribe(
      @NotNull Class<T> eventClazz, @NotNull Consumer<? super T> handler) {
    return subscribe(eventClazz, handler, this.plugin);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull <T extends FeatherEvent> EventSubscription<T> subscribe(
      @NotNull Class<T> eventClazz, @NotNull Consumer<? super T> handler, @NotNull Object plugin) {
    final Class<? extends VelocityFeatherEvent> velocityEventClass = MAPPING.get(eventClazz);
    final EventManager eventManager = server.getEventManager();
    final Object listener = new Object();
    eventManager.register(
        plugin,
        velocityEventClass,
        event -> {
          try {
            handler.accept((T) event);
          } catch (Throwable throwable) {
            throwable.printStackTrace();
          }
        });

    return new VelocityEventSubscription<>(
        (Class<T>) velocityEventClass, handler, eventManager, listener);
  }


}
