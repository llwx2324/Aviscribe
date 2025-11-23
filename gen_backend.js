// generate.js
// (请确保已运行 npm install fs-extra)
const fs = require('fs-extra');
const path = require('path');

const baseDir = 'aviscribe-backend';
const basePackage = 'com.aviscribe';
const packagePath = path.join(baseDir, 'src/main/java', ...basePackage.split('.'));
const resourcesPath = path.join(baseDir, 'src/main/resources');
const testPath = path.join(baseDir, 'src/test/java', ...basePackage.split('.'));

// --- 目录结构 ---
const directories = [
    packagePath,
    path.join(packagePath, 'common/enums'),
    path.join(packagePath, 'common/exception'),
    path.join(packagePath, 'common/utils'),
    path.join(packagePath, 'config'),
    path.join(packagePath, 'controller'),
    path.join(packagePath, 'dto'),
    path.join(packagePath, 'entity'),
    path.join(packagePath, 'mapper'),
    path.join(packagePath, 'service'),
    path.join(packagePath, 'service/impl'),
    resourcesPath,
    path.join(resourcesPath, 'mapper'),
    path.join(resourcesPath, 'static'),
    path.join(resourcesPath, 'templates'),
    testPath,
];

// --- 文件内容模板 ---
const files = {
    // --- POM (Part 2) ---
    [path.join(baseDir, 'pom.xml')]: `
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version> <relativePath/> </parent>
    <groupId>com.aviscribe</groupId>
    <artifactId>aviscribe-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>aviscribe-backend</name>
    <description>Aviscribe Backend Service</description>
    
    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>\${mybatis-plus.version}</version>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.22</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
    `,

    // --- application.yml (Part 2) ---
    [path.join(resourcesPath, 'application.yml')]: `
server:
  port: 8080
  servlet:
    context-path: /api # API 根路径

spring:
  application:
    name: aviscribe-backend
  
  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/aviscribe_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: root
    password: your_mysql_password # !!! 替换为你的 MySQL 密码
    driver-class-name: com.mysql.cj.jdbc.Driver

  # MyBatis Plus 配置
  mybatis-plus:
    mapper-locations: classpath:/mapper/*.xml
    global-config:
      db-config:
        logic-delete-field: false # 
        id-type: auto
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl # 打印 SQL

  # 文件上传配置
  servlet:
    multipart:
      enabled: true
      max-file-size: 1024MB # 1GB
      max-request-size: 1024MB

  # 异步线程池配置 (见 ThreadPoolConfig.java)
  task:
    execution:
      pool:
        core-size: 4  # 核心线程数 (根据 CPU 核心数调整)
        max-size: 16  # 最大线程数
        queue-capacity: 100 # 队列容量

# 自定义配置
aviscribe:
  file:
    # !!! 替换为你服务器上用于存储的真实路径
    upload-path: "/data/aviscribe/files/" 
  ffmpeg:
    # !!! 替换为你服务器上 ffmpeg 的可执行文件路径
    path: "/usr/bin/ffmpeg" 
  stt:
    # 示例：STT API 的 Key (应使用 Spring secrets)
    api-key: "YOUR_STT_API_KEY"
  llm:
    api-key: "YOUR_LLM_API_KEY"
    
logging:
  level:
    com.aviscribe: debug
    
`,

    // --- Main Application ---
    [path.join(packagePath, 'AviscribeBackendApplication.java')]: `
package com.aviscribe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.aviscribe.mapper")
@EnableAsync // 关键：开启异步支持
public class AviscribeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AviscribeBackendApplication.class, args);
    }

}
    `,

    // --- Config ---
    [path.join(packagePath, 'config/ThreadPoolConfig.java')]: `
package com.aviscribe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Value("\${spring.task.execution.pool.core-size}")
    private int coreSize;
    @Value("\${spring.task.execution.pool.max-size}")
    private int maxSize;
    @Value("\${spring.task.execution.pool.queue-capacity}")
    private int queueCapacity;

    @Bean("taskExecutor") // 对应 @Async("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("aviscribe-task-");
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
    `,
    [path.join(packagePath, 'config/MyBatisPlusConfig.java')]: `
package com.aviscribe.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
    `,

    // --- Common ---
    [path.join(packagePath, 'common/enums/SourceType.java')]: `
package com.aviscribe.common.enums;
// ... (此处省略了 getter, ctor, 参照 TaskStatus)
public enum SourceType { LOCAL(1), URL(2); 
    private final int value;
    SourceType(int value) { this.value = value; }
    public int getValue() { return value; }
}
    `,
    [path.join(packagePath, 'common/enums/TaskStatus.java')]: `
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
    `,
    [path.join(packagePath, 'common/utils/FileUtils.java')]: `
package com.aviscribe.common.utils;

import org.springframework.stereotype.Component;
import java.nio.file.Path;
// ... (可使用 Hutool)

@Component
public class FileUtils {
    // TODO: 实施文件保存、删除、获取安全路径的逻辑
    // (例如：防止路径遍历攻击)
    public Path getSafeUploadPath(String baseDir, String filename) {
        // ...
        return null;
    }
}
    `,
    [path.join(packagePath, 'common/utils/FfmpegUtils.java')]: `
package com.aviscribe.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FfmpegUtils {
    private static final Logger log = LoggerFactory.getLogger(FfmpegUtils.class);

    @Value("\${aviscribe.ffmpeg.path}")
    private String ffmpegPath;

    /**
     * @param inputVideoPath
     * @param outputAudioPath
     * @return 成功/失败
     */
    public boolean extractAudio(String inputVideoPath, String outputAudioPath) {
        log.info("Starting audio extraction for: {}", inputVideoPath);
        // TODO: 使用 ProcessBuilder 执行 ffmpeg 命令
        // E.g.: [ffmpegPath, "-i", inputVideoPath, "-vn", "-acodec", "copy", outputAudioPath]
        // E.g.: [ffmpegPath, "-i", inputVideoPath, "-q:a", "0", "-map", "a", outputAudioPath]
        // 需要处理进程的 SdtOut/StdErr，并等待其完成
        log.warn("FfmpegUtils.extractAudio is not implemented. Please install ffmpeg.");
        // 模拟耗时
        try { Thread.sleep(5000); } catch (InterruptedException e) {} 
        // return process.waitFor() == 0;
        return true; // 假设成功 (Stub)
    }
}
    `,

    // --- Entity ---
    [path.join(packagePath, 'entity/Task.java')]: `
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
    `,

    // --- Mapper ---
    [path.join(packagePath, 'mapper/TaskMapper.java')]: `
package com.aviscribe.mapper;

import com.aviscribe.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
    `,

    // --- DTOs ---
    [path.join(packagePath, 'dto/UploadUrlRequest.java')]: `
package com.aviscribe.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

@Data
public class UploadUrlRequest {
    @NotNull(message = "URL 不能为空")
    @URL(message = "无效的 URL 格式")
    private String url;
    
    private String taskName; // 可选的任务名
}
    `,
    [path.join(packagePath, 'dto/TaskInfoDTO.java')]: `
package com.aviscribe.dto;

import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.entity.Task;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskInfoDTO {
    private Long id;
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
    `,

    // --- Service Interfaces ---
    [path.join(packagePath, 'service/TaskService.java')]: `
package com.aviscribe.service;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.entity.Task;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable; // Spring Data Pageable

public interface TaskService extends IService<Task> {
    TaskInfoDTO getTaskInfo(Long id);
    Page<TaskInfoDTO> listTasks(Pageable pageable);
    void deleteTask(Long id); // 需删除文件
    void updateTaskStatus(Long taskId, com.aviscribe.common.enums.TaskStatus status);
    void updateTaskError(Long taskId, String error);
}
    `,
    [path.join(packagePath, 'service/UploadService.java')]: `
package com.aviscribe.service;

import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    Task handleLocalUpload(MultipartFile file);
    Task handleUrlUpload(UploadUrlRequest request);
}
    `,
    [path.join(packagePath, 'service/JobProcessService.java')]: `
package com.aviscribe.service;

import org.springframework.scheduling.annotation.Async;

public interface JobProcessService {
    @Async("taskExecutor")
    void processTask(Long taskId);
}
    `,
    [path.join(packagePath, 'service/AudioExtractService.java')]: `
package com.aviscribe.service;
public interface AudioExtractService { String extractAudio(Task task); }
    `,
    [path.join(packagePath, 'service/SpeechToTextService.java')]: `
package com.aviscribe.service;
public interface SpeechToTextService { String transcribe(Task task, String audioPath); }
    `,
    [path.join(packagePath, 'service/TextFormatService.java')]: `
package com.aviscribe.service;
public interface TextFormatService { String format(Task task, String rawText); }
    `,

    // --- Service Implementations (Stubs) ---
    [path.join(packagePath, 'service/impl/TaskServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.entity.Task;
import com.aviscribe.mapper.TaskMapper;
import com.aviscribe.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.domain.Pageable; // Spring Data
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Override
    public TaskInfoDTO getTaskInfo(Long id) {
        Task task = this.getById(id);
        // TODO: 检查用户权限 (V2)
        return TaskInfoDTO.fromEntity(task);
    }

    @Override
    public Page<TaskInfoDTO> listTasks(Pageable pageable) {
        // Spring Data Pageable 转换为 MyBatis Plus Page
        Page<Task> mpPage = new Page<>(pageable.getPageNumber(), pageable.getPageSize());
        // TODO: 按用户ID过滤 (V2)
        // TODO: 按创建时间倒序
        Page<Task> resultPage = this.page(mpPage);
        
        return (Page<TaskInfoDTO>) resultPage.map(TaskInfoDTO::fromEntity);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = this.getById(id);
        if (task != null) {
            // TODO: 删除物理文件
            // new File(task.getVideoLocalPath()).delete();
            // new File(task.getAudioLocalPath()).delete();
            this.removeById(id);
        }
    }
    
    @Override
    public void updateTaskStatus(Long taskId, TaskStatus status) {
        Task update = new Task();
        update.setId(taskId);
        update.setTaskStatus(status.getCode());
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            update.setFinishTime(LocalDateTime.now());
        }
        this.updateById(update);
    }

    @Override
    public void updateTaskError(Long taskId, String error) {
        Task update = new Task();
        update.setId(taskId);
        update.setTaskStatus(TaskStatus.FAILED.getCode());
        update.setErrorLog(error);
        update.setFinishTime(LocalDateTime.now());
        this.updateById(update);
    }
}
    `,
    [path.join(packagePath, 'service/impl/UploadServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.common.enums.SourceType;
import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import com.aviscribe.service.JobProcessService;
import com.aviscribe.service.TaskService;
import com.aviscribe.service.UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Value("\${aviscribe.file.upload-path}")
    private String uploadPath;

    private final TaskService taskService;
    private final JobProcessService jobProcessService;

    public UploadServiceImpl(TaskService taskService, JobProcessService jobProcessService) {
        this.taskService = taskService;
        this.jobProcessService = jobProcessService;
    }

    @Override
    public Task handleLocalUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        Path storageDir = Paths.get(uploadPath);
        try {
            if (!storageDir.toFile().exists()) {
                storageDir.toFile().mkdirs();
            }
            Path destination = Paths.get(uploadPath, newFilename);
            file.transferTo(destination);

            Task task = createTask(originalFilename, SourceType.LOCAL, null, destination.toString());
            // 触发异步处理
            jobProcessService.processTask(task.getId());
            return task;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public Task handleUrlUpload(UploadUrlRequest request) {
        String taskName = (request.getTaskName() != null) ? request.getTaskName() : request.getUrl();
        Task task = createTask(taskName, SourceType.URL, request.getUrl(), null);
        
        // 状态设置为下载中
        taskService.updateTaskStatus(task.getId(), TaskStatus.DOWNLOADING);
        
        // 触发异步处理
        jobProcessService.processTask(task.getId());
        return task;
    }

    private Task createTask(String taskName, SourceType type, String url, String localPath) {
        Task task = new Task();
        task.setTaskName(taskName);
        task.setSourceType(type.getValue());
        task.setVideoUrl(url);
        task.setVideoLocalPath(localPath);
        task.setTaskStatus(TaskStatus.PENDING.getCode());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskService.save(task);
        return task;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
    `,
    [path.join(packagePath, 'service/impl/JobProcessServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.common.enums.SourceType;
import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.entity.Task;
import com.aviscribe.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class JobProcessServiceImpl implements JobProcessService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessServiceImpl.class);

    private final TaskService taskService;
    private final AudioExtractService audioExtractService;
    private final SpeechToTextService speechToTextService;
    private final TextFormatService textFormatService;
    // TODO: private final DownloadService downloadService;

    public JobProcessServiceImpl(TaskService taskService, AudioExtractService audioExtractService, SpeechToTextService speechToTextService, TextFormatService textFormatService) {
        this.taskService = taskService;
        this.audioExtractService = audioExtractService;
        this.speechToTextService = speechToTextService;
        this.textFormatService = textFormatService;
    }

    @Override
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void processTask(Long taskId) {
        log.info("[Task {}] 开始处理...", taskId);
        Task task = taskService.getById(taskId);
        
        try {
            // 1. (可选) 下载
            if (task.getSourceType() == SourceType.URL.getValue()) {
                taskService.updateTaskStatus(taskId, TaskStatus.DOWNLOADING);
                log.info("[Task {}] 正在下载...", taskId);
                // String videoPath = downloadService.download(task.getVideoUrl());
                // task.setVideoLocalPath(videoPath);
                // taskService.updateById(task);
                log.warn("[Task {}] URL 下载功能 (DownloadService) 未实现。", taskId);
                // 假设下载完成
            }

            // 2. 音频提取
            taskService.updateTaskStatus(taskId, TaskStatus.EXTRACTING_AUDIO);
            log.info("[Task {}] 正在提取音频...", taskId);
            String audioPath = audioExtractService.extractAudio(task);
            task.setAudioLocalPath(audioPath);
            taskService.updateById(task);
            
            // 3. STT (语音转文本)
            taskService.updateTaskStatus(taskId, TaskStatus.TRANSCRIBING);
            log.info("[Task {}] 正在转录...", taskId);
            String rawText = speechToTextService.transcribe(task, audioPath);
            task.setRawText(rawText);
            taskService.updateById(task);

            // 4. 文本排版
            taskService.updateTaskStatus(taskId, TaskStatus.FORMATTING);
            log.info("[Task {}] 正在排版...", taskId);
            String formattedText = textFormatService.format(task, rawText);
            task.setFormattedText(formattedText);
            taskService.updateById(task);

            // 5. 完成
            taskService.updateTaskStatus(taskId, TaskStatus.COMPLETED);
            log.info("[Task {}] 处理完成。", taskId);

        } catch (Exception e) {
            log.error("[Task {}] 处理失败: {}", taskId, e.getMessage(), e);
            taskService.updateTaskError(taskId, e.getMessage());
        }
    }
}
    `,
    // STUB Impls
    [path.join(packagePath, 'service/impl/AudioExtractServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.common.utils.FfmpegUtils;
import com.aviscribe.entity.Task;
import com.aviscribe.service.AudioExtractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Paths;

@Service
public class AudioExtractServiceImpl implements AudioExtractService {
    
    @Value("\${aviscribe.file.upload-path}")
    private String uploadPath;
    
    private final FfmpegUtils ffmpegUtils;
    public AudioExtractServiceImpl(FfmpegUtils ffmpegUtils) { this.ffmpegUtils = ffmpegUtils; }

    @Override
    public String extractAudio(Task task) {
        String inputVideo = task.getVideoLocalPath();
        // e.g., /data/aviscribe/files/task-123.mp3
        String outputAudio = Paths.get(uploadPath, "audio-" + task.getId() + ".mp3").toString(); 
        
        // TODO: 调用 FfmpegUtils (FfmpegUtils.java 是 STUB, 需要实现)
        boolean success = ffmpegUtils.extractAudio(inputVideo, outputAudio);
        
        if (!success) {
            throw new RuntimeException("FFmpeg 音频提取失败");
        }
        return outputAudio;
    }
}
    `,
    [path.join(packagePath, 'service/impl/SpeechToTextServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.entity.Task;
import com.aviscribe.service.SpeechToTextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpeechToTextServiceImpl implements SpeechToTextService {

    @Value("\${aviscribe.stt.api-key}")
    private String sttApiKey;

    @Override
    public String transcribe(Task task, String audioPath) {
        // TODO: 在此处集成 STT API (例如 OpenAI Whisper, 阿里云)
        // 使用 WebClient 或 RestTemplate 调用
        
        // 模拟 STT API 调用
        try { Thread.sleep(10000); } catch (InterruptedException e) {} 

        return "这是 STT (SpeechToTextService) API 返回的【模拟】原始文本。\\n" +
               "Aviscribe 项目启动。\\n" +
               "我将作为你的 AI 开发助手。\\n" +
               "我们将采用专业的分步方式推进。";
    }
}
    `,
    [path.join(packagePath, 'service/impl/TextFormatServiceImpl.java')]: `
package com.aviscribe.service.impl;

import com.aviscribe.entity.Task;
import com.aviscribe.service.TextFormatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextFormatServiceImpl implements TextFormatService {

    @Value("\${aviscribe.llm.api-key}")
    private String llmApiKey;

    @Override
    public String format(Task task, String rawText) {
        // TODO: 在此处集成 LLM API (例如 GPT, Claude) 进行排版
        // Prompt 示例: "请将以下语音转录文本整理成一篇结构化文档，修复断句、添加标点、分段，并提取一个合适的标题：[rawText]"
        
        // 模拟 LLM API 调用
        try { Thread.sleep(3000); } catch (InterruptedException e) {} 
        
        String formatted = "## Aviscribe 模拟排版结果\n\n" +
                           "这是 TextFormatService 返回的【模拟】排版后文本。\n\n" +
                           "### 1. 启动\n\n" +
                           "Aviscribe 项目启动。我将作为你的 AI 开发助手。\n\n" +
                           "### 2. 推进\n\n" +
                           "我们将采用专业、分步的方式推进。\n";
        
        return formatted;
    }
}
    `,

    // --- Controllers ---
    [path.join(packagePath, 'controller/UploadController.java')]: `
package com.aviscribe.controller;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import com.aviscribe.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/local")
    public ResponseEntity<TaskInfoDTO> uploadLocal(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            // TODO: 使用全局异常处理
            return ResponseEntity.badRequest().build();
        }
        Task task = uploadService.handleLocalUpload(file);
        return ResponseEntity.accepted().body(TaskInfoDTO.fromEntity(task));
    }

    @PostMapping("/url")
    public ResponseEntity<TaskInfoDTO> uploadUrl(@Validated @RequestBody UploadUrlRequest request) {
        Task task = uploadService.handleUrlUpload(request);
        return ResponseEntity.accepted().body(TaskInfoDTO.fromEntity(task));
    }
}
    `,
    [path.join(packagePath, 'controller/TaskController.java')]: `
package com.aviscribe.controller;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskInfoDTO> getTaskById(@PathVariable Long id) {
        TaskInfoDTO taskInfo = taskService.getTaskInfo(id);
        if (taskInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskInfo);
    }

    @GetMapping("/list")
    public ResponseEntity<Page<TaskInfoDTO>> listTasks(@PageableDefault(size = 10, sort = "createTime") Pageable pageable) {
        Page<TaskInfoDTO> page = taskService.listTasks(pageable);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
    `,
};

// --- 执行生成 ---
async function generateProject() {
    console.log(`开始生成 Aviscribe 后端项目到 ${baseDir} 目录...`);

    try {
        // 清理旧目录
        await fs.remove(baseDir);
        
        // 创建所有目录
        for (const dir of directories) {
            await fs.ensureDir(dir);
        }
        console.log('目录结构创建完毕。');

        // 写入所有文件
        for (const [filePath, content] of Object.entries(files)) {
            await fs.writeFile(filePath, content.trim());
        }
        console.log('模板文件写入完毕。');

        console.log(`\n🎉 成功!`);
        console.log(`Aviscribe 后端项目已在 ${path.resolve(baseDir)} 生成。`);
        console.log('\n下一步:');
        console.log(`1. cd ${baseDir}`);
        console.log(`2. (重要) 修改 src/main/resources/application.yml 中的数据库密码和文件路径。`);
        console.log(`3. (重要) 确保你已安装 FFmpeg，并在 application.yml 中配置了其路径。`);
        console.log(`4. (重要) 在 STT/LLM Service Impl 中填入你的 API Key 和调用逻辑。`);
        console.log(`5. 运行数据库 SQL (见规划文档) 创建数据库和表。`);
        console.log(`6. 运行 'mvn spring-boot:run' 启动项目。`);

    } catch (err) {
        console.error('生成失败:', err);
    }
}

generateProject();