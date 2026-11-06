package gr.daldon.simpleboatengine.common;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * A temporary, server-authoritative light level 14 source at the top of the pole.
 * Unlike render-only glow, this illuminates surrounding blocks and nearby players.
 * Every block self-expires if its owning boat leaves, even after a server restart.
 */
public final class BoatPoleLights {
    private static final Map<Boat, BlockPos> POSITIONS = new WeakHashMap<>();

    private BoatPoleLights() {}

    public static void update(ServerLevel level, Boat boat, CompoundTag data) {
        BlockPos old = POSITIONS.get(boat);
        boolean active = data.getBoolean("PoleLightInstalled") && MotorData.isDark(level.getDayTime());
        BlockPos wanted = active ? position(boat) : null;

        if (old != null && !old.equals(wanted)) {
            POSITIONS.remove(boat);
            if (!shouldKeep(level, old)) restore(level, old);
        }
        if (wanted == null) return;

        BlockState existing = level.getBlockState(wanted);
        if (existing.is(EngineBlocks.BOAT_LIGHT.get())) {
            POSITIONS.put(boat, wanted);
            return;
        }

        // Never overwrite player builds, other mod blocks, or flowing water.
        boolean air = existing.isAir();
        boolean waterSource = existing.getFluidState().isSource()
            && existing.getFluidState().getType() == Fluids.WATER
            && existing.getBlock() == Blocks.WATER;
        if (!air && !waterSource) {
            POSITIONS.remove(boat);
            return;
        }

        level.setBlock(wanted, EngineBlocks.BOAT_LIGHT.get().defaultBlockState()
            .setValue(BoatGlowBlock.WATERLOGGED, waterSource), 3);
        POSITIONS.put(boat, wanted);
    }

    public static BlockPos position(Boat boat) {
        // Match the exact pole placement and scale in MotorWorldRenderer.
        double yaw = Math.toRadians(180f - boat.getYRot());
        double x = -0.73;
        double z = 0.55;
        double worldX = boat.getX() + x * Math.cos(yaw) + z * Math.sin(yaw);
        double worldZ = boat.getZ() - x * Math.sin(yaw) + z * Math.cos(yaw);
        double worldY = boat.getY() + boat.getBbHeight() * 0.34 + (8.17 / 16.0) * 1.3;
        return BlockPos.containing(worldX, worldY, worldZ);
    }

    /** Called by the self-cleaning block; the boat must still be close and installed. */
    public static boolean shouldKeep(ServerLevel level, BlockPos pos) {
        if (!MotorData.isDark(level.getDayTime())) return false;
        AABB nearby = new AABB(pos).inflate(3.0);
        return !level.getEntitiesOfClass(Boat.class, nearby, boat ->
            MotorData.installed(boat)
                && MotorData.read(boat).getBoolean("PoleLightInstalled")
                && position(boat).equals(pos)).isEmpty();
    }

    /** Only touches our own invisible light block, restoring its previous air/water. */
    public static void restore(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(EngineBlocks.BOAT_LIGHT.get())) return;
        level.setBlock(pos, current.getValue(BoatGlowBlock.WATERLOGGED)
            ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
}
