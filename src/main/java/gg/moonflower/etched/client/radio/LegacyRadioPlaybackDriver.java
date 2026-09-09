package gg.moonflower.etched.client.radio;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.radio.RadioConfiguration;
import gg.moonflower.etched.core.mixin.client.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Keeps the original audio path working until the new radio pipeline owns playback.
 */
final class LegacyRadioPlaybackDriver implements RadioPlaybackManager.PlaybackDriver {

    @Override
    public void apply(RadioKey key, RadioConfiguration configuration) {
        ClientLevel level = getLevel(key);
        if (level != null) {
            String url = configuration.isEnabled() ? configuration.url() : null;
            SoundTracker.playRadio(url, level.getBlockState(key.pos()), level, key.pos());
        }
    }

    @Override
    public void stop(RadioKey key) {
        ClientLevel level = getLevel(key);
        if (level != null) {
            SoundTracker.playRadio(null, level.getBlockState(key.pos()), level, key.pos());
        }
    }

    @Override
    public void tick(RadioKey key, RadioConfiguration configuration) {
        ClientLevel level = getLevel(key);
        if (level == null || !this.isPlaying(key)) {
            return;
        }

        AABB range = new AABB(key.pos()).inflate(3.45);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, range)) {
            living.setRecordPlayingNearby(key.pos(), true);
        }
    }

    @Override
    public boolean isPlaying(RadioKey key) {
        ClientLevel level = getLevel(key);
        if (level == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, SoundInstance> sounds =
                ((LevelRendererAccessor) minecraft.levelRenderer).getPlayingRecords();
        SoundInstance sound = sounds.get(key.pos());
        return sound != null && minecraft.getSoundManager().isActive(sound);
    }

    @Nullable
    private static ClientLevel getLevel(RadioKey key) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension().equals(key.dimension()) ? level : null;
    }
}
