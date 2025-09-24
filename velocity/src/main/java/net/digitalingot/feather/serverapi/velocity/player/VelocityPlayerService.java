package net.digitalingot.feather.serverapi.velocity.player;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.player.PlayerService;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.digitalingot.feather.serverapi.velocity.event.player.FeatherPlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityPlayerService implements PlayerService {

  private final ProxyServer server;
  private final FeatherVelocityPlugin plugin;
  private final Map<UUID, VelocityFeatherPlayer> players = new HashMap<>();

  public VelocityPlayerService(FeatherVelocityPlugin plugin, ProxyServer server) {
    this.plugin = plugin;
    this.server = server;
    this.server.getEventManager().register(plugin, this);
  }

  public void register(VelocityFeatherPlayer player) {
    this.players.put(player.getUniqueId(), player);
  }

  @Override
  public @Nullable VelocityFeatherPlayer getPlayer(@NotNull UUID playerId) {
    return this.players.get(playerId);
  }

  @Override
  public @NotNull Collection<FeatherPlayer> getPlayers() {
    return Collections.unmodifiableCollection(this.players.values());
  }

  @Subscribe(order = PostOrder.LAST)
  public void onPlayerQuit(DisconnectEvent event) {
    FeatherPlayer player = this.players.remove(event.getPlayer().getUniqueId());

    if (player != null) {
      this.server.getEventManager().fireAndForget(new FeatherPlayerQuitEvent(player));
    }
  }
}
