package gr.daldon.simpleboatengine.common;

import gr.daldon.simpleboatengine.SimpleBoatEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EngineSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SimpleBoatEngine.ID);
    public static final RegistryObject<SoundEvent> MOTOR_LOOP = SOUNDS.register("motor_loop",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SimpleBoatEngine.ID, "motor_loop")));
    private EngineSounds() {}
}
