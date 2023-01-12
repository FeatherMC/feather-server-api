package net.digitalingot.feather.serverapi.messaging.messages.client;

import net.digitalingot.feather.serverapi.messaging.ClientMessageHandler;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageReader;
import net.digitalingot.feather.serverapi.messaging.MessageWriter;
import org.jetbrains.annotations.NotNull;

public class S2CKeybindCapture implements Message<ClientMessageHandler> {
    private final @NotNull String pluginSlug;
    private final int keycode;

    public S2CKeybindCapture(@NotNull String pluginSlug, int keycode) {
        this.pluginSlug = pluginSlug;
        this.keycode = keycode;
    }

    public S2CKeybindCapture(MessageReader reader) {
        this.pluginSlug = reader.readUtf();
        this.keycode = reader.readVarInt();
    }

    public void write(MessageWriter writer) {
        writer.writeUtf(this.pluginSlug);
        writer.writeVarInt(this.keycode);
    }

    public void handle(ClientMessageHandler handler) {
        handler.handle(this);
    }

    public @NotNull String getPluginSlug() {
        return this.pluginSlug;
    }

    public int getKeycode() {
        return this.keycode;
    }
}