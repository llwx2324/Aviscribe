package com.aviscribe.service;

public interface DownloadService {
    /**
     * 根据给定 URL 下载远程视频到本地，返回本地文件绝对路径。
     */
    String download(String url) throws Exception;
}

