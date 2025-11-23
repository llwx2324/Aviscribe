package com.aviscribe.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * 将本地生成的音频文件上传到阿里云 OSS，并返回可供语音识别访问的 URL。
 */
@Service
@Slf4j
public class OssUploadService {

    @Value("${aviscribe.oss.endpoint}")
    private String endpoint;

    @Value("${aviscribe.oss.bucket}")
    private String bucketName;

    @Value("${aviscribe.oss.access-key-id:${ALIYUN_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${aviscribe.oss.access-key-secret:${ALIYUN_ACCESS_KEY_SECRET:}}")
    private String accessKeySecret;

    @Value("${aviscribe.oss.object-prefix:audio/}")
    private String objectPrefix;

    @Value("${aviscribe.oss.use-signed-url:false}")
    private boolean useSignedUrl;

    @Value("${aviscribe.oss.signed-url-expire-seconds:86400}")
    private long signedUrlExpireSeconds;

    /**
     * 将本地文件上传到 OSS，并返回可供阿里云语音识别访问的 URL。
     */
    public String uploadAudio(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("待上传的本地文件不存在: " + (file == null ? "null" : file.getAbsolutePath()));
        }
        if (endpoint == null || endpoint.isEmpty() || bucketName == null || bucketName.isEmpty()) {
            throw new IllegalStateException("OSS 配置缺失: endpoint 或 bucket 为空");
        }

        String objectName = buildObjectName(file.getName());

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, file);
            log.info("[OSS] 上传音频成功, bucket={}, object={} (size={}KB)", bucketName, objectName, file.length() / 1024);

            if (useSignedUrl) {
                // 生成带过期时间的签名 URL
                Date expiration = new Date(System.currentTimeMillis() + Duration.ofSeconds(signedUrlExpireSeconds).toMillis());
                String signedUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration).toString();
                log.info("[OSS] 生成签名 URL, 过期时间: {} 秒", signedUrlExpireSeconds);
                return signedUrl;
            } else {
                // 直接返回公共读 URL（需要 bucket 或 object 权限为公共读）
                String encodedObject = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
                String url = String.format("https://%s.%s/%s", bucketName, extractHostFromEndpoint(endpoint), encodedObject);
                log.info("[OSS] 使用公共读 URL: {}", url);
                return url;
            }
        } catch (Exception e) {
            throw new RuntimeException("上传音频到 OSS 失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String buildObjectName(String fileName) {
        String prefix = objectPrefix == null ? "" : objectPrefix;
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + fileName;
    }

    private String extractHostFromEndpoint(String ep) {
        String tmp = ep;
        if (tmp.startsWith("http://")) {
            tmp = tmp.substring(7);
        } else if (tmp.startsWith("https://")) {
            tmp = tmp.substring(8);
        }
        // endpoint 形如 oss-cn-shanghai.aliyuncs.com
        return tmp;
    }
}

