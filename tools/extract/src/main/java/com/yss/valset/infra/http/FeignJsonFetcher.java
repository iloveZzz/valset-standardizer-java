package com.yss.valset.infra.http;

import feign.Feign;
import feign.RequestLine;
import feign.Target;
import feign.codec.StringDecoder;
import org.springframework.stereotype.Component;

/**
 * 基于 Feign 的动态 JSON 获取器。
 */
@Component
public class FeignJsonFetcher {

    /**
     * 通过动态 URL 获取原始 JSON 字符串。
     */
    public String get(String url) {
        try {
            RemoteJsonApi remoteJsonApi = Feign.builder()
                    .decoder(new StringDecoder())
                    .target(new DynamicUrlTarget<>(RemoteJsonApi.class, url));
            return remoteJsonApi.get();
        } catch (Exception e) {
            throw new IllegalStateException("Feign 获取远程 JSON 失败: " + url, e);
        }
    }

    /**
     * 远程 JSON 拉取接口。
     */
    private interface RemoteJsonApi {
        @RequestLine("GET")
        String get();
    }

    /**
     * 动态 URL Target。
     */
    private static final class DynamicUrlTarget<T> implements Target<T> {

        private final Class<T> type;
        private final String url;

        private DynamicUrlTarget(Class<T> type, String url) {
            this.type = type;
            this.url = url;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String name() {
            return type.getSimpleName();
        }

        @Override
        public String url() {
            return url;
        }

        @Override
        public feign.Request apply(feign.RequestTemplate template) {
            template.target(url);
            return template.request();
        }
    }
}
