package net.digitalingot.feather.serverapi.velocity.ui.rpc;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcController;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcRequest;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;
import org.jetbrains.annotations.NotNull;

class RegisteredRpcHandler {

  @NotNull
  public final RpcHandlerExecutor executor;
  @NotNull
  private final RpcController controller;

  public RegisteredRpcHandler(@NotNull RpcController controller, @NotNull Method method)
      throws Throwable {
    this.controller = controller;
    this.executor = generateLambdaExecutor(controller, method);
  }

  private static RpcHandlerExecutor generateLambdaExecutor(RpcController controller, Method method)
      throws Throwable {
    method.setAccessible(true);

    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle methodHandle = lookup.unreflect(method);

    MethodType instanceSignature =
        MethodType.methodType(RpcHandlerExecutor.class, controller.getClass());
    MethodType handlerSignature =
        MethodType.methodType(void.class, RpcRequest.class, RpcResponse.class);

    CallSite callSite =
        LambdaMetafactory.metafactory(
            lookup, "invoke", instanceSignature, handlerSignature, methodHandle, handlerSignature);

    MethodHandle target = callSite.getTarget();
    return (RpcHandlerExecutor) target.invoke(controller);
  }

  @NotNull
  public RpcController getController() {
    return this.controller;
  }

  public <T> void invoke(RpcRequest request, RpcResponse response) {
    this.executor.invoke(request, response);
  }
}
