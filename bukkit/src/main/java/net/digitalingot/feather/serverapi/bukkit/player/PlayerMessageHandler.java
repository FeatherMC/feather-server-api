package net.digitalingot.feather.serverapi.bukkit.player;

import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.bukkit.event.player.BukkitPlayerPressedKeybind;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIPage;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUILoadError;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIRequest;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIStateChange;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SKeybindPressed;

class PlayerMessageHandler implements ServerMessageHandler {

  private final BukkitFeatherPlayer player;
  private final RpcService rpcService;

  public PlayerMessageHandler(BukkitFeatherPlayer player, RpcService rpcService) {
    this.player = player;
    this.rpcService = rpcService;
  }

  @Override
  public void handle(C2SFUIRequest request) {
    String rpcHost = request.getFrame();
    String rpcPath = request.getPath();
    int requestId = request.getId();
    String payload = request.getPayload();
    this.rpcService.handle(this.player, rpcHost, rpcPath, requestId, payload);
  }

  @Override
  public void handle(C2SFUIStateChange stateChange) {
    BukkitUIService uiService = (BukkitUIService) FeatherAPI.getUIService();

    BukkitUIPage page = uiService.getPageByFrame(stateChange.getFrame());

    if (page == null) {
      return;
    }

    switch (stateChange.getType()) {
      case CREATED:
        page.onCreated(this.player);
        break;
      case DESTROYED:
        page.onDestroyed(this.player);
        break;
      case FOCUS_GAINED:
        page.onFocusGained(this.player);
        break;
      case FOCUS_LOST:
        page.onFocusLost(this.player);
        break;
      case VISIBLE:
        page.onShow(this.player);
        break;
      case INVISIBLE:
        page.onHide(this.player);
        break;
      default:
        break;
    }
  }

  @Override
  public void handle(C2SFUILoadError loadError) {
    BukkitUIService uiService = (BukkitUIService) FeatherAPI.getUIService();

    BukkitUIPage page = uiService.getPageByFrame(loadError.getFrame());

    if (page != null) {
      page.onLoadError(this.player, loadError.getErrorText());
    }
  }

  @Override
  public void handle(C2SKeybindPressed keybindPressed) {
    this.player.callEvent(new BukkitPlayerPressedKeybind(this.player, keybindPressed.getKeycode()));
  }

}
