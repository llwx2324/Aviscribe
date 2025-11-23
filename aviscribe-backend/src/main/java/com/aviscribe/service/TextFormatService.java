package com.aviscribe.service;

import com.aviscribe.entity.Task;

public interface TextFormatService { 
    String format(Task task, String rawText); 
}