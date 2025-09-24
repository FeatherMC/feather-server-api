package net.digitalingot.feather.serverapi.velocity.ui.rpc;

import net.digitalingot.feather.serverapi.api.ui.rpc.RpcRequest;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;

@FunctionalInterface
public interface RpcHandlerExecutor {

  void invoke(RpcRequest request, RpcResponse response);
}
