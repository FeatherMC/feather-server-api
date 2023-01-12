package net.digitalingot.feather.serverapi.bukkit.event.player;

import net.digitalingot.feather.serverapi.api.event.player.PlayerHelloEvent;
import net.digitalingot.feather.serverapi.api.event.player.PlayerPressedKeybindEvent;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.model.Platform;
import net.digitalingot.feather.serverapi.api.model.PlatformMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.event.BukkitFeatherEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class BukkitPlayerPressedKeybind extends BukkitFeatherEvent implements PlayerPressedKeybindEvent {
  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final int keycode;

  public BukkitPlayerPressedKeybind(
      @NotNull FeatherPlayer player,
      int keycode
  ) {
    super(player);
    this.keycode = keycode;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public int getKeycode() {
    return keycode;
  }

  @Override
  public HandlerList getHandlers() {
    return getHandlerList();
  }
}
