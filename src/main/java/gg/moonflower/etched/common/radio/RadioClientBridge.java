package gg.moonflower.etched.common.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;

/**
 * Passes radio lifecycle events to the physical client without referencing client-only classes.
 */
public final class RadioClientBridge {

    private static volatile Listener listener = Listener.NOOP;
    private static ResourceKey<Level> openedMenuDimension;
    private static BlockPos openedMenuPos;

    private RadioClientBridge() {
    }

    public static void install(Listener listener) {
        RadioClientBridge.listener = Objects.requireNonNull(listener, "listener");
    }

    public static void update(Level level, BlockPos pos, RadioConfiguration configuration) {
        if (level.isClientSide()) {
            listener.update(level.dimension(), pos.immutable(), configuration);
        }
    }

    public static void remove(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            listener.remove(level.dimension(), pos.immutable());
        }
    }

    public static void tick(Level level, BlockPos pos, RadioConfiguration configuration) {
        if (level.isClientSide()) {
            listener.tick(level.dimension(), pos.immutable(), configuration);
        }
    }

    public static boolean isPlaying(Level level, BlockPos pos) {
        return level.isClientSide() && listener.isPlaying(level.dimension(), pos.immutable());
    }

    public static synchronized void openMenu(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            openedMenuDimension = level.dimension();
            openedMenuPos = pos.immutable();
        }
    }

    public static synchronized Optional<BlockPos> consumeOpenedMenu(Level level) {
        BlockPos pos = level.dimension().equals(openedMenuDimension) ? openedMenuPos : null;
        openedMenuDimension = null;
        openedMenuPos = null;
        return Optional.ofNullable(pos);
    }

    public interface Listener {

        Listener NOOP = new Listener() {
            @Override
            public void update(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration) {
            }

            @Override
            public void remove(ResourceKey<Level> dimension, BlockPos pos) {
            }

            @Override
            public void tick(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration) {
            }

            @Override
            public boolean isPlaying(ResourceKey<Level> dimension, BlockPos pos) {
                return false;
            }
        };

        void update(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration);

        void remove(ResourceKey<Level> dimension, BlockPos pos);

        void tick(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration);

        boolean isPlaying(ResourceKey<Level> dimension, BlockPos pos);
    }
}
