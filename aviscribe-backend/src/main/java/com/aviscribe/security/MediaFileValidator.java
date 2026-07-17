package com.aviscribe.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates an uploaded media file by size, extension and signature. */
@Component
public class MediaFileValidator {

    private static final Map<String, Set<String>> SIGNATURE_EXTENSIONS = Map.of(
            "iso-bmff", Set.of(".mp4", ".mov", ".m4a"),
            "ebml", Set.of(".mkv", ".webm"),
            "riff-wave", Set.of(".wav"),
            "riff-avi", Set.of(".avi"),
            "flac", Set.of(".flac"),
            "ogg", Set.of(".ogg"),
            "mp3", Set.of(".mp3"),
            "aac", Set.of(".aac")
    );

    private final long maxUploadSizeBytes;

    public MediaFileValidator(@Value("${aviscribe.file.max-upload-size-bytes:524288000}") long maxUploadSizeBytes) {
        this.maxUploadSizeBytes = maxUploadSizeBytes;
    }

    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new IllegalArgumentException("上传文件超过 500MB 限制");
        }
        String extension = extensionOf(file.getOriginalFilename());
        byte[] header = readHeader(file);
        String signature = detectSignature(header);
        Set<String> compatibleExtensions = SIGNATURE_EXTENSIONS.get(signature);
        if (compatibleExtensions == null || !compatibleExtensions.contains(extension)) {
            throw new IllegalArgumentException("文件内容与受支持的音视频格式不匹配");
        }
        return extension;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("文件名缺少受支持的扩展名");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("文件名缺少受支持的扩展名");
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(16);
        } catch (IOException ex) {
            throw new IllegalArgumentException("无法读取上传文件", ex);
        }
    }

    private String detectSignature(byte[] h) {
        if (matches(h, 4, "ftyp")) return "iso-bmff";
        if (startsWith(h, 0x1a, 0x45, 0xdf, 0xa3)) return "ebml";
        if (matches(h, 0, "RIFF") && matches(h, 8, "WAVE")) return "riff-wave";
        if (matches(h, 0, "RIFF") && matches(h, 8, "AVI ")) return "riff-avi";
        if (matches(h, 0, "fLaC")) return "flac";
        if (matches(h, 0, "OggS")) return "ogg";
        if (h.length >= 2 && (h[0] & 0xff) == 0xff && ((h[1] & 0xf6) == 0xf0)) return "aac";
        if (matches(h, 0, "ID3") || (h.length >= 2 && (h[0] & 0xff) == 0xff
                && ((h[1] & 0xe0) == 0xe0))) return "mp3";
        return "unknown";
    }

    private boolean matches(byte[] bytes, int offset, String expected) {
        byte[] value = expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (bytes.length < offset + value.length) return false;
        for (int i = 0; i < value.length; i++) {
            if (bytes[offset + i] != value[i]) return false;
        }
        return true;
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xff) != expected[i]) return false;
        }
        return true;
    }
}
