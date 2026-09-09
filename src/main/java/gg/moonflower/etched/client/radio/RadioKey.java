package gg.moonflower.etched.client.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Identifies a radio without colliding across dimensions.
 */
public record RadioKey(ResourceKey<Level> dimension, BlockPos pos) {

    public RadioKey {
        Objects.requireNonNull(dimension, "dimension");
        pos = Objects.requireNonNull(pos, "pos").immutable();
    }
}
