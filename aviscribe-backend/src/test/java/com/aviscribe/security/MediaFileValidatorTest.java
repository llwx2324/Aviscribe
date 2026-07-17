package com.aviscribe.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MediaFileValidatorTest {

    @Test
    void acceptsMatchingMediaSignatureAndExtension() {
        MediaFileValidator validator = new MediaFileValidator(1024);
        byte[] mp4 = new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4);

        assertThat(validator.validate(file)).isEqualTo(".mp4");
    }

    @Test
    void rejectsHtmlRenamedAsMedia() {
        MediaFileValidator validator = new MediaFileValidator(1024);
        MockMultipartFile file = new MockMultipartFile(
                "file", "attack.mp4", "video/mp4", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件内容");
    }

    @Test
    void rejectsSignatureExtensionMismatchAndOversizedFiles() {
        byte[] mp4 = new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        MediaFileValidator normalValidator = new MediaFileValidator(1024);
        MockMultipartFile mismatch = new MockMultipartFile("file", "clip.mp3", "audio/mpeg", mp4);
        assertThatThrownBy(() -> normalValidator.validate(mismatch))
                .isInstanceOf(IllegalArgumentException.class);

        MediaFileValidator smallLimitValidator = new MediaFileValidator(8);
        MockMultipartFile oversized = new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4);
        assertThatThrownBy(() -> smallLimitValidator.validate(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500MB");
    }
}
