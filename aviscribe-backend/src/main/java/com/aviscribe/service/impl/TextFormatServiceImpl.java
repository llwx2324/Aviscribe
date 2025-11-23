package com.aviscribe.service.impl;

import com.aviscribe.entity.Task;
import com.aviscribe.service.TextFormatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TextFormatServiceImpl implements TextFormatService {

    @Value("${aviscribe.llm.api-key}")
    private String llmApiKey;

    @Value("${aviscribe.llm.base-url}")
    private String baseUrl;

    @Value("${aviscribe.llm.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String format(Task task, String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        String prompt = buildPrompt(task, rawText);

        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.3);
            body.put("messages", new Object[]{
                    Map.of(
                            "role", "system",
                            "content", "你是一个专业的文字编辑助手，负责将语音转写的长文本整理成结构清晰、语气自然的文章。"
                    ),
                    Map.of(
                            "role", "user",
                            "content", prompt
                    )
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("[LLM] 调用 DeepSeek 排版, taskId={}, textLen={}",
                    task != null ? task.getId() : null, rawText.length());

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("调用 LLM 失败, status=" + response.getStatusCode());
            }

            String respBody = response.getBody();
            log.debug("[LLM] DeepSeek 响应: {}", respBody);

            return extractContentFromOpenAIStyleResponse(respBody);
        } catch (Exception e) {
            log.error("[LLM] 调用 DeepSeek 排版出错", e);
            // 兜底：出现异常时返回原始文本，以保证主流程不至于完全失败
            return rawText;
        }
    }

    private String buildPrompt(Task task, String rawText) {
        // Task 中目前只有 taskName，没有 title 字段，这里使用 taskName 作为标题提示
        String titleHint = task != null && task.getTaskName() != null ? task.getTaskName() : "";
        return "请将下面的语音转写文本整理成一篇结构清晰的文章，要求：" +
                "1）自动添加和修正标点；2）合理分段；3）如果有明显口语或重复，请适当润色；" +
                "4）在开头给出一个合适的一级标题；5）输出使用 Markdown 格式。" +
                (titleHint.isEmpty() ? "" : "可以参考任务名称：" + titleHint + "。") +
                "\n\n原始文本如下：\n\n" + rawText;
    }

    private String extractContentFromOpenAIStyleResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.warn("[LLM] 响应中没有 choices 字段: {}", json);
            return "";
        }
        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText("");
        return content != null ? content : "";
    }
}