package gg.moonflower.etched.client.radio.net;

import gg.moonflower.etched.client.radio.RadioFailure;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Rejects URLs that can access local or special-purpose network resources.
 *
 * <p>The checked DNS result cannot be pinned through {@link java.net.HttpURLConnection},
 * so a DNS-rebinding window remains between this check and connection setup. A proxy that
 * resolves hostnames remotely must enforce an equivalent destination policy itself.</p>
 */
public final class DefaultRadioNetworkPolicy implements RadioNetworkPolicy {

    private final BooleanSupplier allowPrivateNetworks;
    private final AddressResolver resolver;

    public DefaultRadioNetworkPolicy(BooleanSupplier allowPrivateNetworks) {
        this(allowPrivateNetworks, InetAddress::getAllByName);
    }

    DefaultRadioNetworkPolicy(BooleanSupplier allowPrivateNetworks, AddressResolver resolver) {
        this.allowPrivateNetworks = Objects.requireNonNull(allowPrivateNetworks, "allowPrivateNetworks");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public void check(URI uri) throws RadioTransportException {
        Objects.requireNonNull(uri, "uri");
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        if (!uri.isAbsolute()
                || !normalizedScheme.equals("http") && !normalizedScheme.equals("https")
                || host == null || host.isBlank()
                || uri.getRawUserInfo() != null
                || uri.getPort() == 0 || uri.getPort() > 65535
                || containsControlCharacter(uri.toString())
                || host.contains("%")) {
            throw failure(RadioFailure.Code.INVALID_URL, false,
                    "Radio URL must be an absolute HTTP(S) URL without credentials", null);
        }

        InetAddress[] addresses;
        try {
            addresses = this.resolver.resolve(stripIpv6Brackets(host));
        } catch (UnknownHostException exception) {
            throw failure(RadioFailure.Code.UNKNOWN, true, "Could not resolve the radio host", exception);
        }
        if (addresses == null || addresses.length == 0) {
            throw failure(RadioFailure.Code.UNKNOWN, true, "Could not resolve the radio host", null);
        }

        boolean hasPublic = false;
        boolean hasPrivate = false;
        for (InetAddress address : addresses) {
            if (address == null) {
                throw failure(RadioFailure.Code.BLOCKED_ADDRESS, false,
                        "The radio host resolved to an invalid address", null);
            }
            switch (RadioAddressClassifier.classify(address)) {
                case PUBLIC -> hasPublic = true;
                case PRIVATE -> hasPrivate = true;
                case FORBIDDEN -> throw failure(RadioFailure.Code.BLOCKED_ADDRESS, false,
                        "The radio host resolved to a blocked address", null);
            }
        }

        if (hasPublic && hasPrivate) {
            throw failure(RadioFailure.Code.BLOCKED_ADDRESS, false,
                    "The radio host resolved to mixed public and private addresses", null);
        }
        if (hasPrivate && !this.allowPrivateNetworks.getAsBoolean()) {
            throw failure(RadioFailure.Code.BLOCKED_ADDRESS, false,
                    "Private-network radio stations are disabled", null);
        }
    }

    private static String stripIpv6Brackets(String host) {
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static RadioTransportException failure(RadioFailure.Code code, boolean recoverable,
                                                   String message, Throwable cause) {
        return new RadioTransportException(code, recoverable, message, cause);
    }

    @FunctionalInterface
    interface AddressResolver {

        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
