package studio.detemkur.museummannequins;

import java.util.WeakHashMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

final class MannequinRenderer implements BlockEntityRenderer<MannequinEntity,MannequinRenderer.State> {
    private final EntityRenderDispatcher dispatcher;
    private final WeakHashMap<MannequinEntity,LivingEntity> previews = new WeakHashMap<>();
    private int nextPreviewId = -1;
    MannequinRenderer(BlockEntityRendererProvider.Context context) { dispatcher = context.entityRenderer(); }
    static final class State extends BlockEntityRenderState {
        EntityRenderState mob;
        float scale, yaw;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(MannequinEntity display, State state, float delta, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumble) {
        BlockEntityRenderer.super.extractRenderState(display,state,delta,camera,crumble);
        state.mob = null;
        var id = Identifier.tryParse(display.mob);
        if (id == null || display.getLevel() == null) return;
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null || !type.canSummon()) return;
        LivingEntity mob = previews.get(display);
        if (mob == null || mob.getType() != type) {
            if (!(type.create(display.getLevel(),EntitySpawnReason.LOAD) instanceof LivingEntity created)) return;
            created.setId(nextPreviewId--);
            mob = created;
            previews.put(display,mob);
        }
        mob.setPos(Vec3.atBottomCenterOf(display.getBlockPos()));
        mob.setPose(MannequinEntity.POSES[display.pose]);
        var rendered = dispatcher.extractEntity(mob,0);
        rendered.ageInTicks = 0;
        rendered.nameTag = null;
        rendered.shadowRadius = 0;
        rendered.lightCoords = state.lightCoords;
        rendered.displayFireAnimation = false;
        rendered.leashStates = null;
        if (rendered instanceof LivingEntityRenderState living) {
            living.bodyRot = 0; living.yRot = display.head; living.xRot = display.pitch;
            living.walkAnimationPos = display.stride / 18F;
            living.walkAnimationSpeed = display.stride == 0 ? 0 : 1;
        }
        state.mob = rendered;
        state.scale = Math.min(.8F / Math.max(.1F,mob.getBbWidth()),.85F / Math.max(.1F,mob.getBbHeight()));
        state.yaw = display.yaw;
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera) {
        if (state.mob == null) return;
        pose.pushPose();
        pose.translate(.5,.075,.5);
        pose.rotateDegrees(Axis.YP,state.yaw);
        pose.scale(state.scale,state.scale,state.scale);
        dispatcher.submit(state.mob,camera,0,0,0,pose,nodes);
        pose.popPose();
    }
}
