package studio.detemkur.museummannequins;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class MannequinScreen extends Screen {
    private final BlockPos pos;
    private final String mob;
    private final float[] values;
    private final EditBox[] fields = new EditBox[5];
    private int pose;
    private Component status = Component.empty();
    MannequinScreen(MannequinEntity display) {
        super(Component.translatable("museummannequins.editor"));
        pos = display.getBlockPos(); mob = display.mob; pose = display.pose;
        values = new float[]{display.yaw,display.head,display.pitch,display.stride};
    }
    @Override protected void init() {
        String[] previous = new String[5];
        for (int i=0;i<5;i++) previous[i] = fields[i] == null ? (i==0 ? mob : Float.toString(values[i-1])) : fields[i].getValue();
        int x=width/2-120,y=height/2-100;
        String[] labels={"mob","yaw","head","pitch","stride"};
        for (int i=0;i<5;i++) {
            fields[i] = new EditBox(font,x+90,y+i*27,150,20,Component.translatable("museummannequins."+labels[i]));
            fields[i].setMaxLength(i==0 ? 128 : 10);
            fields[i].setValue(previous[i]);
            addRenderableWidget(fields[i]);
        }
        addRenderableWidget(Button.builder(poseLabel(),button -> {
            pose=(pose+1)%MannequinEntity.POSES.length; button.setMessage(poseLabel());
        }).bounds(x,y+140,240,20).build());
        addRenderableWidget(Button.builder(Component.translatable("museummannequins.apply"),button -> apply())
            .bounds(x,y+166,115,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),button -> onClose())
            .bounds(x+125,y+166,115,20).build());
    }
    private Component poseLabel() { return Component.translatable("museummannequins.pose."+pose); }
    private void apply() {
        try {
            float yaw=number(1),head=number(2),pitch=number(3),stride=number(4);
            if (!ClientPlayNetworking.canSend(MuseumMannequins.Configure.TYPE)) return;
            ClientPlayNetworking.send(new MuseumMannequins.Configure(pos,fields[0].getValue().trim(),yaw,head,pitch,pose,stride));
            status=Component.translatable("museummannequins.sent");
        } catch (IllegalArgumentException error) { status=Component.translatable("museummannequins.invalid"); }
    }
    private float number(int index) {
        float value=Float.parseFloat(fields[index].getValue().replace(',','.'));
        if (!Float.isFinite(value)) throw new IllegalArgumentException();
        return value;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        int x=width/2-120,y=height/2-100;
        graphics.fill(x-12,y-29,x+252,y+220,0xee25272e);
        graphics.centeredText(font,title,width/2,y-20,0xffe7cd9e);
        String[] labels={"mob","yaw","head","pitch","stride"};
        for (int i=0;i<5;i++) graphics.text(font,Component.translatable("museummannequins."+labels[i]),x,y+6+i*27,0xffffffff,false);
        graphics.centeredText(font,status,width/2,y+196,0xffffd194);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
    }
    @Override public boolean isPauseScreen() { return false; }
}
