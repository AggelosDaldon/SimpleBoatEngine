package gr.daldon.simpleboatengine.common;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Place on shore, click to capture the nearest unoccupied boat within 4 blocks. */
public final class BoatRecoveryBlock extends BaseEntityBlock {
    public BoatRecoveryBlock(BlockBehaviour.Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new BoatRecoveryBlockEntity(pos,state);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,
                Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(world.isClientSide)return InteractionResult.SUCCESS;
        if(!(world.getBlockEntity(pos) instanceof BoatRecoveryBlockEntity crate))
            return InteractionResult.PASS;
        if(!crate.empty()){
            ItemStack stored=crate.take();
            if(!player.getInventory().add(stored))player.drop(stored,false);
            return InteractionResult.CONSUME;
        }
        var candidates=world.getEntitiesOfClass(Boat.class,new AABB(pos).inflate(4.0),
            b->b.isAlive()&&b.getPassengers().isEmpty());
        if(candidates.isEmpty()){
            player.displayClientMessage(Component.literal("No unoccupied boat within 4 blocks."),true);
            return InteractionResult.CONSUME;
        }
        Boat nearest=candidates.get(0);
        for(Boat b:candidates)if(b.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<
                              nearest.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5))
            nearest=b;
        ItemStack packed=nearest instanceof BlueFishingBoat blue
            ? blue.getPickResult() : new ItemStack(nearest.getDropItem());
        crate.store(packed);
        nearest.discard(); // Not a damage death: stored cargo must not spill.
        player.displayClientMessage(Component.literal("Boat safely recovered with contents."),true);
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!old.is(next.getBlock())){
            if(!level.isClientSide && level.getBlockEntity(pos) instanceof BoatRecoveryBlockEntity crate
                && !crate.empty())net.minecraft.world.Containers.dropItemStack(level,
                    pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,crate.take());
            super.onRemove(old,level,pos,next,moving);
        }
    }
}
