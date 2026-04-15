package com.yss.subjectmatch.common.exception;

public class MatchEngineException extends BizException {
    public MatchEngineException(String message) {
        super("MATCH_ENGINE_ERROR", message);
    }
}
