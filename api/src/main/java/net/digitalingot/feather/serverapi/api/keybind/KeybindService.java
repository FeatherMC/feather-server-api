package net.digitalingot.feather.serverapi.api.keybind;

import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;

public interface KeybindService {
    /**
     *
     * Registers a keybind to a player associated with this plugin.
     *
     * @param plugin
     * Unique string identifier associated with the plugin
     * @param player
     * The FeatherPlayer to register a keybind
     * @param keycode
     * Input keycode for the client to bind
     *
     * @return Returns false if this plugin is attempting to bind more keys at once than allowed, or if the keybind is already bound
     */
    boolean registerKeybind(String plugin, FeatherPlayer player, int keycode);

    /**
     * Releases a keybind associated to this plugin from the player
     *
     * @param plugin
     * Unique string identifier associated with the plugin
     * @param player
     * The FeatherPlayer to remove a keybind
     * @param keycode
     * Input keycode for the client to un-bind
     */
    void releaseKeybind(String plugin, FeatherPlayer player, int keycode);

    /**
     * Returns the number of keybinds active for this plugin, associated with this player.
     *
     * @param plugin
     * Unique string identifier associated with the plugin
     * @param player
     * The FeatherPlayer to which the keybinds are associated
     */
    int getKeybindCount(String plugin, FeatherPlayer player);

    /**
     * Returns true if the player has a keybound registered with the provided keycode
     *
     * @param player
     * The FeatherPlayer to which the keybinds are associated
     * @param keycode
     * Input keycode value for the keybind
     */
    boolean hasKeybind(FeatherPlayer player, int keycode);

}
