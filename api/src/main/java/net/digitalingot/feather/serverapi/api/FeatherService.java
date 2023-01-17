package net.digitalingot.feather.serverapi.api;

import net.digitalingot.feather.serverapi.api.event.EventService;
import net.digitalingot.feather.serverapi.api.mod.FeatherModService;
import net.digitalingot.feather.serverapi.api.player.PlayerService;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointService;
import org.jetbrains.annotations.NotNull;

public interface FeatherService {
  @NotNull
  EventService getEventService();

  @NotNull
  PlayerService getPlayerService();

  @NotNull
  UIService getUIService();

  @NotNull
  WaypointService getWaypointService();

  @NotNull
  FeatherModService getFeatherModService();
}
