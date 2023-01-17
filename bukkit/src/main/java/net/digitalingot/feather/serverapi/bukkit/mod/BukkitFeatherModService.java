package net.digitalingot.feather.serverapi.bukkit.mod;

import net.digitalingot.feather.serverapi.api.mod.FeatherModService;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.messaging.BukkitMessagingService;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CPluginModDisable;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CPluginModReEnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class BukkitFeatherModService implements FeatherModService {

  Map<FeatherPlayer, Set<FeatherMod>> playerDisabledMods = new HashMap<>();

  BukkitMessagingService messagingService;

  public BukkitFeatherModService(BukkitMessagingService messagingService) {
    this.messagingService = messagingService;
  }

  @Override
  public void disableMods(@NotNull FeatherPlayer player, @NotNull List<FeatherMod> mods) {
    Set<FeatherMod> disabledMods = playerDisabledMods.computeIfAbsent(player, k -> new HashSet<>());
    disabledMods.addAll(mods);

    messagingService.sendMessage((BukkitFeatherPlayer) player, new S2CPluginModDisable(mods.stream().map(
            (featherMod -> new net.digitalingot.feather.serverapi.messaging.domain.FeatherMod(featherMod.getName()))).collect(Collectors.toList())));
  }

  @Override
  public void reEnableMods(@NotNull FeatherPlayer player, @NotNull List<FeatherMod> mods) {
    Set<FeatherMod> disabledMods = playerDisabledMods.getOrDefault(player, new HashSet<>());
    mods.forEach(disabledMods::remove);

    if (disabledMods.isEmpty()) playerDisabledMods.remove(player);
    messagingService.sendMessage((BukkitFeatherPlayer) player, new S2CPluginModReEnable(mods.stream().map(
            (featherMod -> new net.digitalingot.feather.serverapi.messaging.domain.FeatherMod(featherMod.getName()))).collect(Collectors.toList())));
  }

  @Override
  public List<FeatherMod> getDisabledMods(@NotNull FeatherPlayer player) {
    return playerDisabledMods.getOrDefault(player, new HashSet<>()).stream().collect(Collectors.toList());
  }
}
