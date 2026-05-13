package com.yss.valset.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;

/**
 * SSE 断连异常解析器。
 */
@Slf4j
@Component
public class SseDisconnectExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final String TEXT_EVENT_STREAM = "text/event-stream";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception exception) {
        if (!isSseRequest(request, response)) {
            return null;
        }
        if (!response.isCommitted() && !isClientDisconnect(exception)) {
            return null;
        }
        log.debug("忽略 SSE 连接异常，uri={}, committed={}, exception={}",
                request == null ? null : request.getRequestURI(),
                response != null && response.isCommitted(),
                exception == null ? null : exception.getMessage());
        return new ModelAndView();
    }

    private boolean isSseRequest(HttpServletRequest request, HttpServletResponse response) {
        if (response != null) {
            String contentType = response.getContentType();
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains(TEXT_EVENT_STREAM)) {
                return true;
            }
        }
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.toLowerCase(Locale.ROOT).contains(TEXT_EVENT_STREAM)) {
            return true;
        }
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.endsWith("/stream");
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ClosedChannelException || current instanceof IOException) {
                String message = current.getMessage();
                if (message != null) {
                    String lowerMessage = message.toLowerCase(Locale.ROOT);
                    if (lowerMessage.contains("broken pipe")
                            || lowerMessage.contains("connection reset by peer")
                            || lowerMessage.contains("stream closed")) {
                        return true;
                    }
                }
            }
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase(Locale.ROOT);
                if (lowerMessage.contains("broken pipe")
                        || lowerMessage.contains("connection reset by peer")
                        || lowerMessage.contains("stream closed")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
