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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TextFormatServiceImpl implements TextFormatService {

    @Value("${aviscribe.llm.api-key}")
    private String llmApiKey;

    @Value("${aviscribe.llm.base-url}")
    private String baseUrl;

    @Value("${aviscribe.llm.model:deepseek-chat}")
    private String model;

    @Value("${aviscribe.llm.max-concurrent:1}")
    private int maxConcurrent;

    @Value("${aviscribe.llm.acquire-timeout-ms:120000}")
    private long acquireTimeoutMs;

    @Value("${aviscribe.llm.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${aviscribe.llm.retry.backoff-ms:2000}")
    private long retryBackoffMs;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Semaphore llmSemaphore;

    @jakarta.annotation.PostConstruct
    public void initSemaphore() {
        int permits = Math.max(1, maxConcurrent);
        this.llmSemaphore = new Semaphore(permits);
    }

    @Override
    public String format(Task task, String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        String prompt = buildPrompt(task, rawText);
        boolean acquired = false;
        try {
            if (llmSemaphore == null) {
                initSemaphore();
            }
            acquired = llmSemaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new RuntimeException("排版服务繁忙，请稍后重试");
            }

            return executeWithRetry(task, prompt, rawText);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("[LLM] 获取排版令牌被中断", ie);
            return rawText;
        } catch (Exception e) {
            log.error("[LLM] 调用 DeepSeek 排版出错", e);
            return rawText;
        } finally {
            if (acquired && llmSemaphore != null) {
                llmSemaphore.release();
            }
        }
    }

    private String executeWithRetry(Task task, String prompt, String rawText) throws Exception {
        Exception lastException = null;
        int attempts = Math.max(1, maxRetryAttempts);
        for (int i = 1; i <= attempts; i++) {
            try {
                return invokeLlm(task, prompt, rawText);
            } catch (Exception ex) {
                lastException = ex;
                if (i == attempts) {
                    throw ex;
                }
                long backoff = Math.max(0, retryBackoffMs * i);
                log.warn("[LLM] 排版失败，第 {} 次重试将在 {} ms 后进行: {}", i, backoff, ex.getMessage());
                Thread.sleep(backoff);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return rawText;
    }

    private String invokeLlm(Task task, String prompt, String rawText) throws Exception {
        String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("messages", new Object[]{
                Map.of(
                        "role", "system",
                        "content", "你是一名擅长阅读和总结文本的智能助手。\n" +
                                "接下来我会提供一段视频字幕文本，请你根据字幕内容完成以下任务：\n" +
                                "\n" +
                                "对字幕内容进行整体总结，语言简洁清晰。\n" +
                                "\n" +
                                "提取其中的关键信息/要点（如观点、结论、步骤、时间、人物、重要数据等），用条目列出。\n" +
                                "\n" +
                                "自动忽略口头禅、语气词、重复、与主题无关的闲聊内容。\n" +
                                "\n" +
                                "如果内容包含步骤或操作流程，请尽量按步骤顺序整理。\n" +
                                "\n" +
                                "最后给出一条一句话超精简总结，方便快速浏览。"
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