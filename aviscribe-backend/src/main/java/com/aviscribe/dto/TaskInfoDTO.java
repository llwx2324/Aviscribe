package com.aviscribe.dto;

import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.entity.Task;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskInfoDTO {
    private Long id;
    private Long userId;
    private String taskName;
    private Integer taskStatus;
    private String taskStatusText;
    private Integer sourceType;
    private String videoUrl;
    private String rawText;
    private String formattedText;
    private String errorLog;
    private Integer durationSeconds;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;

    // 转换器：Entity -> DTO
    public static TaskInfoDTO fromEntity(Task task) {
        if (task == null) return null;
        
        TaskInfoDTO dto = new TaskInfoDTO();
        dto.setId(task.getId());
        dto.setUserId(task.getUserId());
        dto.setTaskName(task.getTaskName());
        dto.setTaskStatus(task.getTaskStatus());
        dto.setTaskStatusText(TaskStatus.getDescriptionByCode(task.getTaskStatus()));
        dto.setSourceType(task.getSourceType());
        dto.setVideoUrl(task.getVideoUrl());
        dto.setDurationSeconds(task.getDurationSeconds());
        dto.setCreateTime(task.getCreateTime());
        dto.setFinishTime(task.getFinishTime());
        dto.setErrorLog(task.getErrorLog());

        // 仅在任务完成时返回结果，保护数据传输
        if (task.getTaskStatus() == TaskStatus.COMPLETED.getCode()) {
            dto.setRawText(task.getRawText());
            dto.setFormattedText(task.getFormattedText());
        }
        
        return dto;
    }
}