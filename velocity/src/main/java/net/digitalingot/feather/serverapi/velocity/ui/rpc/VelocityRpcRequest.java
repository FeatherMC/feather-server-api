package net.digitalingot.feather.serverapi.velocity.ui.rpc;

import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcRequest;
import net.digitalingot.feather.serverapi.velocity.player.VelocityFeatherPlayer;
import org.jetbrains.annotations.NotNull;

public class VelocityRpcRequest implements RpcRequest {

  @NotNull
  private final VelocityFeatherPlayer player;
  @NotNull
  private final String body;

  public VelocityRpcRequest(@NotNull VelocityFeatherPlayer player, @NotNull String body) {
    this.player = player;
    this.body = body;
  }

  @Override
  public @NotNull FeatherPlayer getSource() {
    return this.player;
  }

  @Override
  public @NotNull String getBody() {
    return this.body;
  }
}
