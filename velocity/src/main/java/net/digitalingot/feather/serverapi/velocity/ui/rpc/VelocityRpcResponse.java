package net.digitalingot.feather.serverapi.velocity.ui.rpc;

import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CFUIResponse;
import net.digitalingot.feather.serverapi.velocity.FeatherVelocityPlugin;
import net.digitalingot.feather.serverapi.velocity.player.VelocityFeatherPlayer;
import org.jetbrains.annotations.NotNull;

public class VelocityRpcResponse implements RpcResponse {

  private final FeatherVelocityPlugin plugin;
  public final int id;
  @NotNull
  public final VelocityFeatherPlayer player;

  public VelocityRpcResponse(
      int id, @NotNull VelocityFeatherPlayer player, @NotNull FeatherVelocityPlugin plugin) {
    this.id = id;
    this.player = player;
    this.plugin = plugin;
  }

  @Override
  public void respond(final @NotNull String jsonResponse) {
    sendOnMainThread(
        () -> this.player.sendMessage(new S2CFUIResponse(this.id, true, jsonResponse)));
  }

  private void sendOnMainThread(Runnable task) {
    plugin.getServer().getScheduler()
        .buildTask(plugin, task)
        .schedule();
  }
}
