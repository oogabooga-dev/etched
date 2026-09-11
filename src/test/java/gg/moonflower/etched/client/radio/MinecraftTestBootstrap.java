package gg.moonflower.etched.client.radio;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Initializes vanilla registries without requiring Forge's ModLauncher event transformation. */
public final class MinecraftTestBootstrap {

    private MinecraftTestBootstrap() {
    }

    public static void bootStrap() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError exception) {
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (!(cause instanceof NoSuchMethodException)
                    || !"net.minecraftforge.network.NetworkEvent.<init>()".equals(cause.getMessage())) {
                throw exception;
            }
        }
    }
}
