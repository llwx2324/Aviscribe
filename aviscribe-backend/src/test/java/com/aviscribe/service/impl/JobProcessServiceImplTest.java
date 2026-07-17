package com.aviscribe.service.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aviscribe.common.enums.SourceType;
import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.common.utils.FfmpegUtils;
import com.aviscribe.entity.Task;
import com.aviscribe.service.AudioExtractService;
import com.aviscribe.service.DownloadService;
import com.aviscribe.service.SpeechToTextService;
import com.aviscribe.service.TaskService;
import com.aviscribe.service.TextFormatService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class JobProcessServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void resumesFromExistingArtifactsWithoutRepeatingPaidWork() throws Exception {
        TaskService taskService = Mockito.mock(TaskService.class);
        AudioExtractService audioExtractService = Mockito.mock(AudioExtractService.class);
        SpeechToTextService speechToTextService = Mockito.mock(SpeechToTextService.class);
        TextFormatService textFormatService = Mockito.mock(TextFormatService.class);
        DownloadService downloadService = Mockito.mock(DownloadService.class);
        FfmpegUtils ffmpegUtils = Mockito.mock(FfmpegUtils.class);

        Path video = Files.writeString(tempDir.resolve("video.mp4"), "video");
        Path audio = Files.writeString(tempDir.resolve("audio.wav"), "audio");
        Task task = new Task();
        task.setId(42L);
        task.setSourceType(SourceType.LOCAL.getValue());
        task.setTaskStatus(TaskStatus.FORMATTING.getCode());
        task.setVideoLocalPath(video.toString());
        task.setAudioLocalPath(audio.toString());
        task.setRawText("already transcribed");
        task.setFormattedText("# already formatted");
        task.setDurationSeconds(10);
        task.setTaskName("existing");
        when(taskService.getById(42L)).thenReturn(task);

        JobProcessServiceImpl service = new JobProcessServiceImpl(
                taskService, audioExtractService, speechToTextService,
                textFormatService, downloadService, ffmpegUtils);
        service.processTask(42L);

        verify(downloadService, never()).download(Mockito.anyString());
        verify(audioExtractService, never()).extractAudio(Mockito.any());
        verify(speechToTextService, never()).transcribe(Mockito.any(), Mockito.anyString());
        verify(textFormatService, never()).format(Mockito.any(), Mockito.anyString());
        verify(taskService).updateTaskStatus(42L, TaskStatus.COMPLETED);
    }
}
