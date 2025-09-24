package net.digitalingot.feather.serverapi.velocity.event.player;

import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.velocity.event.VelocityFeatherEvent;
import org.jetbrains.annotations.NotNull;

public class FeatherPlayerQuitEvent extends VelocityFeatherEvent {

  public FeatherPlayerQuitEvent(@NotNull FeatherPlayer player) {
    super(player);
  }
}
