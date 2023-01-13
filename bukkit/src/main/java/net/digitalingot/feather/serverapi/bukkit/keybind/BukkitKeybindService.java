package net.digitalingot.feather.serverapi.bukkit.keybind;

import net.digitalingot.feather.serverapi.api.FeatherAPIConstants;
import net.digitalingot.feather.serverapi.api.keybind.KeybindService;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CKeybindCapture;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CKeybindRelease;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BukkitKeybindService implements KeybindService {

    private final Map<FeatherPlayer, Map<Integer, Set<String>>> keyBindings = new HashMap<>();

    private final Map<FeatherPlayer, Map<String, Integer>> keyBindCounts = new HashMap<>();

    private void incrementKeybindCount(FeatherPlayer player, int amount, String pluginSlug) {
        int adjustedAmount = getKeybindCount(pluginSlug, player) + amount;
        if (adjustedAmount > 0) {
            keyBindCounts.computeIfAbsent(player, k -> new HashMap<>()).put(pluginSlug, adjustedAmount);
        } else if (keyBindCounts.containsKey(player)) {
            Map<String, Integer> playerKeybindCounts = keyBindCounts.get(player);
            playerKeybindCounts.remove(pluginSlug);
            if (playerKeybindCounts.size() == 0) keyBindCounts.remove(player);
        }
    }

    private boolean hasKeybound(FeatherPlayer player, int key, String pluginSlug) {
        return keyBindings.getOrDefault(player, new HashMap<>()).getOrDefault(key, new HashSet<>()).contains(pluginSlug);
    }


    private boolean bindKey(FeatherPlayer player, int key, String pluginSlug) {
        Map<Integer, Set<String>> playerKeyBindings = keyBindings.computeIfAbsent(player, k -> new HashMap<>());

        if (getKeybindCount(pluginSlug, player) < FeatherAPIConstants.PLUGIN_KEYBIND_LIMIT && !hasKeybound(player, key, pluginSlug)) {
            Set<String> plugins = playerKeyBindings.computeIfAbsent(key, k -> new HashSet<>());
            plugins.add(pluginSlug);
            incrementKeybindCount(player, 1, pluginSlug);

            return true;
        }

        return false;
    }

    private void unbindKey(FeatherPlayer player, int key, String pluginSlug) {
        Map<Integer, Set<String>> playerKeyBindings = keyBindings.computeIfAbsent(player, k -> new HashMap<>());

        if (hasKeybound(player, key, pluginSlug)) {
            Set<String> plugins = playerKeyBindings.computeIfAbsent(key, k -> new HashSet<>());
            plugins.remove(pluginSlug);
            incrementKeybindCount(player, -1, pluginSlug);
        }
    }


    @Override
    public boolean registerKeybind(String plugin, FeatherPlayer player, int keycode) {
        if (bindKey(player, keycode, plugin)) {
            ((BukkitFeatherPlayer) player).sendMessage(
                new S2CKeybindCapture(plugin, keycode)
            );
            return true;
        }

        return false;
    }

    @Override
    public void releaseKeybind(String plugin, FeatherPlayer player, int keycode) {
        ((BukkitFeatherPlayer) player).sendMessage(
            new S2CKeybindRelease(plugin, keycode)
        );
        unbindKey(player, keycode, plugin);
    }

    @Override
    public int getKeybindCount(String plugin, FeatherPlayer player) {
        return keyBindCounts.getOrDefault(player, new HashMap<>()).getOrDefault(plugin, 0);
    }

    @Override
    public boolean hasKeybind(FeatherPlayer player, int keycode) {
        return keyBindings.getOrDefault(player, new HashMap<>()).containsKey(keycode);
    }

}
