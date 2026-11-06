package gr.daldon.simpleboatengine.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.vehicle.Boat;

/** Cached server data for nearby motors; reset automatically on world change. */
public final class ClientMotorState {
    private static final Map<Integer, CompoundTag> RECEIVED = new HashMap<>();
    private static ClientLevel observed;
    private ClientMotorState() {}

    public static void accept(int entityId, CompoundTag data) {
        RECEIVED.put(entityId, data);
    }

    public static CompoundTag get(Boat boat) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != observed) {
            RECEIVED.clear();
            observed = current;
        }
        return RECEIVED.get(boat.getId());
    }
}
