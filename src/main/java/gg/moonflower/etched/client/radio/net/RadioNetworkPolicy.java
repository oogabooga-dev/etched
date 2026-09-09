package gg.moonflower.etched.client.radio.net;

import java.net.URI;

@FunctionalInterface
public interface RadioNetworkPolicy {

    void check(URI uri) throws RadioTransportException;
}
