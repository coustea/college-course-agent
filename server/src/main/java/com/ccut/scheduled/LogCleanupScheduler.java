package com.ccut.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * 日志清理定时任务
 * 每 30 天执行一次，清理 30 天前的日志文件
 */
@Component
public class LogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupScheduler.class);

    /**
     * 日志文件存储目录
     */
    @Value("${LOG_HOME:/home/couseta/develop/CCUT/backend/logs}")
    private String logHome;

    /**
     * 日志文件保留天数
     */
    private static final int LOG_RETENTION_DAYS = 30;

    /**
     * 日志文件名日期格式
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每 30 天执行一次日志清理
     * 在每天凌晨 2:00 执行
     */
    @Scheduled(cron = "0 0 2 */30 * ?")
    public void cleanupOldLogs() {
        log.info("========== 开始清理过期日志文件 ==========");

        File logDir = new File(logHome);
        if (!logDir.exists() || !logDir.isDirectory()) {
            log.warn("日志目录不存在或不是目录：{}", logHome);
            return;
        }

        File[] logFiles = logDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // 匹配 ccut-service.2024-01-15.log 格式的历史日志文件
                return name.matches("ccut-service\\.\\d{4}-\\d{2}-\\d{2}\\.log");
            }
        });

        if (logFiles == null || logFiles.length == 0) {
            log.info("没有找到日志文件");
            return;
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(LOG_RETENTION_DAYS);
        int deletedCount = 0;
        long totalDeletedSize = 0;

        for (File logFile : logFiles) {
            try {
                LocalDate fileDate = extractDateFromFileName(logFile.getName());
                if (fileDate != null && fileDate.isBefore(cutoffDate)) {
                    long fileSize = logFile.length();
                    if (logFile.delete()) {
                        deletedCount++;
                        totalDeletedSize += fileSize;
                        log.info("删除过期日志：{} ({})", logFile.getName(), formatFileSize(fileSize));
                    } else {
                        log.warn("删除日志文件失败：{}", logFile.getName());
                    }
                }
            } catch (Exception e) {
                log.error("处理日志文件失败：{}", logFile.getName(), e);
            }
        }

        log.info("========== 日志清理完成：删除 {} 个文件，释放 {} 空间 ==========",
                deletedCount, formatFileSize(totalDeletedSize));
    }

    /**
     * 从日志文件名中提取日期
     * 支持格式：ccut-service.2024-01-15.log
     */
    private LocalDate extractDateFromFileName(String fileName) {
        try {
            // 匹配带日期的日志文件名：ccut-service.yyyy-MM-dd.log
            if (fileName.matches("ccut-service\\.\\d{4}-\\d{2}-\\d{2}\\.log")) {
                int dateStartIndex = fileName.indexOf('.') + 1;
                int dateEndIndex = fileName.lastIndexOf('.');
                String dateStr = fileName.substring(dateStartIndex, dateEndIndex);
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            }
            // 当前日志文件（不带日期），不删除
            return null;
        } catch (DateTimeParseException e) {
            log.warn("无法解析日志文件日期：{}", fileName);
            return null;
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
