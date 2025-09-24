package net.digitalingot.feather.serverapi.velocity.event;

import net.digitalingot.feather.serverapi.api.event.FeatherEvent;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import org.jetbrains.annotations.NotNull;

public abstract class VelocityFeatherEvent implements FeatherEvent {

  private final FeatherPlayer player;

  public VelocityFeatherEvent(@NotNull FeatherPlayer player) {
    this.player = player;
  }

  @NotNull
  @Override
  public FeatherPlayer getPlayer() {
    return this.player;
  }
}
