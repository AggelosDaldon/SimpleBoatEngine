package gr.daldon.simpleboatengine.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 * Temporary, server-authoritative light level 14 sources at each fitted lamp.
 * Unlike render-only glow, this illuminates surrounding blocks and nearby players.
 * Every block self-expires if its owning boat leaves, even after a server restart.
 */
public final class BoatPoleLights {
    private static final Map<Boat, Set<BlockPos>> POSITIONS = new WeakHashMap<>();

    private BoatPoleLights() {}

    public static void update(ServerLevel level, Boat boat, CompoundTag data) {
        Set<BlockPos> old = POSITIONS.getOrDefault(boat, Set.of());
        Set<BlockPos> wanted = MotorData.isDark(level.getDayTime())
            ? positions(boat, data) : Set.of();

        for (BlockPos previous : old) {
            if (!wanted.contains(previous) && !shouldKeep(level, previous)) {
                restore(level, previous);
            }
        }

        Set<BlockPos> placed = new HashSet<>();
        for (BlockPos target : wanted) {
            BlockState existing = level.getBlockState(target);
            if (existing.is(EngineBlocks.BOAT_LIGHT.get())) {
                placed.add(target);
                continue;
            }

            // Never overwrite player builds, other mod blocks, or flowing water.
            boolean air = existing.isAir();
            boolean waterSource = existing.getFluidState().isSource()
                && existing.getFluidState().getType() == Fluids.WATER
                && existing.getBlock() == Blocks.WATER;
            if (!air && !waterSource) continue;

            level.setBlock(target, EngineBlocks.BOAT_LIGHT.get().defaultBlockState()
                .setValue(BoatGlowBlock.WATERLOGGED, waterSource), 3);
            placed.add(target);
        }
        if (placed.isEmpty()) POSITIONS.remove(boat);
        else POSITIONS.put(boat, placed);
    }

    /** Exact model-space lamps after their attachment transform in MotorWorldRenderer. */
    private static Set<BlockPos> positions(Boat boat, CompoundTag data) {
        if (!data.getBoolean("PoleLightInstalled")) return Set.of();
        return Set.of(position(boat, -0.62, 1.0, 1.0, 8.17));
    }

    private static BlockPos position(Boat boat, double x, double heightFraction,
                                     double z, double modelTopY) {
        double yaw = Math.toRadians(180f - boat.getYRot());
        double worldX = boat.getX() + x * Math.cos(yaw) + z * Math.sin(yaw);
        double worldZ = boat.getZ() - x * Math.sin(yaw) + z * Math.cos(yaw);
        double worldY = boat.getY() + boat.getBbHeight() * heightFraction
            + modelTopY / 16.0 * 1.3;
        return BlockPos.containing(worldX, worldY, worldZ);
    }

    /** Called by the self-cleaning block; the boat must still be close and installed. */
    public static boolean shouldKeep(ServerLevel level, BlockPos pos) {
        if (!MotorData.isDark(level.getDayTime())) return false;
        AABB nearby = new AABB(pos).inflate(3.0);
        return !level.getEntitiesOfClass(Boat.class, nearby, boat ->
            MotorData.installed(boat)
                && positions(boat, MotorData.read(boat)).contains(pos)).isEmpty();
    }

    /** Only touches our own invisible light block, restoring its previous air/water. */
    public static void restore(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(EngineBlocks.BOAT_LIGHT.get())) return;
        level.setBlock(pos, current.getValue(BoatGlowBlock.WATERLOGGED)
            ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
}
