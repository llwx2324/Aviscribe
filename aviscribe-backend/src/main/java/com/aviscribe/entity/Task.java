package com.aviscribe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskName;
    private Integer sourceType;
    private String videoUrl;
    private String videoLocalPath;
    private String audioLocalPath;
    private Integer taskStatus;
    private String rawText;
    private String formattedText;
    private String errorLog;
    private Integer durationSeconds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishTime;
}