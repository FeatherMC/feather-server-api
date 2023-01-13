package net.digitalingot.feather.serverapi.examples.bukkit;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.digitalingot.feather.serverapi.api.FeatherAPI;
import net.digitalingot.feather.serverapi.api.event.player.PlayerHelloEvent;
import net.digitalingot.feather.serverapi.api.event.player.PlayerPressedKeybindEvent;
import net.digitalingot.feather.serverapi.api.model.FeatherMod;
import net.digitalingot.feather.serverapi.api.player.FeatherPlayer;
import net.digitalingot.feather.serverapi.api.ui.UIPage;
import net.digitalingot.feather.serverapi.api.ui.handler.UIFocusHandler;
import net.digitalingot.feather.serverapi.api.ui.handler.UILifecycleHandler;
import net.digitalingot.feather.serverapi.api.ui.handler.UILoadHandler;
import net.digitalingot.feather.serverapi.api.ui.handler.UIVisibilityHandler;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcController;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcHandler;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcRequest;
import net.digitalingot.feather.serverapi.api.ui.rpc.RpcResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExamplePlugin extends JavaPlugin implements Listener {

    public static final String PLUGIN_SLUG = "examplePlugin";
  private static final String DEMO_HTML_RESOURCE = "demo.min.html";
  private static final Gson GSON = new Gson();
  private static final int MAX_SCORES = 10;

  private List<Score> scores = new ArrayList<>(MAX_SCORES);
  private UIPage page;

  @Override
  public void onEnable() {
    String demoUrl;

    try {
      demoUrl = getDemoHtmlUrl();
    } catch (IOException exception) {
      getLogger().severe("Could not generate demo html data url");
      exception.printStackTrace();
      return;
    }

    this.page = createPage(demoUrl);

    subscribeToFeatherHello();
    subscribeToFeatherKeyInput();

    setupCommands();
  }

  private UIPage createPage(String url) {
    UIPage page = FeatherAPI.getUIService().registerPage(this, url);

    page.setLifecycleHandler(
        new UILifecycleHandler() {
          @Override
          public void onCreated(@NotNull FeatherPlayer player) {
            getLogger().info("Page created for " + player.getName());

            updateScoresFor(player);
            FeatherAPI.getUIService().showOverlayForPlayer(player, page);
          }

          @Override
          public void onDestroyed(@NotNull FeatherPlayer player) {
            getLogger().info("Page destroyed for " + player.getName());
          }
        });

    page.setFocusHandler(
        new UIFocusHandler() {
          @Override
          public void onFocusGained(@NotNull FeatherPlayer player) {
            getLogger().info("Page gained focus for " + player.getName());
          }

          @Override
          public void onFocusLost(@NotNull FeatherPlayer player) {
            getLogger().info("Page lost focus for " + player.getName());
          }
        });

    page.setLoadHandler(
        new UILoadHandler() {
          @Override
          public void onLoadError(@NotNull FeatherPlayer player, @NotNull String errorText) {
            getLogger().info("Page failed to load for " + player.getName() + ": " + errorText);
          }
        });

    page.setVisibilityHandler(
        new UIVisibilityHandler() {
          @Override
          public void onShow(FeatherPlayer player) {
            getLogger().info("Page made visible for " + player.getName());
          }

          @Override
          public void onHide(FeatherPlayer player) {
            getLogger().info("Page hidden for " + player.getName());
          }
        });

    FeatherAPI.getUIService().registerCallbacks(page, new ExampleController(this));

    return page;
  }

  private void subscribeToFeatherHello() {
    FeatherAPI.getEventService()
        .subscribe(
            PlayerHelloEvent.class,
            event -> {
              getLogger().info("Got hello from " + event.getPlayer().getName());
              getLogger().info("Platform: " + event.getPlatform());

              initializeKeybinds(event.getPlayer());

              getLogger()
                  .info(
                      "Platform Mods ("
                          + event.getPlatformMods().size()
                          + "): "
                          + event.getPlatformMods().stream()
                              .map(mod -> mod.getName() + ":" + mod.getVersion())
                              .collect(Collectors.joining(", ")));
              getLogger()
                  .info(
                      "Feather Mods ("
                          + event.getFeatherMods().size()
                          + "): "
                          + event.getFeatherMods().stream()
                              .map(FeatherMod::getName)
                              .collect(Collectors.joining(", ")));

              FeatherAPI.getUIService().createPageForPlayer(event.getPlayer(), this.page);
            });
  }

    private void initializeKeybinds(FeatherPlayer player) {
        FeatherAPI.getKeybindService().registerKeybind(PLUGIN_SLUG, player, 88);
        getLogger().info("Initialized keybinds for: " + player.getName());
    }

    private void subscribeToFeatherKeyInput() {
        FeatherAPI.getEventService()
                .subscribe(
                        PlayerPressedKeybindEvent.class,
                        event -> {
                            getLogger().info("Key pressed by: " + event.getPlayer().getName());
                            getLogger().info("Keycode is: " + event.getKeycode());
                        });
    }

  @SuppressWarnings("deprecation")
  private void setupCommands() {
    getCommand("snake")
        .setExecutor(
            (sender, command, label, args) -> {
              Player target;

              if (!(sender instanceof Player)) {
                if (args.length < 1) {
                  sender.sendMessage("Usage: /" + label + " [target]");
                  return true;
                }

                target = getServer().getPlayer(args[0]);
              } else if (sender.isOp() && args.length > 0) {
                target = getServer().getPlayer(args[0]);
              } else {
                target = (Player) sender;
              }

              if (target == null) {
                sender.sendMessage("No such player");
                return true;
              }

              FeatherPlayer featherPlayer =
                  FeatherAPI.getPlayerService().getPlayer(target.getUniqueId());
              if (featherPlayer == null) {
                sender.sendMessage("Not a feather player");
                return true;
              }

              FeatherAPI.getUIService().openPageForPlayer(featherPlayer, this.page);
              return true;
            });
  }

  private void submitScore(FeatherPlayer player, int score) {
    if (!this.scores.isEmpty()) {
      int scoreRequirement = this.scores.get(this.scores.size() - 1).getScore();
      if (score < scoreRequirement) {
        return;
      }
    }

    this.scores.add(new Score(player.getName(), score));
    this.scores.sort(ScoreComparator.INSTANCE);

    if (this.scores.size() > MAX_SCORES) {
      this.scores = this.scores.subList(0, MAX_SCORES);
    }

    updatesScores();
  }

  private void updatesScores() {
    String updateScores = serializeScores();
    for (FeatherPlayer player : FeatherAPI.getPlayerService().getPlayers()) {
      FeatherAPI.getUIService().sendPageMessage(player, this.page, updateScores);
    }
  }

  private void updateScoresFor(FeatherPlayer player) {
    if (!this.scores.isEmpty()) {
      FeatherAPI.getUIService().sendPageMessage(player, this.page, serializeScores());
    }
  }

  private String serializeScores() {
    JsonObject message = new JsonObject();
    message.addProperty("type", "updateScore");
    JsonArray scores = new JsonArray();
    for (Score score : this.scores) {
      JsonObject entry = new JsonObject();
      entry.addProperty("playerName", score.getPlayerName());
      entry.addProperty("score", score.getScore());
      scores.add(entry);
    }
    message.add("scores", scores);
    return GSON.toJson(message);
  }

  public static class ExampleController implements RpcController {
    private final ExamplePlugin plugin;

    public ExampleController(ExamplePlugin plugin) {
      this.plugin = plugin;
    }

    @RpcHandler("submitScore")
    public void submitScore(RpcRequest request, RpcResponse response) {
      try {
        int score = Integer.parseInt(request.getBody());
        this.plugin.submitScore(request.getSource(), score);
      } catch (NumberFormatException ignored) {
      }
      response.respond("");
    }
  }

  private static class Score {
    private final String playerName;
    private final int score;

    public Score(String playerName, int score) {
      this.playerName = playerName;
      this.score = score;
    }

    public String getPlayerName() {
      return this.playerName;
    }

    public int getScore() {
      return this.score;
    }
  }

  private enum ScoreComparator implements Comparator<Score> {
    INSTANCE() {
      @Override
      public int compare(Score o1, Score o2) {
        return Integer.compare(o2.getScore(), o1.getScore());
      }
    }
  }

  private String getDemoHtmlUrl() throws IOException {
    return generateHtmlDataUrl(getResourceBytes(DEMO_HTML_RESOURCE));
  }

  @SuppressWarnings({"UnstableApiUsage", "SameParameterValue"})
  private byte[] getResourceBytes(String path) throws IOException {
    InputStream resourceStream = getResource(path);
    if (resourceStream == null) {
      throw new IOException("No such file: " + path);
    }
    try {
      return ByteStreams.toByteArray(resourceStream);
    } finally {
      Closeables.closeQuietly(resourceStream);
    }
  }

  private static String generateHtmlDataUrl(byte[] htmlBytes) {
    return "data:text/html;base64," + Base64.getEncoder().encodeToString(htmlBytes);
  }
}
