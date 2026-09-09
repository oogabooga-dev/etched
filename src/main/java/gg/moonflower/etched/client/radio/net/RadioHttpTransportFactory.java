package gg.moonflower.etched.client.radio.net;

import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;

/**
 * Composes the production transport from client-owned proxy and security settings.
 */
public final class RadioHttpTransportFactory {

    private RadioHttpTransportFactory() {
    }

    public static RadioHttpTransport createDefault() {
        return new HttpUrlConnectionRadioHttpTransport(
                Minecraft.getInstance().getProxy(),
                new DefaultRadioNetworkPolicy(Etched.CLIENT_CONFIG.allowPrivateNetworkStations::get),
                HttpUrlConnectionRadioHttpTransport.DEFAULT_CONNECT_TIMEOUT,
                HttpUrlConnectionRadioHttpTransport.DEFAULT_READ_TIMEOUT,
                HttpUrlConnectionRadioHttpTransport.DEFAULT_MAX_REDIRECTS);
    }
}
