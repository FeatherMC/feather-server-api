package net.digitalingot.feather.serverapi.bukkit.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.ui.rpc.RpcService;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CMissPenaltyState;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CModsAction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class BukkitFeatherPlayer implements FeatherPlayer {

  @NotNull private final Player player;
  @NotNull private final BukkitMessagingService messagingService;
  @NotNull private final PlayerMessageHandler messageHandler;
  private final Set<FeatherMod> blockedMods = ConcurrentHashMap.newKeySet();

  public BukkitFeatherPlayer(
      @NotNull Player player,
      @NotNull BukkitMessagingService messagingService,
      @NotNull RpcService rpcService) {
    this.player = player;
    this.messagingService = messagingService;
    this.messageHandler = new PlayerMessageHandler(this, rpcService);
  }

  public @NotNull Player getPlayer() {
    return this.player;
  }

  @Override
  public @NotNull UUID getUniqueId() {
    return this.player.getUniqueId();
  }

  @Override
  public @NotNull String getName() {
    return this.player.getName();
  }

  @Override
  public void blockMods(@NotNull Collection<@NotNull FeatherMod> mods) {
    this.blockedMods.addAll(mods);
    sendModsAction(S2CModsAction.Action.BLOCK, mods);
  }

  @Override
  public void unblockMods(@NotNull Collection<@NotNull FeatherMod> mods) {
    this.blockedMods.removeAll(mods);
    sendModsAction(S2CModsAction.Action.UNBLOCK, mods);
  }

  @Override
  public CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> getBlockedMods() {
    return CompletableFuture.completedFuture(
        Collections.unmodifiableSet(new HashSet<>(this.blockedMods)));
  }

  @Override
  public void enableMods(@NotNull Collection<@NotNull FeatherMod> mods) {
    sendModsAction(S2CModsAction.Action.ENABLE, mods);
  }

  @Override
  public void disableMods(@NotNull Collection<@NotNull FeatherMod> mods) {
    sendModsAction(S2CModsAction.Action.DISABLE, mods);
  }

  @Override
  public @NotNull CompletableFuture<@NotNull Collection<@NotNull FeatherMod>> getEnabledMods() {
    return this.messageHandler.requestEnabledMods();
  }

  @Override
  public void bypassMissPenalty(boolean enabled) {
    sendMessage(new S2CMissPenaltyState(!enabled));
  }

  private void sendModsAction(
      S2CModsAction.Action action, @NotNull Collection<@NotNull FeatherMod> mods) {
    if (!mods.isEmpty()) {
      sendMessage(new S2CModsAction(action, mapFeatherModsToDomain(mods)));
    }
  }

  private static Collection<net.digitalingot.feather.serverapi.messaging.domain.FeatherMod>
      mapFeatherModsToDomain(Collection<FeatherMod> mods) {
    return mods.stream()
        .map(
            mod ->
                new net.digitalingot.feather.serverapi.messaging.domain.FeatherMod(mod.getName()))
        .collect(Collectors.toSet());
  }

  public void sendMessage(@NotNull Message<?> message) {
    this.messagingService.sendMessage(this.player, message);
  }

  public void handleMessage(@NotNull Message<ServerMessageHandler> message) {
    message.handle(this.messageHandler);
  }

  public void callEvent(Event event) {
    this.messagingService.callEvent(event);
  }
}
