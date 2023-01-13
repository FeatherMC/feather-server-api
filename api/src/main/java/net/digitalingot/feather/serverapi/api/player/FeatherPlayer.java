package net.digitalingot.feather.serverapi.api.player;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface FeatherPlayer {

  @NotNull
  UUID getId();

  @NotNull
  String getName();

}
