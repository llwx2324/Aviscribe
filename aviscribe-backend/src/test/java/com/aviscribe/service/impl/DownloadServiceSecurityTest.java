package com.aviscribe.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.aviscribe.security.UrlSafetyValidator;
import com.aviscribe.security.UrlSafetyValidator.ValidatedUrl;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DownloadServiceSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsLoopbackBeforeOpeningDownloadConnection() {
        DownloadServiceImpl service = new DownloadServiceImpl(new UrlSafetyValidator());

        assertThatThrownBy(() -> service.download("http://127.0.0.1:8080/private"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("内网");
    }

    @Test
    void revalidatesRedirectBeforeFollowingIt() throws Exception {
        AtomicInteger privateEndpointHits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/private");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/private", exchange -> {
            privateEndpointHits.incrementAndGet();
            byte[] body = "secret".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "video/mp4");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            UrlSafetyValidator redirectBlockingValidator = new UrlSafetyValidator() {
                @Override
                public ValidatedUrl validateAndResolve(String rawUrl) {
                    URI uri = URI.create(rawUrl);
                    if ("/private".equals(uri.getPath())) {
                        throw new IllegalArgumentException("blocked redirect");
                    }
                    try {
                        return new ValidatedUrl(uri, java.util.List.of(java.net.InetAddress.getByName("127.0.0.1")));
                    } catch (java.net.UnknownHostException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            };
            DownloadServiceImpl service = configuredService(redirectBlockingValidator);
            String startUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/start";

            assertThatThrownBy(() -> service.download(startUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blocked redirect");
            assertThat(privateEndpointHits).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    private DownloadServiceImpl configuredService(UrlSafetyValidator validator) {
        DownloadServiceImpl service = new DownloadServiceImpl(validator);
        ReflectionTestUtils.setField(service, "uploadRootPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "maxDownloadSizeBytes", 1024L);
        ReflectionTestUtils.setField(service, "connectTimeoutMs", 1000);
        ReflectionTestUtils.setField(service, "readTimeoutMs", 1000);
        ReflectionTestUtils.setField(service, "processTimeoutSeconds", 1L);
        return service;
    }
}
