package com.aviscribe.service.impl;

import com.aviscribe.entity.Task;
import com.aviscribe.service.SpeechToTextService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Service
@Slf4j
public class SpeechToTextServiceImpl implements SpeechToTextService {

    // NLS FileTrans 常量 / 配置（来自官方示例）
    @Value("${aviscribe.stt.region-id:cn-shanghai}")
    private String regionId;

    @Value("${aviscribe.stt.endpoint-name:cn-shanghai}")
    private String endpointName;

    @Value("${aviscribe.stt.product:nls-filetrans}")
    private String product;

    @Value("${aviscribe.stt.domain:filetrans.cn-shanghai.aliyuncs.com}")
    private String fileTransDomain;

    @Value("${aviscribe.stt.api-version:2018-08-17}")
    private String apiVersion;

    @Value("${aviscribe.stt.submit-action:SubmitTask}")
    private String submitAction;

    @Value("${aviscribe.stt.query-action:GetTaskResult}")
    private String queryAction;

    @Value("${aviscribe.stt.app-key:}")
    private String appKey;

    @Value("${aviscribe.stt.access-key-id:}")
    private String accessKeyId;

    @Value("${aviscribe.stt.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aviscribe.stt.max-poll-times:60}")
    private int maxPollTimes;

    @Value("${aviscribe.stt.poll-interval-ms:3000}")
    private long pollIntervalMs;

    /**
     * 提供给阿里云访问音频文件的公网基址，例如 http://your-domain/audio/
     * 实际的文件名由本地音频文件名拼接。
     */
    @Value("${aviscribe.stt.public-audio-base-url:http://127.0.0.1:8090/audio/}")
    private String publicAudioBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OssUploadService ossUploadService;

    @Autowired
    public SpeechToTextServiceImpl(OssUploadService ossUploadService) {
        this.ossUploadService = ossUploadService;
    }

    @Override
    public String transcribe(Task task, String audioPath) {
        if (audioPath == null || audioPath.isEmpty()) {
            throw new IllegalArgumentException("audioPath 不能为空");
        }
        File audioFile = new File(audioPath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            throw new IllegalArgumentException("音频文件不存在: " + audioPath);
        }
        try {
            String result = fileTransRecognize(task, audioFile);
            log.info("[STT] 识别完成, taskId={}, length={} chars",
                    task != null ? task.getId() : null,
                    result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("[STT] 调用阿里云录音文件识别出错", e);
            throw new RuntimeException("调用语音识别服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 NLS FileTrans SubmitTask / GetTaskResult 实现录音文件识别。
     */
    private String fileTransRecognize(Task task, File audioFile) throws Exception {
        // 1. 先将本地音频上传到 OSS，获取公网可访问的 file_link
        String fileLink;
        try {
            fileLink = ossUploadService.uploadAudio(audioFile);
        } catch (Exception e) {
            log.error("[STT] 上传音频到 OSS 失败，将回退使用本地 publicAudioBaseUrl: {}", e.getMessage(), e);
            String fileName = audioFile.getName();
            fileLink = publicAudioBaseUrl;
            if (!fileLink.endsWith("/")) {
                fileLink += "/";
            }
            fileLink += URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        }

        log.info("[STT] 音频已准备，开始提交 NLS FileTrans");

        // 2. 创建 NLS FileTrans 客户端
        DefaultProfile.addEndpoint(endpointName, regionId, product, fileTransDomain);
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        // 3. 提交任务（SubmitTask）
        String taskId = submitFileTransTask(client, appKey, fileLink);
        if (taskId == null || taskId.isEmpty()) {
            throw new RuntimeException("提交录音文件识别请求失败，TaskId 为空");
        }
        log.info("[STT] 录音文件识别请求成功，task_id={}", taskId);

        // 4. 轮询获取结果（GetTaskResult）
        return queryFileTransResult(client, taskId);
    }

    private String submitFileTransTask(IAcsClient client, String appKey, String fileLink) throws ClientException {
        CommonRequest postRequest = new CommonRequest();
        postRequest.setDomain(fileTransDomain);
        postRequest.setVersion(apiVersion);
        postRequest.setAction(submitAction);
        postRequest.setProduct(product);

        JsonNode taskObject = objectMapper.createObjectNode()
                .put("appkey", appKey)
                .put("file_link", fileLink)
                .put("version", "4.0")
                .put("enable_words", true);
        String taskJson = taskObject.toString();
        log.debug("[STT] SubmitTask 请求已构造");

        postRequest.putBodyParameter("Task", taskJson);
        postRequest.setMethod(MethodType.POST);

        CommonResponse postResponse = client.getCommonResponse(postRequest);
        String data = postResponse.getData();
        if (postResponse.getHttpStatus() != 200) {
            throw new ClientException("SubmitTask 调用失败, httpStatus=" + postResponse.getHttpStatus());
        }

        JsonNode result = parseResponse(data);
        String statusText = result.path("StatusText").asText();
        if (!"SUCCESS".equalsIgnoreCase(statusText)) {
            throw new ClientException("SubmitTask 返回非 SUCCESS, StatusText=" + statusText);
        }
        return result.path("TaskId").asText();
    }

    private String queryFileTransResult(IAcsClient client, String taskId) throws InterruptedException, ClientException {
        CommonRequest getRequest = new CommonRequest();
        getRequest.setDomain(fileTransDomain);
        getRequest.setVersion(apiVersion);
        getRequest.setAction(queryAction);
        getRequest.setProduct(product);
        getRequest.putQueryParameter("TaskId", taskId);
        getRequest.setMethod(MethodType.GET);

        String resultText = null;
        for (int i = 0; i < maxPollTimes; i++) {
            CommonResponse getResponse = client.getCommonResponse(getRequest);
            String data = getResponse.getData();
            if (getResponse.getHttpStatus() != 200) {
                throw new ClientException("GetTaskResult 调用失败, httpStatus=" + getResponse.getHttpStatus());
            }

            JsonNode root = parseResponse(data);
            String statusText = root.path("StatusText").asText();
            if ("RUNNING".equalsIgnoreCase(statusText) || "QUEUEING".equalsIgnoreCase(statusText)) {
                Thread.sleep(pollIntervalMs);
                continue;
            }

            if ("SUCCESS".equalsIgnoreCase(statusText)) {
                resultText = root.path("Result").asText(null);
                if (resultText == null) {
                    resultText = ""; // 可能是静音等情况
                }
                break;
            } else {
                throw new ClientException("GetTaskResult 返回错误状态, StatusText=" + statusText + ", data=" + data);
            }
        }

        if (resultText == null) {
            throw new ClientException("在最大轮询次数内未获得识别结果, taskId=" + taskId);
        }
        return resultText;
    }

    private JsonNode parseResponse(String data) throws ClientException {
        try {
            return objectMapper.readTree(data);
        } catch (Exception ex) {
            throw new ClientException("第三方服务返回了无效 JSON: " + ex.getMessage());
        }
    }
}
