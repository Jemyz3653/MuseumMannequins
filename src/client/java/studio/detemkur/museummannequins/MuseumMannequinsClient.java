package studio.detemkur.museummannequins;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public final class MuseumMannequinsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockEntityRendererRegistry.register(MuseumMannequins.ENTITY,MannequinRenderer::new);
        UseBlockCallback.EVENT.register((player,level,hand,hit) -> {
            if (!level.isClientSide() || player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()
                    || !(level.getBlockEntity(hit.getBlockPos()) instanceof MannequinEntity display)) return InteractionResult.PASS;
            Minecraft.getInstance().setScreenAndShow(new MannequinScreen(display));
            return InteractionResult.SUCCESS;
        });
    }
}
