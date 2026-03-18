package net.digitalingot.feather.serverapi.bukkit.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackground;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIPage;
import net.digitalingot.feather.serverapi.bukkit.ui.BukkitUIService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CGetEnabledMods;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CServerBackground;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CServerBackground.Action;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SEnabledMods;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUILoadError;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIRequest;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SFUIStateChange;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SRequestServerBackground;
import org.jetbrains.annotations.NotNull;

class PlayerMessageHandler implements ServerMessageHandler {
  private final int REQUEST_TIMEOUT_SECONDS = 30;

  private final BukkitFeatherPlayer player;
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
                  //noinspection ConstantConditions
                  removalNotification.getValue().completeExceptionally(new TimeoutException());
                }
              })
          .build();

  private final AtomicInteger requestId = new AtomicInteger();

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
            new S2CServerBackground(Action.DATA, serverListBackground.getImage()));
        this.sentServerListBackground = true;
      }
    }
  }

  public @NotNull CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> requestEnabledMods() {
    int id = this.requestId.getAndIncrement();
    CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> future = new CompletableFuture<>();
    //noinspection UnstableApiUsage
    this.pendingModsRequests.put(id, future);
    this.player.sendMessage(new S2CGetEnabledMods(id));
    return future;
  }
}
