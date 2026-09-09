package gg.moonflower.etched.client.radio.net;

import gg.moonflower.etched.client.radio.RadioCancellation;

public interface RadioHttpTransport {

    RadioHttpResponse execute(RadioHttpRequest request, RadioCancellation cancellation)
            throws RadioTransportException;
}
