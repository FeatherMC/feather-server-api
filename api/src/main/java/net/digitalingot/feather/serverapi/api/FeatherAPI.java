package net.digitalingot.feather.serverapi.api;

import net.digitalingot.feather.serverapi.api.event.EventService;
import net.digitalingot.feather.serverapi.api.mod.FeatherModService;
import net.digitalingot.feather.serverapi.api.player.PlayerService;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointService;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

public final class FeatherAPI {
  @Internal private static FeatherService featherService = null;

  @Internal
  private FeatherAPI() {
    throw new UnsupportedOperationException();
  }

  @Internal
  public static void register(@NotNull FeatherService featherService) {
    if (FeatherAPI.featherService != null) {
      throw new IllegalStateException();
    }

    FeatherAPI.featherService = featherService;
  }

  public static FeatherService getFeatherService() {
    return featherService;
  }

  /**
   * @see FeatherService#getEventService()
   */
  @NotNull
  public static EventService getEventService() {
    return featherService.getEventService();
  }

  /**
   * @see FeatherService#getPlayerService()
   */
  @NotNull
  public static PlayerService getPlayerService() {
    return featherService.getPlayerService();
  }

  /**
   * @see FeatherService#getUIService()
   */
  @NotNull
  public static UIService getUIService() {
    return featherService.getUIService();
  }

  /**
   * @see FeatherService#getWaypointService()
   */
  @NotNull
  public static WaypointService getWaypointService() {
    return featherService.getWaypointService();
  }

  /**
   * @see FeatherService#getFeatherModService() ()
   */
  @NotNull
  public static FeatherModService getFeatherModService() {
    return featherService.getFeatherModService();
  }
}
