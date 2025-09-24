package net.digitalingot.feather.serverapi.velocity.meta;

import com.velocitypowered.api.event.Subscribe;
import net.digitalingot.feather.serverapi.api.meta.DiscordActivity;
import net.digitalingot.feather.serverapi.api.meta.MetaService;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackground;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackgroundFactory;
import net.digitalingot.feather.serverapi.api.meta.exception.ImageSizeExceededException;
import net.digitalingot.feather.serverapi.api.meta.exception.InvalidImageException;
import net.digitalingot.feather.serverapi.api.meta.exception.UnsupportedImageFormatException;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CClearDiscordActivity;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CServerBackground;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CServerBackground.Action;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetDiscordActivity;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.digitalingot.feather.serverapi.velocity.event.player.VelocityPlayerHelloEvent;
import net.digitalingot.feather.serverapi.velocity.player.VelocityFeatherPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityMetaService implements MetaService {

  private final ServerListBackgroundFactory serverListBackgroundFactory =
      new VelocityServerListBackgroundFactory();
  @Nullable
  private volatile ServerListBackground serverListBackground = null;

  public VelocityMetaService(@NotNull FeatherVelocityPlugin plugin) {
    plugin.getServer().getEventManager().register(plugin, this);
  }

  @Override
  public synchronized void setServerListBackground(
      @NotNull ServerListBackground serverListBackground)
      throws UnsupportedImageFormatException, ImageSizeExceededException, InvalidImageException {
    ServerListBackgroundValidator.validate(serverListBackground);
    this.serverListBackground = serverListBackground;
  }

  @Override
  public synchronized @Nullable ServerListBackground getServerListBackground() {
    return this.serverListBackground;
  }

  @Override
  public void updateDiscordActivity(
      @NotNull FeatherPlayer player, @NotNull DiscordActivity discordActivity) {
    ((VelocityFeatherPlayer) player)
        .sendMessage(
            new S2CSetDiscordActivity(
                discordActivity.getImage().orElse(null),
                discordActivity.getImageText().orElse(null),
                discordActivity.getState().orElse(null),
                discordActivity.getDetails().orElse(null),
                discordActivity.getPartySize().orElse(null),
                discordActivity.getPartyMax().orElse(null),
                discordActivity.getStartTimestamp().orElse(null),
                discordActivity.getEndTimestamp().orElse(null)));
  }

  @Override
  public void clearDiscordActivity(@NotNull FeatherPlayer player) {
    ((VelocityFeatherPlayer) player).sendMessage(new S2CClearDiscordActivity());
  }

  @Override
  public @NotNull ServerListBackgroundFactory getServerListBackgroundFactory() {
    return this.serverListBackgroundFactory;
  }

  @Subscribe
  public void onFeatherPlayerHello(VelocityPlayerHelloEvent event) {
    ServerListBackground background = getServerListBackground();
    if (background != null) {
      ((VelocityFeatherPlayer) event.getPlayer())
          .sendMessage(new S2CServerBackground(Action.HASH, background.getHash()));
    }
  }
}
