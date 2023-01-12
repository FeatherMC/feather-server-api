package net.digitalingot.feather.serverapi.messaging.messages.server;

import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageReader;
import net.digitalingot.feather.serverapi.messaging.MessageWriter;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;

public class C2SKeybindPressed implements Message<ServerMessageHandler> {
    private final int keycode;

    public C2SKeybindPressed(int keycode) {
        this.keycode = keycode;
    }

    public C2SKeybindPressed(MessageReader reader) {
        this.keycode = reader.readVarInt();
    }

    public void write(MessageWriter writer) {
        writer.writeVarInt(keycode);
    }

    public void handle(ServerMessageHandler handler) {
        handler.handle(this);
    }

    public int getKeycode() {
        return this.keycode;
    }
}