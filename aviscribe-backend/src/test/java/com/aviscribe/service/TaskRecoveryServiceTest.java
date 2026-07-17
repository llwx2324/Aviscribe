package com.aviscribe.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.aviscribe.entity.Task;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskRecoveryServiceTest {

    @Test
    void resubmitsInterruptedTasksOnStartup() {
        TaskService taskService = Mockito.mock(TaskService.class);
        JobProcessService jobProcessService = Mockito.mock(JobProcessService.class);
        Task first = task(11L);
        Task second = task(12L);
        when(taskService.list(any(Wrapper.class))).thenReturn(List.of(first, second));

        new TaskRecoveryService(taskService, jobProcessService, true).recoverInterruptedTasks();

        verify(jobProcessService).processTask(11L);
        verify(jobProcessService).processTask(12L);
    }

    @Test
    void canDisableRecoveryForTestsOrMaintenance() {
        TaskService taskService = Mockito.mock(TaskService.class);
        JobProcessService jobProcessService = Mockito.mock(JobProcessService.class);

        new TaskRecoveryService(taskService, jobProcessService, false).recoverInterruptedTasks();

        verify(taskService, never()).list(any(Wrapper.class));
        verify(jobProcessService, never()).processTask(any());
    }

    private Task task(Long id) {
        Task task = new Task();
        task.setId(id);
        return task;
    }
}
