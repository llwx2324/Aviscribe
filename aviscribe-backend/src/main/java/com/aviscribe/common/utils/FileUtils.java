package com.aviscribe.common.utils;

import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileUtils {

    // 实施文件保存、删除、获取安全路径的逻辑
    /**
     * 组合安全的上传路径，防止路径穿越，并自动创建父目录。
     */
    public Path getSafeUploadPath(String baseDir, String filename) {
        if (baseDir == null || baseDir.isEmpty()) {
            throw new IllegalArgumentException("baseDir 不能为空");
        }
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename 不能为空");
        }
        String safeName = FileUtil.getName(filename);
        Path base = Paths.get(baseDir).toAbsolutePath().normalize();
        Path target = base.resolve(safeName).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法文件名，存在路径穿越风险: " + filename);
        }
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败: " + target.getParent(), e);
        }
        return target;
    }

    /**
     * 删除单个文件，删除失败不抛异常，返回是否成功。
     */
    public boolean deleteFileQuietly(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        try {
            return Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            return false;
        }
    }
}