package dev.cartoongui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.WeakHashMap;

public class CartoonGuiClient implements ClientModInitializer {
    public static final String MOD_ID = "cartoongui";

    private static final ResourceLocation SELECTOR =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "hud/hotbar_selector");

    // ---- анимация открытия контейнерных окон (инвентарь, сундуки, верстак...)
    private static final long OPEN_MS = 230L;
    private static final Map<Screen, Long> OPEN_START = new WeakHashMap<>();
    private static final Map<Screen, Boolean> HOOKED = new WeakHashMap<>();
    private static boolean screenPushed = false;

    // ---- пружина выбора слота хотбара
    private static float selPos = Float.NaN;
    private static float selVel = 0f;
    private static long selLast = 0L;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)) return;
            OPEN_START.putIfAbsent(screen, Util.getMillis());
            if (HOOKED.put(screen, Boolean.TRUE) != null) return;

            ScreenEvents.beforeRender(screen).register((s, g, mx, my, dt) -> {
                long start = OPEN_START.getOrDefault(s, 0L);
                float t = Mth.clamp((Util.getMillis() - start) / (float) OPEN_MS, 0f, 1f);
                if (t >= 1f) return;
                float e = Ease.outBack(t);
                float scale = 0.93f + 0.07f * e;
                float cx = s.width / 2f;
                float cy = s.height / 2f;
                g.pose().pushPose();
                g.pose().translate(cx, cy + (1f - e) * 10f, 0f);
                g.pose().scale(scale, scale, 1f);
                g.pose().translate(-cx, -cy, 0f);
                screenPushed = true;
            });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, dt) -> {
                if (screenPushed) {
                    g.pose().popPose();
                    screenPushed = false;
                }
            });
        });

        HudRenderCallback.EVENT.register(CartoonGuiClient::renderSelector);
    }

    /** Плавно «едущая» рамка выбранного слота хотбара. */
    private static void renderSelector(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        int target = mc.player.getInventory().selected;
        long now = Util.getMillis();
        if (Float.isNaN(selPos)) {
            selPos = target;
            selLast = now;
        }
        float dt = Mth.clamp((now - selLast) / 1000f, 0f, 0.05f);
        selLast = now;

        // критически близкая к демпфированной пружина: ~0.3 c, лёгкий перелёт
        final float omega = 24f;
        final float zeta = 0.72f;
        float accel = -omega * omega * (selPos - target) - 2f * zeta * omega * selVel;
        selVel += accel * dt;
        selPos += selVel * dt;

        int x = Math.round(g.guiWidth() / 2f - 91f - 1f + selPos * 20f);
        int y = g.guiHeight() - 22 - 1;
        g.blitSprite(SELECTOR, x, y, 24, 23);
    }
}
