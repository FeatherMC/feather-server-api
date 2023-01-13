package net.digitalingot.feather.serverapi.api;

import net.digitalingot.feather.serverapi.api.event.EventService;
import net.digitalingot.feather.serverapi.api.keybind.KeybindService;
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
  KeybindService getKeybindService();

  @NotNull
  WaypointService getWaypointService();
}
