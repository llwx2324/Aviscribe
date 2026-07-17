package com.aviscribe.service;

import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.entity.Task;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaskRecoveryService {

    private final TaskService taskService;
    private final JobProcessService jobProcessService;
    private final boolean recoveryEnabled;

    public TaskRecoveryService(TaskService taskService,
                               JobProcessService jobProcessService,
                               @Value("${aviscribe.jobs.recovery-enabled:true}") boolean recoveryEnabled) {
        this.taskService = taskService;
        this.jobProcessService = jobProcessService;
        this.recoveryEnabled = recoveryEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        if (!recoveryEnabled) {
            return;
        }
        List<Integer> recoverableStatuses = List.of(
                TaskStatus.PENDING.getCode(),
                TaskStatus.DOWNLOADING.getCode(),
                TaskStatus.EXTRACTING_AUDIO.getCode(),
                TaskStatus.TRANSCRIBING.getCode(),
                TaskStatus.FORMATTING.getCode()
        );
        List<Task> tasks = taskService.list(new LambdaQueryWrapper<Task>()
                .in(Task::getTaskStatus, recoverableStatuses)
                .orderByAsc(Task::getCreateTime));
        if (!tasks.isEmpty()) {
            log.info("发现 {} 个中断任务，开始恢复调度", tasks.size());
        }
        tasks.forEach(task -> jobProcessService.processTask(task.getId()));
    }
}
