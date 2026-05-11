package com.yss.valset.common.exception;

public class InvalidWorkbookException extends BizException {
    public InvalidWorkbookException(String message) {
        super("INVALID_WORKBOOK", message);
    }
}
