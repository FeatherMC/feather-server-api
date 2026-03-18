package net.digitalingot.feather.serverapi.bukkit.ui.rpc;

import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CFUIResponse;
import org.jetbrains.annotations.NotNull;

public class BukkitRpcResponse implements RpcResponse {
  public final int id;
  @NotNull public final BukkitFeatherPlayer player;

  public BukkitRpcResponse(int id, @NotNull BukkitFeatherPlayer player) {
    this.id = id;
    this.player = player;
  }

  @Override
  public void respond(final @NotNull String jsonResponse) {
    this.player.sendMessage(new S2CFUIResponse(this.id, true, jsonResponse));
  }
}
