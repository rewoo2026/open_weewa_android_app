package com.xh.hotme.environment;

import android.content.Context;
import android.content.SharedPreferences;
import com.xh.hotme.BuildConfig;
import com.xh.hotme.utils.Constants;

public final class RuntimeEnvironment {
    public static final String CAMERA_KEY = "idle.server:environment";
    private static SharedPreferences preferences;
    private static volatile EnvironmentProfile current;
    private static final java.util.concurrent.atomic.AtomicLong revision = new java.util.concurrent.atomic.AtomicLong();
    public static long revision() { return revision.get(); }

    private RuntimeEnvironment() {}

    public static boolean canEdit() {
        return !("user".equals(BuildConfig.FLAVOR_role) && "prod".equals(BuildConfig.FLAVOR_server));
    }

    private static String defaultMode() {
        if (!canEdit()) return EnvironmentProfile.PRODUCTION;
        return !Constants.isGztCrew() && Constants.TEST_SERVER ? EnvironmentProfile.TEST : EnvironmentProfile.PRODUCTION;
    }

    public static void init(Context context) {
        preferences = context.getSharedPreferences("runtime_environment", Context.MODE_PRIVATE);
        if (!canEdit()) {
            current = resolve(EnvironmentProfile.PRODUCTION, "", "");
            preferences.edit().putString("mode", current.mode).putString("rabbitmq", current.rabbitmq)
                    .putString("api", current.api).apply();
            return;
        }
        String mode = preferences.getString("mode", defaultMode());
        try {
            current = resolve(mode, preferences.getString("rabbitmq", ""), preferences.getString("api", ""));
        } catch (IllegalArgumentException e) {
            current = resolve(EnvironmentProfile.PRODUCTION, "", "");
        }
    }

    public static EnvironmentProfile resolve(String mode, String rabbitmq, String api) {
        if (EnvironmentProfile.CUSTOM.equals(mode)) return new EnvironmentProfile(mode, rabbitmq, api);
        boolean test = EnvironmentProfile.TEST.equals(mode);
        // Preserve the existing production endpoints of the GZT build.
        boolean gzt = !test && Constants.isGztCrew();
        return new EnvironmentProfile(mode,
                test ? "amqp://mq.test.rewoo360.com:5672/rewoo" : gzt ? "amqp://223.109.239.30:61003/rewoo" : "amqp://mq.rewoo360.com:5672/rewoo",
                test ? "https://weewa.test.rewoo360.com/hotme-api" : gzt ? "http://223.109.239.30:61004/hotme-api" : "https://weewa.rewoo360.com/hotme-api");
    }

    public static EnvironmentProfile resolveCamera(String mode, String rabbitmq, String api) {
        if (EnvironmentProfile.PRODUCTION.equals(mode)) {
            return new EnvironmentProfile(mode, "amqp://mq.rewoo360.com:5672/rewoo", "https://weewa.rewoo360.com/hotme-api");
        }
        return resolve(mode, rabbitmq, api);
    }

    public static EnvironmentProfile customDefaults() {
        return new EnvironmentProfile(EnvironmentProfile.CUSTOM,
                "amqp://223.109.239.30:61003/rewoo", "http://223.109.239.30:61004/hotme-api");
    }

    public static String cacheScope() {
        EnvironmentProfile profile = get();
        return com.xh.hotme.utils.MD5.md5(profile.api + "|" + profile.rabbitmq);
    }

    public static EnvironmentProfile get() {
        if (!canEdit()) return resolve(EnvironmentProfile.PRODUCTION, "", "");
        return current != null ? current : resolve(defaultMode(), "", "");
    }

    public static boolean save(EnvironmentProfile profile) {
        if (!canEdit()) return false;
        if (!preferences.edit().putString("mode", profile.mode).putString("rabbitmq", profile.rabbitmq)
                .putString("api", profile.api).commit()) return false;
        current = profile;
        revision.incrementAndGet();
        return true;
    }

    public static String label(String mode) {
        return EnvironmentProfile.TEST.equals(mode) ? "测试环境" : EnvironmentProfile.CUSTOM.equals(mode) ? "自定义环境" : "正式环境";
    }
}
