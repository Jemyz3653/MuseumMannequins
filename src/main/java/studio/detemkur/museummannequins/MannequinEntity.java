package studio.detemkur.museummannequins;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class MannequinEntity extends BlockEntity {
    public static final Pose[] POSES = {Pose.STANDING,Pose.SITTING,Pose.CROUCHING,Pose.SLEEPING,Pose.SWIMMING};
    public String mob = "minecraft:pig";
    public float yaw, head, pitch, stride;
    public int pose;
    private UUID owner;

    public MannequinEntity(BlockPos pos, BlockState state) { super(MuseumMannequins.ENTITY,pos,state); }
    public void owner(UUID id) { owner = id; setChanged(); }
    public boolean canEdit(ServerPlayer player) {
        return owner == null || owner.equals(player.getUUID()) || player.level().getServer().getPlayerList().isOp(player.nameAndId());
    }
    public void configure(String mob, float yaw, float head, float pitch, int pose, float stride) {
        this.mob = mob;
        this.yaw = yaw % 360;
        this.head = Math.clamp(head,-180,180);
        this.pitch = Math.clamp(pitch,-90,90);
        this.pose = pose;
        this.stride = Math.clamp(stride,0,360);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("mob",mob); output.putFloat("yaw",yaw); output.putFloat("head",head);
        output.putFloat("pitch",pitch); output.putInt("pose",pose); output.putFloat("stride",stride);
        if (owner != null) output.store("owner",UUIDUtil.CODEC,owner);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mob = input.getStringOr("mob","minecraft:pig");
        yaw = finite(input.getFloatOr("yaw",0)); head = Math.clamp(finite(input.getFloatOr("head",0)),-180,180);
        pitch = Math.clamp(finite(input.getFloatOr("pitch",0)),-90,90);
        pose = Math.clamp(input.getIntOr("pose",0),0,POSES.length-1);
        stride = Math.clamp(finite(input.getFloatOr("stride",0)),0,360);
        owner = input.read("owner",UUIDUtil.CODEC).orElse(null);
    }
    private static float finite(float value) { return Float.isFinite(value) ? value : 0; }
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        var tag = new CompoundTag();
        tag.putString("mob",mob); tag.putFloat("yaw",yaw); tag.putFloat("head",head);
        tag.putFloat("pitch",pitch); tag.putInt("pose",pose); tag.putFloat("stride",stride);
        builder.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(tag));
    }
    @Override protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var data = components.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null && level != null) loadAdditional(TagValueInput.create(
            net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),data.copyTag()));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,registries);
        saveAdditional(output);
        return output.buildResult();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
