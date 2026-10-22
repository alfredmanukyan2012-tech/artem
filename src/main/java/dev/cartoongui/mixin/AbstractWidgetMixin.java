package dev.cartoongui.mixin;

import dev.cartoongui.Ease;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кнопки: пружинное появление со сдвигом снизу (с лёгкой задержкой по высоте)
 * и мягкое увеличение при наведении.
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Unique private long cg$first = -1L;
    @Unique private long cg$last = 0L;
    @Unique private float cg$scale = 1f;
    @Unique private float cg$vel = 0f;
    @Unique private boolean cg$pushed = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cg$pre(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        if (!(self instanceof AbstractButton) || !self.visible) return;

        long now = Util.getMillis();
        if (cg$first < 0L) {
            cg$first = now;
            cg$last = now;
        }

        // появление: небольшая задержка зависит от Y, чтобы кнопки «каскадом» всплывали
        long delay = (long) Mth.clamp(self.getY() * 0.35f, 0f, 160f);
        long since = now - cg$first - delay;
        if (since < 0L) {
            ci.cancel();
            return;
        }
        float tIn = Mth.clamp(since / 280f, 0f, 1f);
        float e = Ease.outBack(tIn);
        float entryScale = 0.9f + 0.1f * e;
        float offY = (1f - e) * 9f;

        // наведение: пружина масштаба
        float dt = Mth.clamp((now - cg$last) / 1000f, 0f, 0.05f);
        cg$last = now;
        float target = (self.isHoveredOrFocused() && self.active) ? 1.045f : 1f;
        final float omega = 30f;
        final float zeta = 0.7f;
        float accel = -omega * omega * (cg$scale - target) - 2f * zeta * omega * cg$vel;
        cg$vel += accel * dt;
        cg$scale += cg$vel * dt;

        float s = entryScale * cg$scale;
        if (Math.abs(s - 1f) < 0.0005f && offY < 0.01f) return;

        float cx = self.getX() + self.getWidth() / 2f;
        float cy = self.getY() + self.getHeight() / 2f;
        g.pose().pushPose();
        g.pose().translate(cx, cy + offY, 0f);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-cx, -cy, 0f);
        cg$pushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void cg$post(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (cg$pushed) {
            g.pose().popPose();
            cg$pushed = false;
        }
    }
}
