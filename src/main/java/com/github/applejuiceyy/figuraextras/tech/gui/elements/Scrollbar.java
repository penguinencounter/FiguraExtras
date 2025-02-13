package com.github.applejuiceyy.figuraextras.tech.gui.elements;

import com.github.applejuiceyy.figuraextras.tech.gui.NinePatch;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.DefaultCancellableEvent;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.util.Observers;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class Scrollbar extends Element implements NumberRangeAlike {
    public boolean enabled = true;
    public Observers.WritableObserver<Float> pos = Observers.of(0f);
    NinePatch thumb = new NinePatch(true, new ResourceLocation("figuraextras", "textures/gui/scrollbar.png"), 0, 0, 5 / 27f, 5 / 15f, 5, 5, 2, 2, 1, 2);
    NinePatch thumbHovered = new NinePatch(true, new ResourceLocation("figuraextras", "textures/gui/scrollbar.png"), 5 / 27f, 0, 5 / 27f, 5 / 15f, 5, 5, 2, 2, 1, 2);
    NinePatch track = new NinePatch(true, new ResourceLocation("figuraextras", "textures/gui/scrollbar.png"), 15 / 27f, 0, 3 / 27f, 3 / 15f, 3, 3, 1, 1, 1, 1);
    NinePatch trackDisabled = new NinePatch(true, new ResourceLocation("figuraextras", "textures/gui/scrollbar.png"), 18 / 27f, 0, 3 / 27f, 3 / 15f, 3, 3, 1, 1, 1, 1);
    private boolean horizontal = false;
    private float size = 100, thumbSize = 10;
    private double dragPos, previousPos;

    {
        pos.observe(() -> enqueueDirtySection(false, false));
        hoveringKind.observe(() -> enqueueDirtySection(false, false));
    }

    public float getSize() {
        return size;
    }

    public Scrollbar setSize(float size) {
        this.size = size;
        enqueueDirtySection(false, false);
        return this;
    }

    public float getThumbSize() {
        return thumbSize;
    }

    public Scrollbar setThumbSize(float thumbSize) {
        this.thumbSize = thumbSize;
        enqueueDirtySection(false, false);
        return this;
    }

    public boolean getHorizontal() {
        return horizontal;
    }

    public Scrollbar setHorizontal(boolean value) {
        horizontal = value;
        enqueueDirtySection(false, false);
        optimalSizeChanged();
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        int v, w;
        if (horizontal) {
            graphics.pose().translate(0, getHeight(), 0);
            graphics.pose().rotateAround(Axis.ZP.rotationDegrees(-90), 0, 0, 0);
            v = getHeight();
            w = getWidth();
        } else {
            v = getWidth();
            w = getHeight();
        }

        if (enabled) {
            track.render(graphics.pose(), 0, 0, v, w);
            (hoveringKind.get() == HoverIntent.INTERACT ? thumbHovered : thumb).render(graphics.pose(), 1, pos.get() / size * w + 1, v - 2, thumbSize / size * w - 2);
        } else {
            trackDisabled.render(graphics.pose(), 0, 0, v, w);
        }

        // graphics.blit(new ResourceLocation("figuraextras", "textures/gui/scrollbar.png"), 0, 0, 0, 5, 12, 10, 27, 15);

        graphics.pose().popPose();
    }

    @Override
    protected boolean renders() {
        return true;
    }

    private double getMousePos(DefaultCancellableEvent.MousePositionEvent event) {
        return horizontal ? event.x - getX() : event.y - getY();
    }

    @Override
    protected void defaultMouseDraggedBehaviour(DefaultCancellableEvent.MousePositionButtonDeltaEvent event) {
        if (enabled) {
            float pos = (float) ((getMousePos(event) - dragPos) / (horizontal ? getWidth() : getHeight()) * size + previousPos);
            pos = pos < 0 ? 0 : pos;
            pos = Math.min(pos, size - thumbSize);
            this.pos.set(pos);
        }
    }

    @Override
    protected void defaultMouseDownBehaviour(DefaultCancellableEvent.MousePositionButtonEvent event) {
        dragPos = getMousePos(event);
        previousPos = this.pos.get();
    }

    @Override
    protected void defaultMouseScrolledBehaviour(DefaultCancellableEvent.MousePositionAmountEvent event) {
        pos.set(Math.min(Math.max(0, (float) (pos.get() - event.amount * 10)), getMax()));
    }

    @Override
    public int computeOptimalWidth() {
        return horizontal ? 10 + 60 + 10 : 10;
    }

    @Override
    public int computeOptimalHeight(int width) {
        return horizontal ? 10 : 10 + 60 + 10;
    }

    @Override
    public Element.HoverIntent mouseHoverIntent(double mouseX, double mouseY) {
        return HoverIntent.INTERACT;
    }

    @Override
    public float getMin() {
        return 0;
    }

    @Override
    public float getMax() {
        return size - thumbSize;
    }

    @Override
    public float getValue() {
        return pos.get();
    }
}
