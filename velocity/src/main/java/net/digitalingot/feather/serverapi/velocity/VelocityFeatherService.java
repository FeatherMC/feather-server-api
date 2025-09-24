package net.digitalingot.feather.serverapi.velocity;

import net.digitalingot.feather.serverapi.api.FeatherService;
import net.digitalingot.feather.serverapi.api.event.EventService;
import net.digitalingot.feather.serverapi.api.meta.MetaService;
import net.digitalingot.feather.serverapi.api.player.PlayerService;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.api.waypoint.WaypointService;
import net.digitalingot.feather.serverapi.velocity.event.VelocityEventService;
import net.digitalingot.feather.serverapi.velocity.meta.VelocityMetaService;
import net.digitalingot.feather.serverapi.velocity.player.VelocityPlayerService;
import net.digitalingot.feather.serverapi.velocity.ui.VelocityUIService;
import org.jetbrains.annotations.NotNull;

public class VelocityFeatherService implements FeatherService {

  @NotNull
  private final VelocityEventService eventService;
  @NotNull
  private final VelocityPlayerService playerService;
  @NotNull
  private final VelocityUIService uiService;
  @NotNull
  private final VelocityMetaService metaService;

  public VelocityFeatherService(
      @NotNull VelocityEventService eventService,
      @NotNull VelocityPlayerService playerService,
      @NotNull VelocityUIService uiService,
      @NotNull VelocityMetaService metaService) {
    this.eventService = eventService;
    this.playerService = playerService;
    this.uiService = uiService;
    this.metaService = metaService;
  }

  @Override
  public @NotNull EventService getEventService() {
    return this.eventService;
  }

  @Override
  public @NotNull PlayerService getPlayerService() {
    return this.playerService;
  }

  @Override
  public @NotNull UIService getUIService() {
    return this.uiService;
  }

  @Override
  public @NotNull WaypointService getWaypointService() {
    return null;
  }

  @Override
  public @NotNull MetaService getMetaService() {
    return this.metaService;
  }
}
