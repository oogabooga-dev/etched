package gg.moonflower.etched.client.radio.source;

import gg.moonflower.etched.client.radio.RadioCancellation;
import gg.moonflower.etched.client.radio.net.RadioHttpTransport;
import gg.moonflower.etched.client.radio.net.RadioHttpTransportFactory;
import gg.moonflower.etched.client.radio.net.RadioNetworkPolicy;

import java.util.Objects;

public record RadioResolveContext(RadioHttpTransport transport, RadioNetworkPolicy networkPolicy,
                                  RadioCancellation cancellation, RadioResolveLimits limits) {

    public RadioResolveContext {
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(networkPolicy, "networkPolicy");
        Objects.requireNonNull(cancellation, "cancellation");
        Objects.requireNonNull(limits, "limits");
    }

    public static RadioResolveContext createDefault(RadioCancellation cancellation) {
        RadioHttpTransportFactory.Components components = RadioHttpTransportFactory.createDefaultComponents();
        return new RadioResolveContext(components.transport(), components.networkPolicy(),
                cancellation, RadioResolveLimits.DEFAULT);
    }
}
