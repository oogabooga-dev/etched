package gg.moonflower.etched.core;

import net.minecraftforge.fml.IExtensionPoint;

import java.util.Objects;

/**
 * Multiplayer compatibility values advertised independently of the fork version.
 */
public final class EtchedCompatibility {

    public static final String ORIGINAL_MOD_VERSION = "3.0.4";
    private static final String FORK_VERSION_PREFIX = "4.";

    private EtchedCompatibility() {
    }

    public static boolean acceptsModVersion(String remoteVersion) {
        return Objects.equals(ORIGINAL_MOD_VERSION, remoteVersion)
                || remoteVersion != null && remoteVersion.startsWith(FORK_VERSION_PREFIX);
    }

    public static IExtensionPoint.DisplayTest displayTest() {
        return new IExtensionPoint.DisplayTest(ORIGINAL_MOD_VERSION,
                (remoteVersion, isFromServer) -> acceptsModVersion(remoteVersion));
    }
}
