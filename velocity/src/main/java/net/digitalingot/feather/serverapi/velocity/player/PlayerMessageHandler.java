package net.digitalingot.feather.serverapi.velocity.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackground;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CGetEnabledMods;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CServerBackground;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SEnabledMods;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUILoadError;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIRequest;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIStateChange;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SRequestServerBackground;
import net.digitalingot.feather.serverapi.velocity.ui.VelocityUIPage;
import net.digitalingot.feather.serverapi.velocity.ui.VelocityUIService;
import net.digitalingot.feather.serverapi.velocity.ui.rpc.RpcService;
import org.jetbrains.annotations.NotNull;

class PlayerMessageHandler implements ServerMessageHandler {

  private final int REQUEST_TIMEOUT_SECONDS = 30;

  private final VelocityFeatherPlayer player;
  private final RpcService rpcService;

  private boolean sentServerListBackground = false;

  @SuppressWarnings("UnstableApiUsage")
  private final Cache<Integer, CompletableFuture<Collection<FeatherMod>>> pendingModsRequests =
      CacheBuilder.newBuilder()
          .expireAfterWrite(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .<Integer, CompletableFuture<Collection<FeatherMod>>>removalListener(
              removalNotification -> {
                RemovalCause cause = removalNotification.getCause();
                if (cause != RemovalCause.EXPLICIT && cause != RemovalCause.REPLACED) {
                  removalNotification.getValue().completeExceptionally(new TimeoutException());
                }
              })
          .build();

  private int requestId = 0;

  public PlayerMessageHandler(VelocityFeatherPlayer player, RpcService rpcService) {
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
    VelocityUIService uiService = (VelocityUIService) FeatherAPI.getUIService();

    VelocityUIPage page = uiService.getPageByFrame(stateChange.getFrame());

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
    VelocityUIService uiService = (VelocityUIService) FeatherAPI.getUIService();
    VelocityUIPage page = uiService.getPageByFrame(loadError.getFrame());

    if (page != null) {
      page.onLoadError(this.player, loadError.getErrorText());
    }
  }


  @Override
  public void handle(C2SEnabledMods enabledMods) {
    @SuppressWarnings("UnstableApiUsage")
    CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> future =
        this.pendingModsRequests.asMap().remove(enabledMods.getId());
    if (future != null) {
      future.complete(
          enabledMods.getMods().stream()
              .map(mod -> new FeatherMod(mod.getName()))
              .collect(Collectors.toList()));
    }
  }

  @Override
  public void handle(C2SRequestServerBackground serverBackground) {
    if (!this.sentServerListBackground) {
      ServerListBackground serverListBackground =
          FeatherAPI.getMetaService().getServerListBackground();
      if (serverListBackground != null) {
        this.player.sendMessage(
            new S2CServerBackground(S2CServerBackground.Action.DATA,
                serverListBackground.getImage()));
        this.sentServerListBackground = true;
      }
    }
  }

  public @NotNull CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> requestEnabledMods() {
    int id = this.requestId++;
    CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> future = new CompletableFuture<>();
    this.pendingModsRequests.put(id, future);
    this.player.sendMessage(new S2CGetEnabledMods(id));
    return future;
  }
}
