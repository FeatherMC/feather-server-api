package net.digitalingot.feather.serverapi.api.event.player;

import net.digitalingot.feather.serverapi.api.event.FeatherEvent;

public interface PlayerPressedKeybindEvent extends FeatherEvent {
  int getKeycode();

}
