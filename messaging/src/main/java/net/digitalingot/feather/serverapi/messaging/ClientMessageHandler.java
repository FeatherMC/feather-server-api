package net.digitalingot.feather.serverapi.messaging;

import net.digitalingot.feather.serverapi.messaging.messages.client.*;

public interface ClientMessageHandler extends MessageHandler {
  void handle(S2CCreateFUI createFUI);

  void handle(S2CDestroyFUI destroyFUI);

  void handle(S2CSetFUIState setFUIState);

  void handle(S2CFUIMessage message);

  void handle(S2CFUIResponse response);

  void handle(S2CKeybindCapture captureKeybind);

  void handle(S2CKeybindRelease releaseKeybind);
  
  void handle(S2CPluginModDisable modDisable);

  void handle(S2CPluginModReEnable modReEnable);
}
