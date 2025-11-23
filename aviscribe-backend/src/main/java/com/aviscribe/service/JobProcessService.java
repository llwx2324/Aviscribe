package com.aviscribe.service;

import org.springframework.scheduling.annotation.Async;

public interface JobProcessService {
    @Async("taskExecutor")
    void processTask(Long taskId);
}