package gg.moonflower.etched.client.radio;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.core.mixin.client.GuiAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Keeps radio overlays and nearby-record state aligned with actual playback. */
final class MinecraftRadioPlaybackEffects implements RadioPlaybackEffects {

    private final Map<RadioKey, ActiveEffect> active = new HashMap<>();

    @Override
    public void update(RadioKey key, RadioSession.Snapshot snapshot) {
        if (snapshot.state() != RadioPlaybackState.PLAYING) {
            this.stop(key);
            return;
        }
        ClientLevel level = getLevel(key);
        if (level == null) {
            this.stop(key);
            return;
        }

        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class, new AABB(key.pos()).inflate(3.45))) {
            living.setRecordPlayingNearby(key.pos(), true);
        }

        ActiveEffect effect = this.active.computeIfAbsent(key, ignored -> new ActiveEffect());
        if (!effect.initialized || !Objects.equals(effect.title, snapshot.streamTitle())) {
            if (effect.overlay != null) {
                GuiAccessor gui = (GuiAccessor) Minecraft.getInstance().gui;
                if (gui.getOverlayMessageString() == effect.overlay) {
                    gui.setOverlayMessageTime(0);
                }
                effect.overlay = null;
            }
            effect.initialized = true;
            effect.title = snapshot.streamTitle();
        }
        if (effect.overlay == null) {
            effect.overlay = this.showOverlay(key, snapshot.streamTitle());
        }
    }

    @Override
    public void stop(RadioKey key) {
        ActiveEffect effect = this.active.remove(key);
        if (effect == null) {
            return;
        }

        ClientLevel level = getLevel(key);
        if (level != null) {
            for (LivingEntity living : level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(key.pos()).inflate(3.45))) {
                living.setRecordPlayingNearby(key.pos(), false);
            }
        }
        if (effect.overlay != null) {
            GuiAccessor gui = (GuiAccessor) Minecraft.getInstance().gui;
            if (gui.getOverlayMessageString() == effect.overlay) {
                gui.setOverlayMessageTime(0);
            }
        }
        this.refreshActiveNearbyState();
    }

    @Nullable
    private Component showOverlay(RadioKey key, @Nullable String streamTitle) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = getLevel(key);
        if (level == null || !level.getBlockState(key.pos().above()).isAir()
                || !PlayableRecord.canShowMessage(
                key.pos().getX() + 0.5, key.pos().getY() + 0.5, key.pos().getZ() + 0.5)) {
            return null;
        }

        Component overlay = streamTitle == null
                ? Component.translatable("sound_source.etched.radio")
                : Component.literal(streamTitle);
        minecraft.gui.setOverlayMessage(overlay, true);
        return overlay;
    }

    private void refreshActiveNearbyState() {
        for (RadioKey activeKey : this.active.keySet()) {
            ClientLevel level = getLevel(activeKey);
            if (level == null) {
                continue;
            }
            for (LivingEntity living : level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(activeKey.pos()).inflate(3.45))) {
                living.setRecordPlayingNearby(activeKey.pos(), true);
            }
        }
    }

    @Nullable
    private static ClientLevel getLevel(RadioKey key) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension().equals(key.dimension()) ? level : null;
    }

    private static final class ActiveEffect {

        private boolean initialized;
        private String title;
        private Component overlay;
    }
}
