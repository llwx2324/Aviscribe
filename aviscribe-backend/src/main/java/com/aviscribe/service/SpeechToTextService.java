package com.aviscribe.service;

import com.aviscribe.entity.Task;

public interface SpeechToTextService { String transcribe(Task task, String audioPath); }