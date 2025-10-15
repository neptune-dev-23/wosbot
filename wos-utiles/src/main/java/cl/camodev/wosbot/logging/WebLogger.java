package cl.camodev.wosbot.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * Logger that mirrors the profile-specific logging strategy but targets a dedicated web log file.
 * Intended for HTTP and WebSocket interaction traces produced by the web module.
 */
public class WebLogger {
    private static final Logger mainLogger = LoggerFactory.getLogger(WebLogger.class);
    private static final String LOG_DIRECTORY = "log";
    private static final String LOG_FILE_NAME = "web.log";
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_BACKUP_FILES = 5;
    private static final SimpleDateFormat timestampFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat dateSuffixFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private static final ReentrantLock writerLock = new ReentrantLock();
    private static volatile PrintWriter writer;

    private final Logger logger;
    private final String className;

    public WebLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.className = clazz.getSimpleName();
        ensureLogDirectory();
    }

    public void info(String message) {
        logger.info(message);
        logToFile("INFO", message, null);
    }

    public void debug(String message) {
        logger.debug(message);
        logToFile("DEBUG", message, null);
    }

    public void warn(String message) {
        logger.warn(message);
        logToFile("WARN", message, null);
    }

    public void error(String message) {
        logger.error(message);
        logToFile("ERROR", message, null);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
        logToFile("ERROR", message, throwable);
    }

    private void logToFile(String level, String message, Throwable throwable) {
        writerLock.lock();
        try {
            ensureWriter();
            checkAndRotateIfNeeded();
            if (writer == null) {
                return;
            }
            writer.println(formatMessage(level, message));
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
            writer.flush();
        } finally {
            writerLock.unlock();
        }
    }

    private String formatMessage(String level, String message) {
        String timestamp = timestampFormat.format(Date.from(Instant.now()));
        return timestamp + " [" + level + "] " + className + " - " + message;
    }

    private void ensureLogDirectory() {
        try {
            Files.createDirectories(Paths.get(LOG_DIRECTORY));
        } catch (IOException ex) {
            mainLogger.error("Failed to create log directory '{}'", LOG_DIRECTORY, ex);
        }
    }

    private void ensureWriter() {
        if (writer != null) {
            return;
        }
        try {
            File file = new File(LOG_DIRECTORY, LOG_FILE_NAME);
            if (!file.exists() && !file.createNewFile()) {
                mainLogger.warn("Could not create web log file {}", file.getAbsolutePath());
            }
            FileWriter fileWriter = new FileWriter(file, true);
            writer = new PrintWriter(fileWriter, true);
            writer.println("==========================================================");
            writer.println("Web Log Started: " + timestampFormat.format(new Date()));
            writer.println("==========================================================");
        } catch (IOException ex) {
            mainLogger.error("Failed to initialize web log writer", ex);
            writer = null;
        }
    }

    private void checkAndRotateIfNeeded() {
        File logFile = new File(LOG_DIRECTORY, LOG_FILE_NAME);
        if (!logFile.exists() || logFile.length() <= MAX_LOG_FILE_SIZE) {
            return;
        }

        // Close current writer before rotation
        if (writer != null) {
            writer.close();
            writer = null;
        }

        try {
            rotateLogFile(logFile);
        } catch (IOException ex) {
            mainLogger.error("Failed to rotate web log file", ex);
        }
    }

    private void rotateLogFile(File logFile) throws IOException {
        String baseName = LOG_FILE_NAME.substring(0, LOG_FILE_NAME.lastIndexOf('.'));
        String dateSuffix = dateSuffixFormat.format(new Date());

        int index = findAvailableIndex(baseName, dateSuffix);
        File backup = new File(logFile.getParent(),
                baseName + "." + dateSuffix + "." + index + ".gz");

        try (FileInputStream fis = new FileInputStream(logFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(backup);
             GZIPOutputStream gzos = new GZIPOutputStream(new BufferedOutputStream(fos))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }

        if (!logFile.delete() && !logFile.createNewFile()) {
            mainLogger.warn("Failed to recreate web log file {}", logFile.getAbsolutePath());
        }
    }

    private int findAvailableIndex(String baseName, String dateSuffix) {
        File dir = new File(LOG_DIRECTORY);
        for (int index = 0; index < MAX_BACKUP_FILES; index++) {
            File candidate = new File(dir, baseName + "." + dateSuffix + "." + index + ".gz");
            if (!candidate.exists()) {
                return index;
            }
        }

        File[] backups = dir.listFiles((d, name) ->
                name.startsWith(baseName + ".") && name.endsWith(".gz"));
        if (backups != null && backups.length > 0) {
            File oldest = backups[0];
            for (File file : backups) {
                if (file.getName().compareTo(oldest.getName()) < 0) {
                    oldest = file;
                }
            }
            if (!oldest.delete()) {
                mainLogger.warn("Failed to delete oldest web log backup {}", oldest.getAbsolutePath());
            }
        }

        return 0;
    }
}
