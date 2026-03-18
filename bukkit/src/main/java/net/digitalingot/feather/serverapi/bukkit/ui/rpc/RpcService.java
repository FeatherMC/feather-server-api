package net.digitalingot.feather.serverapi.bukkit.ui.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcController;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcHandler;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcRequest;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;
import net.digitalingot.feather.serverapi.bukkit.FeatherBukkitPlugin;
import net.digitalingot.feather.serverapi.bukkit.player.BukkitFeatherPlayer;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CFUIResponse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RpcService {

  private final Logger logger;
  private final Map<String, RpcHost> rpcHosts;

  public RpcService(FeatherBukkitPlugin plugin) {
    this.logger = plugin.getLogger();
    this.rpcHosts = new ConcurrentHashMap<>();
    plugin.getServer().getPluginManager().registerEvents(new HandlerReaper(), plugin);
  }

  public void register(@NotNull Plugin plugin, @NotNull RpcController controller) {
    Objects.requireNonNull(plugin);
    String rpcHostName = getRpcHostName(plugin);
    register(rpcHostName, controller);
  }

  private void register(@NotNull String rpcHostName, @NotNull RpcController controller) {
    Objects.requireNonNull(controller);

    RpcHost rpcHost = this.rpcHosts.get(rpcHostName);

    if (isControllerRegistered(rpcHost, controller)) {
      throw new IllegalStateException(
          "Controller '" + controller.getClass().getName() + "' is already registered");
    }

    Class<?> controllerClazz = controller.getClass();
    Method[] methods = controllerClazz.getDeclaredMethods();

    Map<String, Method> nameToMethod = new HashMap<>(methods.length);

    for (Method method : methods) {
      RpcHandler rpcHandler = method.getAnnotation(RpcHandler.class);

      if (rpcHandler == null) {
        continue;
      }

      if (method.isBridge() || method.isSynthetic()) {
        continue;
      }

      if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
        this.logger.warning("Handler '" + method.getName() + "' is not public.");
        continue;
      }

      if ((method.getModifiers() & Modifier.STATIC) != 0) {
        this.logger.warning("Handler '" + method.getName() + "' is static.");
        continue;
      }

      if (method.getReturnType() != void.class) {
        this.logger.warning(
            "Handler '" + method.getName() + " has invalid return type.  Expecting void.");
        continue;
      }

      Class<?>[] paramTypes = method.getParameterTypes();
      if (paramTypes.length != 2
          || !RpcRequest.class.isAssignableFrom(paramTypes[0])
          || !RpcResponse.class.isAssignableFrom(paramTypes[1])) {
        this.logger.warning(
            "Handler '"
                + method.getName()
                + "' has invalid parameter type(s).  Expecting (RpcRequest, RpcResponse).");
        continue;
      }

      String rpcName = rpcHandler.value();

      if (isNameRegistered(rpcHost, rpcName) || nameToMethod.put(rpcName, method) != null) {
        throw new IllegalArgumentException("Handler for key '" + rpcName + "' already exists");
      }
    }

    if (nameToMethod.isEmpty()) {
      return;
    }

    Map<String, RegisteredRpcHandler> handlers = new HashMap<>(nameToMethod.size());

    for (Entry<String, Method> entry : nameToMethod.entrySet()) {
      String rpcName = entry.getKey();
      Method rpcMethod = entry.getValue();

      try {
        RegisteredRpcHandler handler = new RegisteredRpcHandler(controller, rpcMethod);
        handlers.put(rpcName, handler);
      } catch (Throwable throwable) {
        throw new IllegalStateException(throwable);
      }
    }

    if (rpcHost == null) {
      RpcHost newRpcHost = new RpcHost();
      RpcHost previous = this.rpcHosts.putIfAbsent(rpcHostName, newRpcHost);
      rpcHost = previous != null ? previous : newRpcHost;
    }

    rpcHost.registerAll(handlers);
  }

  public void unregisterByController(Plugin owner, RpcController controller) {
    RpcHost rpcHost = this.rpcHosts.get(getRpcHostName(owner));
    if (rpcHost != null) {
      rpcHost.unregisterByController(controller);
    }
  }

  public void unregisterByPlugin(Plugin plugin) {
    this.rpcHosts.remove(getRpcHostName(plugin));
  }

  public void handle(
      BukkitFeatherPlayer player, String rpcHostName, String rpcName, int requestId, String body) {
    RegisteredRpcHandler handler = getRpcHandler(rpcHostName, rpcName);

    if (handler != null) {
      try {
        handler.invoke(
            new BukkitRpcRequest(player, body),
            new BukkitRpcResponse(requestId, player));
      } catch (Throwable throwable) {
        this.logger.warning("Error occurred handling RPC request");
        throwable.printStackTrace();
      }
    } else {
      player.sendMessage(new S2CFUIResponse(requestId, false, ""));
    }
  }

  private RegisteredRpcHandler getRpcHandler(@NotNull String rpcHostName, @NotNull String rpcName) {
    rpcHostName = rpcHostName.toLowerCase(Locale.ROOT);
    RpcHost rpcHost = this.rpcHosts.get(rpcHostName);
    return rpcHost != null ? rpcHost.getHandlerByName(rpcName) : null;
  }

  private static boolean isControllerRegistered(
      @Nullable RpcHost rpcHost, @NotNull RpcController controller) {
    return rpcHost != null && rpcHost.isControllerRegistered(controller);
  }

  private static boolean isNameRegistered(@Nullable RpcHost rpcHost, @NotNull String name) {
    return rpcHost != null && rpcHost.isNameRegistered(name);
  }

  private static String getRpcHostName(Plugin plugin) {
    return plugin.getName().toLowerCase(Locale.ROOT);
  }

  private static class RpcHost {

    private final Map<String, RegisteredRpcHandler> handlers;

    public RpcHost() {
      this.handlers = new ConcurrentHashMap<>();
    }

    public void registerAll(@NotNull Map<String, RegisteredRpcHandler> handlers) {
      this.handlers.putAll(handlers);
    }

    public void unregisterByController(@NotNull RpcController controller) {
      for (Entry<String, RegisteredRpcHandler> entry : this.handlers.entrySet()) {
        RegisteredRpcHandler handler = entry.getValue();
        if (handler.getController() == controller) {
          this.handlers.remove(entry.getKey(), handler);
        }
      }
    }

    public RegisteredRpcHandler getHandlerByName(@NotNull String name) {
      return this.handlers.get(name);
    }

    public boolean isControllerRegistered(@NotNull RpcController controller) {
      return this.handlers.values().stream()
          .anyMatch(handler -> handler.getController() == controller);
    }

    public boolean isNameRegistered(@NotNull String name) {
      return this.handlers.containsKey(name);
    }
  }

  private class HandlerReaper implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
      RpcService.this.unregisterByPlugin(event.getPlugin());
    }
  }
}
