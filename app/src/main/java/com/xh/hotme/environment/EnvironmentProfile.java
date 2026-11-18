package com.xh.hotme.environment;

import java.net.URI;
import java.net.URISyntaxException;

/** Address validation shared by the App and camera configuration form. */
public final class EnvironmentProfile {
    public static final String PRODUCTION = "production";
    public static final String TEST = "test";
    public static final String CUSTOM = "custom";
    public final String mode;
    public final String rabbitmq;
    public final String api;

    public EnvironmentProfile(String mode, String rabbitmq, String api) {
        if (!PRODUCTION.equals(mode) && !TEST.equals(mode) && !CUSTOM.equals(mode)) {
            throw new IllegalArgumentException("请选择运行环境");
        }
        this.mode = mode;
        this.rabbitmq = CUSTOM.equals(mode) ? normalizeRabbitmq(rabbitmq) : rabbitmq;
        this.api = CUSTOM.equals(mode) ? normalizeApi(api) : api;
    }

    private static URI parse(String value, String message) {
        try {
            if (value == null || value.length() > 2048) throw new IllegalArgumentException(message);
            URI uri = new URI(value.trim());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException(message);
            }
            return uri;
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException(message);
        }
    }

    public static String normalizeRabbitmq(String value) {
        String message = "RabbitMQ 地址应为 amqp://主机:端口/rewoo，账号沿用现有配置";
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
        String address = value.trim();
        if (!address.contains("://")) address = "amqp://" + address;
        URI uri = parse(address, message);
        if (!"amqp".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException(message);
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) path = "/rewoo";
        if (path.substring(1).contains("/")) throw new IllegalArgumentException(message);
        String host = uri.getHost();
        return "amqp://" + host + ":" + (uri.getPort() < 0 ? 5672 : uri.getPort()) + path;
    }

    public static String normalizeApi(String value) {
        String message = "hotme-api 地址应为 http:// 或 https:// 开头的完整地址";
        URI uri = parse(value, message);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(message);
        }
        String address = uri.toString().replaceAll("/+$", "");
        if (!uri.getPath().replaceAll("/+$", "").endsWith("/hotme-api")) address += "/hotme-api";
        return address;
    }

    public String apiOrigin() {
        return api.substring(0, api.length() - "/hotme-api".length());
    }
}
