package net.digitalingot.feather.serverapi.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerTaskScheduler {
  private static final Runnable NO_OP = () -> {};

  @NotNull private final FeatherBukkitPlugin plugin;
  private final boolean regionSchedulerSupported;
  @Nullable private final Method isOwnedByCurrentRegionMethod;
  @Nullable private final Method getSchedulerMethod;
  @Nullable private final Method runMethod;
  @Nullable private final Method runDelayedMethod;

  public PlayerTaskScheduler(@NotNull FeatherBukkitPlugin plugin) {
    this.plugin = plugin;

    this.isOwnedByCurrentRegionMethod =
        findMethod(Bukkit.getServer().getClass(), "isOwnedByCurrentRegion", Entity.class);
    this.getSchedulerMethod = findMethod(Player.class, "getScheduler");

    Class<?> schedulerType =
        this.getSchedulerMethod != null ? this.getSchedulerMethod.getReturnType() : null;

    this.runMethod = findMethod(schedulerType, "run", 3);
    this.runDelayedMethod = findMethod(schedulerType, "runDelayed", 4);
    this.regionSchedulerSupported =
        this.getSchedulerMethod != null && this.runMethod != null && this.runDelayedMethod != null;
  }

  public void execute(@NotNull Player player, @NotNull Runnable task) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(task, "task");

    if (shouldRunInline(player)) {
      task.run();
      return;
    }

    if (!player.isOnline()) {
      return;
    }

    if (this.regionSchedulerSupported) {
      invoke(
          this.runMethod,
          invoke(this.getSchedulerMethod, player),
          this.plugin,
          consumer(player, task),
          NO_OP);
      return;
    }

    Bukkit.getScheduler().runTask(this.plugin, () -> runIfOnline(player, task));
  }

  public void executeLater(@NotNull Player player, long delayTicks, @NotNull Runnable task) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(task, "task");

    if (delayTicks <= 0L) {
      execute(player, task);
      return;
    }

    if (!player.isOnline()) {
      return;
    }

    if (this.regionSchedulerSupported) {
      invoke(
          this.runDelayedMethod,
          invoke(this.getSchedulerMethod, player),
          this.plugin,
          consumer(player, task),
          NO_OP,
          delayTicks);
      return;
    }

    Bukkit.getScheduler().runTaskLater(this.plugin, () -> runIfOnline(player, task), delayTicks);
  }

  private boolean shouldRunInline(@NotNull Player player) {
    if (!player.isOnline()) {
      return false;
    }

    if (this.regionSchedulerSupported) {
      return this.isOwnedByCurrentRegionMethod != null
          && Boolean.TRUE.equals(invoke(this.isOwnedByCurrentRegionMethod, Bukkit.getServer(), player));
    }

    return Bukkit.isPrimaryThread();
  }

  private static void runIfOnline(@NotNull Player player, @NotNull Runnable task) {
    if (player.isOnline()) {
      task.run();
    }
  }

  private static Consumer<Object> consumer(@NotNull Player player, @NotNull Runnable task) {
    return ignored -> runIfOnline(player, task);
  }

  @Nullable
  private static Method findMethod(@Nullable Class<?> type, @NotNull String name, int parameterCount) {
    if (type == null) {
      return null;
    }

    for (Method method : type.getMethods()) {
      if (method.getName().equals(name) && method.getParameterTypes().length == parameterCount) {
        return method;
      }
    }

    return null;
  }

  @Nullable
  private static Method findMethod(
      @Nullable Class<?> type, @NotNull String name, @NotNull Class<?>... parameterTypes) {
    if (type == null) {
      return null;
    }

    try {
      return type.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }

  private static Object invoke(@Nullable Method method, @Nullable Object instance, Object... args) {
    if (method == null) {
      throw new IllegalStateException("Required scheduler method is not available");
    }

    try {
      return method.invoke(instance, args);
    } catch (IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException(
          "Failed to invoke scheduler method '" + method.getName() + "'", exception);
    }
  }
}
