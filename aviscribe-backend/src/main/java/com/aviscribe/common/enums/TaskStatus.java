package com.aviscribe.common.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING(1, "待处理"),
    DOWNLOADING(2, "下载中"),
    EXTRACTING_AUDIO(3, "音频提取中"),
    TRANSCRIBING(4, "转录中"),
    FORMATTING(5, "排版中"),
    COMPLETED(6, "已完成"),
    FAILED(7, "失败");

    private final int code;
    private final String description;

    TaskStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static String getDescriptionByCode(int code) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getCode() == code) {
                return status.getDescription();
            }
        }
        return "未知";
    }
}