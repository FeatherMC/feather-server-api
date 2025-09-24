package net.digitalingot.feather.serverapi.velocity.event.player;

import java.util.Collection;
import java.util.Collections;
import net.digitalingot.feather.serverapi.api.event.player.PlayerHelloEvent;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.model.Platform;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.velocity.event.VelocityFeatherEvent;
import org.jetbrains.annotations.NotNull;

public class VelocityPlayerHelloEvent extends VelocityFeatherEvent implements PlayerHelloEvent {

  @NotNull
  private final Platform platform;
  @NotNull
  private final Collection<FeatherMod> featherMods;

  public VelocityPlayerHelloEvent(
      @NotNull FeatherPlayer player,
      @NotNull Platform platform,
      @NotNull Collection<FeatherMod> featherMods) {
    super(player);
    this.platform = platform;
    this.featherMods = featherMods;
  }

  @NotNull
  @Override
  public Platform getPlatform() {
    return this.platform;
  }

  @NotNull
  @Override
  public Collection<FeatherMod> getFeatherMods() {
    return Collections.unmodifiableCollection(this.featherMods);
  }
}
