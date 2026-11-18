package com.xh.hotme.account;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import dalvik.system.DexClassLoader;

/**
 * Checks real minified APK callbacks using fake token data, without login/SMS requests.
 * Supply original and fixed APKs as old.apk/fixed.apk in test assets, and pass the
 * oldCallback and comma-separated fixedCallbacks class names from their R8 mappings.
 */
public class ReleaseLoginCallbackInstrumentation extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments;
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int code = Activity.RESULT_OK;
        try {
            ClassLoader oldLoader = loader("old.apk");
            Class<?> oldCallback = oldLoader.loadClass(arguments.getString("oldCallback"));
            require(!(oldCallback.getGenericSuperclass() instanceof ParameterizedType),
                    "Original APK did not reproduce missing callback signature");
            ClassLoader fixedLoader = loader("fixed.apk");
            Class<?> base = fixedLoader.loadClass("com.xh.hotme.http.OkHttpCallbackDecode");
            Method getType = base.getDeclaredMethod("getTClass");
            getType.setAccessible(true);
            Class<?> gsonClass = fixedLoader.loadClass("com.google.gson.Gson");
            Object gson = gsonClass.getDeclaredConstructor().newInstance();
            Method fromJson = gsonClass.getMethod("fromJson", String.class, Class.class);
            String[] names = arguments.getString("fixedCallbacks").split(",");
            for (String name : names) {
                Class<?> callback = fixedLoader.loadClass(name);
                require(callback.getGenericSuperclass() instanceof ParameterizedType,
                        "Fixed callback lost generic signature: " + name);
                Constructor<?> constructor = callback.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                Object instance = constructor.newInstance(new Object[constructor.getParameterCount()]);
                Class<?> type = (Class<?>) getType.invoke(instance);
                require(type.getName().equals("com.xh.hotme.bean.LoginResultBean"), "Wrong result type");
                Object data = fromJson.invoke(gson, "{\"token\":\"release-login-probe\"}", type);
                require("release-login-probe".equals(type.getMethod("getToken").invoke(data)),
                        "Login token was not decoded");
            }
            result.putString("stream", "\nOK (original APK failure reproduced; " + names.length
                    + " minified login callback checks passed)\n");
        } catch (Throwable failure) {
            result.putString("stream", "\nFAIL: " + failure + "; cause: " + failure.getCause() + "\n");
            code = Activity.RESULT_CANCELED;
        } finally {
            // Remove only the APK copies made by this test, leaving app login data intact.
            for (String asset : new String[]{"old.apk", "fixed.apk"}) {
                new File(getTargetContext().getCacheDir(), "release-login-" + asset).delete();
            }
        }
        finish(code, result);
    }

    private ClassLoader loader(String asset) throws Exception {
        File apk = new File(getTargetContext().getCacheDir(), "release-login-" + asset);
        if (apk.exists() && !apk.delete()) throw new IllegalStateException("Cannot remove old probe APK");
        try (InputStream in = getContext().getAssets().open(asset);
             FileOutputStream out = new FileOutputStream(apk)) {
            require(apk.setReadOnly(), "Dynamic APK must be read-only");
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        // Use the framework parent so classes are loaded from the supplied APK.
        return new DexClassLoader(apk.getAbsolutePath(), getTargetContext().getCodeCacheDir().getPath(),
                null, getTargetContext().getClassLoader().getParent());
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
