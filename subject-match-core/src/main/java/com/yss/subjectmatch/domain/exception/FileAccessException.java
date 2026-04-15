package com.yss.subjectmatch.domain.exception;

import com.yss.subjectmatch.common.exception.BizException;

/**
 * Exception thrown when a file cannot be opened or read during data extraction.
 * This typically occurs when the file path is invalid, the file does not exist,
 * or the application lacks necessary permissions to access the file.
 */
public class FileAccessException extends BizException {

    public FileAccessException(String filePath) {
        super("FILE_ACCESS_ERROR", "Cannot access file: " + filePath);
    }

    public FileAccessException(String filePath, Throwable cause) {
        super("FILE_ACCESS_ERROR", "Cannot access file: " + filePath);
        initCause(cause);
    }
}
