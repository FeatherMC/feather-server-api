package net.digitalingot.feather.serverapi.bukkit.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.UIPage;
import net.digitalingot.feather.serverapi.api.ui.UIService;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcController;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CCreateFUI;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CDestroyFUI;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CFUIMessage;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetFUIState;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetFUIState.Action;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitUIService implements UIService {

  @NotNull private final BukkitMessagingService messagingService;
  @NotNull private final RpcService rpcService;

  private final Map<String, BukkitUIPage> registeredPages = new ConcurrentHashMap<>();

  public BukkitUIService(
      @NotNull BukkitMessagingService messagingService, @NotNull RpcService rpcService) {
    this.messagingService = messagingService;
    this.rpcService = rpcService;
  }

  private static Plugin validatePlugin(Object plugin) {
    if (!(plugin instanceof Plugin)) {
      throw new IllegalArgumentException("Invalid plugin object");
    }
    return (Plugin) plugin;
  }

  @Override
  public UIPage registerPage(@NotNull Object pluginObject, @NotNull String url) {
    Plugin plugin = validatePlugin(pluginObject);
    String pluginName = plugin.getName();
    BukkitUIPage page = new BukkitUIPage(plugin, url);
    if (this.registeredPages.putIfAbsent(pluginName, page) != null) {
      throw new IllegalArgumentException("Page already exists");
    }
    return page;
  }

  @Override
  public void unregisterPage(@NotNull UIPage page) {
    Plugin owner = ((BukkitUIPage) page).getOwner();
    this.registeredPages.remove(owner.getName());
    this.rpcService.unregisterByPlugin(owner);
  }

  @Override
  public void createPageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    BukkitUIPage bukkitPage = (BukkitUIPage) page;
    this.messagingService.sendMessage(
        (BukkitFeatherPlayer) player,
        new S2CCreateFUI(bukkitPage.getRpcHostname(), bukkitPage.getPage()));
  }

  @Override
  public void destroyPageForPlayer(@NotNull FeatherPlayer player, @NotNull UIPage page) {
    this.messagingService.sendMessage(
        (BukkitFeatherPlayer) player, new S2CDestroyFUI(((BukkitUIPage) page).getRpcHostname()));
  }

  private void setState(
      @NotNull FeatherPlayer player, @NotNull UIPage page, @NotNull Action action, boolean state) {
    this.messagingService.sendMessage(
        (BukkitFeatherPlayer) player,
        new S2CSetFUIState(((BukkitUIPage) page).getRpcHostname(), action, state));
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
        (BukkitFeatherPlayer) player,
        new S2CFUIMessage(((BukkitUIPage) page).getRpcHostname(), jsonString));
  }

  @Override
  public void registerCallbacks(@NotNull UIPage page, @NotNull RpcController controller) {
    this.rpcService.register(((BukkitUIPage) page).getOwner(), controller);
  }

  @Override
  public void unregisterCallbacksForController(
      @NotNull UIPage page, @NotNull RpcController controller) {
    this.rpcService.unregisterByController(((BukkitUIPage) page).getOwner(), controller);
  }

  @Override
  public void unregisterCallbacksForPage(@NotNull UIPage page) {
    this.rpcService.unregisterByPlugin(((BukkitUIPage) page).getOwner());
  }

  @Nullable
  public BukkitUIPage getPageByFrame(String frame) {
    return this.registeredPages.get(frame);
  }
}
