package net.digitalingot.feather.serverapi.api.mod;

import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FeatherModService {

    /**
     * Disables the selected feather mods for the player
     *
     * @param player
     * The player whose mods will be disabled
     * @param mods
     * The list of mods to disable
     */
    void disableMods(@NotNull FeatherPlayer player, @NotNull List<FeatherMod> mods);

    /**
     * Re-enables the selected feather mods for the player
     *
     * @param player
     * The player whose mods will be re-enabled
     * @param mods
     * The list of mods to re-enable
     */
    void reEnableMods(@NotNull FeatherPlayer player, @NotNull List<FeatherMod> mods);

    /**
     * Returns the list of disabled feather mods for the player
     *
     * @param player
     * The player whose mods are disabled
     */
    List<FeatherMod> getDisabledMods(@NotNull FeatherPlayer player);

}
