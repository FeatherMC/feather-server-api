package net.digitalingot.feather.serverapi.velocity.ui;

import com.google.common.collect.Maps;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Map;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.UIPage;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcController;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CCreateFUI;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CDestroyFUI;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CFUIMessage;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetFUIState;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetFUIState.Action;
import net.digitalingot.feather.serverapi.velocity.messaging.VelocityMessagingService;
import net.digitalingot.feather.serverapi.velocity.player.VelocityFeatherPlayer;
import net.digitalingot.feather.serverapi.velocity.ui.rpc.RpcService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityUIService implements UIService {

  @NotNull
  private final VelocityMessagingService messagingService;
  @NotNull
  private final RpcService rpcService;
  @NotNull
  private final ProxyServer server;

  private final Map<String, VelocityUIPage> registeredPages = Maps.newHashMap();

  public VelocityUIService(
      @NotNull VelocityMessagingService messagingService,
      @NotNull RpcService rpcService,
      @NotNull ProxyServer server) {
    this.messagingService = messagingService;
    this.rpcService = rpcService;
    this.server = server;
  }

  private Object validatePlugin(Object plugin) {
    return plugin;
  }

  @Override
  public UIPage registerPage(@NotNull Object pluginObject, @NotNull String url) {
    Object plugin = validatePlugin(pluginObject);
    String pluginId = getPluginId(plugin);

    if (this.registeredPages.containsKey(pluginId)) {
      throw new IllegalArgumentException("Page already exists");
    }

    VelocityUIPage page = new VelocityUIPage(plugin, url);
    this.registeredPages.put(pluginId, page);
    return page;
  }

  private String getPluginId(Object plugin) {
    if (plugin instanceof PluginContainer) {
      return ((PluginContainer) plugin).getDescription().getId();
    } else {
      return plugin.getClass().getSimpleName();
    }
  }

  @Override
  public void unregisterPage(@NotNull UIPage page) {
    Object owner = ((VelocityUIPage) page).getOwner();
    this.registeredPages.remove(getPluginId(owner));
    this.rpcService.unregisterByPlugin(owner);
  }

  @Override
  public void createPageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    VelocityUIPage velocityPage = (VelocityUIPage) page;
    this.messagingService.sendMessage(
        (VelocityFeatherPlayer) player,
        new S2CCreateFUI(velocityPage.getRpcHostname(), velocityPage.getPage()));
  }

  @Override
  public void destroyPageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    this.messagingService.sendMessage(
        (VelocityFeatherPlayer) player,
        new S2CDestroyFUI(((VelocityUIPage) page).getRpcHostname()));
  }

  private void setState(
      @NotNull FeatherPlayer player, @NotNull UIPage page, @NotNull Action action, boolean state) {
    this.messagingService.sendMessage(
        (VelocityFeatherPlayer) player,
        new S2CSetFUIState(((VelocityUIPage) page).getRpcHostname(), action, state));
  }

  @Override
  public void showOverlayForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    setState(player, page, Action.VISIBILITY, true);
  }

  @Override
  public void hideOverlayFromPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    setState(player, page, Action.VISIBILITY, false);
  }

  @Override
  public void openPageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    setState(player, page, Action.FOCUS, true);
  }

  @Override
  public void closePageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    setState(player, page, Action.FOCUS, false);
  }

  @Override
  public void sendPageMessage(
      @NotNull FeatherPlayer player, @NotNull UIPage page, @NotNull String jsonString) {
    this.messagingService.sendMessage(
        (VelocityFeatherPlayer) player,
        new S2CFUIMessage(((VelocityUIPage) page).getRpcHostname(), jsonString));
  }

  @Override
  public void registerCallbacks(@NotNull UIPage page, @NotNull RpcController controller) {
    this.rpcService.register(((VelocityUIPage) page).getOwner(), controller);
  }

  @Override
  public void unregisterCallbacksForController(
      @NotNull UIPage page, @NotNull RpcController controller) {
    this.rpcService.unregisterByController(((VelocityUIPage) page).getOwner(), controller);
  }

  @Override
  public void unregisterCallbacksForPage(@NotNull UIPage page) {
    this.rpcService.unregisterByPlugin(((VelocityUIPage) page).getOwner());
  }

  @Nullable
  public VelocityUIPage getPageByFrame(String frame) {
    return this.registeredPages.get(frame);
  }
}
