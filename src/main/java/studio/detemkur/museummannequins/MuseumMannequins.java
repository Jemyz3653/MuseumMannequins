package studio.detemkur.museummannequins;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MuseumMannequins implements ModInitializer {
    public static final String ID = "museummannequins";
    public static Block DISPLAY;
    public static BlockEntityType<MannequinEntity> ENTITY;
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    public record Configure(BlockPos pos, String mob, float yaw, float head, float pitch, int pose, float stride)
            implements CustomPacketPayload {
        public static final Type<Configure> TYPE = new Type<>(id("configure"));
        public static final StreamCodec<FriendlyByteBuf, Configure> CODEC = CustomPacketPayload.codec(
            (packet, buffer) -> {
                buffer.writeBlockPos(packet.pos); buffer.writeUtf(packet.mob,128);
                buffer.writeFloat(packet.yaw); buffer.writeFloat(packet.head); buffer.writeFloat(packet.pitch);
                buffer.writeVarInt(packet.pose); buffer.writeFloat(packet.stride);
            }, buffer -> new Configure(buffer.readBlockPos(),buffer.readUtf(128),buffer.readFloat(),
                buffer.readFloat(),buffer.readFloat(),buffer.readVarInt(),buffer.readFloat()));
        @Override public Type<Configure> type() { return TYPE; }
    }

    @Override public void onInitialize() {
        var key = ResourceKey.create(Registries.BLOCK,id("mannequin"));
        DISPLAY = Registry.register(BuiltInRegistries.BLOCK,key,new MannequinBlock(
            BlockBehaviour.Properties.of().setId(key).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        var itemKey = ResourceKey.create(Registries.ITEM,id("mannequin"));
        var item = Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(DISPLAY,
            new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("mannequin"),
            FabricBlockEntityTypeBuilder.create(MannequinEntity::new,DISPLAY).build());
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("minecraft","functional_blocks"))).register(entries -> entries.accept(item));
        PayloadTypeRegistry.serverboundPlay().register(Configure.TYPE,Configure.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Configure.TYPE,(packet,context) -> context.server().execute(() -> {
            var player = context.player();
            var level = player.level();
            if (!player.getAbilities().mayBuild || !player.isWithinBlockInteractionRange(packet.pos,0)
                || !level.hasChunkAt(packet.pos) || !player.mayInteract(level,packet.pos)
                || !(level.getBlockEntity(packet.pos) instanceof MannequinEntity display) || !display.canEdit(player)) return;
            Identifier mobId = Identifier.tryParse(packet.mob);
            if (mobId == null || !mobId.getNamespace().equals("minecraft")
                || !Float.isFinite(packet.yaw) || !Float.isFinite(packet.head) || !Float.isFinite(packet.pitch)
                || !Float.isFinite(packet.stride) || packet.pose < 0 || packet.pose >= MannequinEntity.POSES.length) return;
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(mobId);
            if (type == null || !type.canSummon() || !(type.create(level,EntitySpawnReason.LOAD) instanceof LivingEntity)) return;
            display.configure(mobId.toString(),packet.yaw,packet.head,packet.pitch,packet.pose,packet.stride);
        }));
    }
}
