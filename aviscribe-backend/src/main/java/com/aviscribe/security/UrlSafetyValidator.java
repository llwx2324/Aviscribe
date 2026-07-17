package com.aviscribe.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Validates user supplied download URLs before any outbound connection is made. */
@Component
public class UrlSafetyValidator {

    public URI validate(String rawUrl) {
        return validateAndResolve(rawUrl).uri();
    }

    public ValidatedUrl validateAndResolve(String rawUrl) {
        final URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("URL 格式无效", ex);
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("仅支持 HTTP 或 HTTPS URL");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL 不允许包含用户凭据");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL 缺少有效主机名");
        }

        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("URL 主机无法解析", ex);
        }
        if (addresses.length == 0) {
            throw new IllegalArgumentException("URL 主机无法解析");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new IllegalArgumentException("URL 不允许访问本机、内网或保留地址");
            }
        }
        return new ValidatedUrl(uri, List.of(addresses));
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        if (address instanceof Inet4Address) {
            return isPublicIpv4(address.getAddress());
        }
        if (address instanceof Inet6Address) {
            return isPublicIpv6(address.getAddress());
        }
        return false;
    }

    private boolean isPublicIpv4(byte[] bytes) {
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        int third = bytes[2] & 0xff;
        if (first == 0 || first == 10 || first == 127 || first >= 224) return false;
        if (first == 100 && second >= 64 && second <= 127) return false;
        if (first == 169 && second == 254) return false;
        if (first == 172 && second >= 16 && second <= 31) return false;
        if (first == 192 && second == 168) return false;
        if (first == 192 && second == 0 && (third == 0 || third == 2)) return false;
        if (first == 198 && (second == 18 || second == 19)) return false;
        if (first == 198 && second == 51 && third == 100) return false;
        if (first == 203 && second == 0 && third == 113) return false;
        return true;
    }

    private boolean isPublicIpv6(byte[] bytes) {
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        // fc00::/7 unique-local and 2001:db8::/32 documentation ranges.
        if ((first & 0xfe) == 0xfc) return false;
        return !(first == 0x20 && second == 0x01
                && (bytes[2] & 0xff) == 0x0d && (bytes[3] & 0xff) == 0xb8);
    }

    public record ValidatedUrl(URI uri, List<InetAddress> addresses) {}
}
