package com.aviscribe.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @Test
    void acceptsPublicHttpAddress() {
        URI result = validator.validate("https://8.8.8.8/media.mp4");
        assertThat(result.getHost()).isEqualTo("8.8.8.8");
    }

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1/admin"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("http://10.0.0.8/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("http://[::1]/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonHttpSchemesAndEmbeddedCredentials() {
        assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("http://user:pass@8.8.8.8/video.mp4"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
