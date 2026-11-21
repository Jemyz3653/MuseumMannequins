package studio.detemkur.museummannequins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class MannequinBlock extends Block implements EntityBlock {
    public static final BooleanProperty GLASS = BooleanProperty.create("glass");
    public MannequinBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(GLASS,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(GLASS); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MannequinEntity(pos,state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer != null && level.getBlockEntity(pos) instanceof MannequinEntity display) {
            display.owner(placer.getUUID());
        }
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS) || state.getValue(GLASS)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MannequinEntity display && display.canEdit(server)) {
            level.setBlockAndUpdate(pos,state.setValue(GLASS,true));
            stack.consume(1,player);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && state.getValue(GLASS) && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof MannequinEntity display && display.canEdit(server)) {
            level.setBlockAndUpdate(pos,state.setValue(GLASS,false));
            if (!player.getAbilities().instabuild) popResource(level,pos,new ItemStack(Items.GLASS));
        }
        // The screen opens client-side through the interaction callback.
        return InteractionResult.SUCCESS;
    }
}
