package net.digitalingot.feather.serverapi.velocity.event;

import com.velocitypowered.api.event.EventManager;
import java.util.function.Consumer;
import net.digitalingot.feather.serverapi.api.event.EventSubscription;
import net.digitalingot.feather.serverapi.api.event.FeatherEvent;
import org.jetbrains.annotations.NotNull;

public class VelocityEventSubscription<T extends FeatherEvent> implements EventSubscription<T> {

  private final Class<T> event;
  private final Consumer<? super T> handler;
  private final EventManager eventManager;
  private final Object listener;

  public VelocityEventSubscription(
      Class<T> event, Consumer<? super T> handler, EventManager eventManager, Object listener) {
    this.event = event;
    this.handler = handler;
    this.eventManager = eventManager;
    this.listener = listener;
  }

  @Override
  public @NotNull Class<T> getEvent() {
    return event;
  }

  @Override
  public @NotNull Consumer<? super T> getHandler() {
    return handler;
  }

  @Override
  public void unsubscribe() {
    eventManager.unregisterListeners(listener);
  }
}
