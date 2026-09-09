package gg.moonflower.etched.client.radio.net;

import gg.moonflower.etched.core.Etched;
import net.minecraft.client.Minecraft;

import java.util.Objects;

/**
 * Composes the production transport from client-owned proxy and security settings.
 */
public final class RadioHttpTransportFactory {

    private RadioHttpTransportFactory() {
    }

    public static RadioHttpTransport createDefault() {
        return createDefaultComponents().transport();
    }

    public static Components createDefaultComponents() {
        RadioNetworkPolicy networkPolicy = new DefaultRadioNetworkPolicy(
                Etched.CLIENT_CONFIG.allowPrivateNetworkStations::get);
        RadioHttpTransport transport = new HttpUrlConnectionRadioHttpTransport(
                Minecraft.getInstance().getProxy(),
                networkPolicy,
                HttpUrlConnectionRadioHttpTransport.DEFAULT_CONNECT_TIMEOUT,
                HttpUrlConnectionRadioHttpTransport.DEFAULT_READ_TIMEOUT,
                HttpUrlConnectionRadioHttpTransport.DEFAULT_MAX_REDIRECTS);
        return new Components(transport, networkPolicy);
    }

    public record Components(RadioHttpTransport transport, RadioNetworkPolicy networkPolicy) {

        public Components {
            Objects.requireNonNull(transport, "transport");
            Objects.requireNonNull(networkPolicy, "networkPolicy");
        }
    }
}
