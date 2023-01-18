package net.digitalingot.feather.serverapi.messaging.messages.client;

import net.digitalingot.feather.serverapi.messaging.ClientMessageHandler;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageReader;
import net.digitalingot.feather.serverapi.messaging.MessageWriter;
import net.digitalingot.feather.serverapi.messaging.domain.FeatherMod;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class S2CPluginModDisable implements Message<ClientMessageHandler> {
    private final Collection<FeatherMod> featherMods;

    public S2CPluginModDisable(@NotNull Collection<FeatherMod> featherMods) {
        this.featherMods = featherMods;
    }

    public S2CPluginModDisable(MessageReader reader) {
        this.featherMods = reader.readList(FeatherMod.DECODER);
    }

    public void write(MessageWriter writer) {
        writer.writeCollection(this.featherMods, FeatherMod.ENCODER);
    }

    public void handle(ClientMessageHandler handler) {
        handler.handle(this);
    }

    public Collection<FeatherMod> getFeatherMods() {
        return this.featherMods;
    }

}
