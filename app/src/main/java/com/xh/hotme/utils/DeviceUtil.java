package com.xh.hotme.utils;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;

import androidx.annotation.Keep;
import androidx.core.app.ActivityCompat;

import com.xh.hotme.HotmeApplication;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

/**
 * Create by zhaozhihui on 2018/10/12
 **/
@Keep
public class DeviceUtil {
    private static final String TAG = DeviceUtil.class.getSimpleName();

    /**
     * phnoe model
     */
    public static String getPhoneModel() {
        return Build.MODEL;
    }

    /**
     * phone brand
     */
    public static String getPhoneBrand() {
        if(Build.BOARD == null) {
            return "unknown";
        }
        return Build.BRAND;
    }

    public static String getVersionCode(Context context) {
        return getVersionCode(context.getPackageName(), context) + "";
    }

    /**
     * 当前应用versionCode
     */
    public static int getVersionCode(String packageName, Context context) {
        int versionCode = 0;
        PackageManager mPackageManger = context.getPackageManager();
        try {
            versionCode = mPackageManger.getPackageInfo(packageName, 0).versionCode;
        } catch(NameNotFoundException e) {
            // TODO Auto-generated catch block
            versionCode = 0;
        }

        return versionCode;
    }

    /**
     * 获取手机序列号
     */
    public static String getSerialNumber() {
        String serial = null;
        try {
            Class<?> c = Class
                .forName("android.android.android.android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch(Exception e) {
            serial = null;
            // e.printStackTrace();
        }
        return serial;
    }

    // like 21
    public static int SdkLevel() {
        return Build.VERSION.SDK_INT;
    }

    // like 5.1.1
    public static String SdkRelease() {
        return Build.VERSION.RELEASE;
    }


    /**
     * 系统语言
     *
     * @return
     */
    public static String getMobileLanguage() {
        String language = HotmeApplication.getContext().getResources().getConfiguration().locale
            .getLanguage();
        return language;
    }

    //默认是ISO-2
    public static String getCountry() {
        String able = HotmeApplication.getContext().getResources().getConfiguration().locale
            .getCountry();
        return able;
    }

    public static String getCountryISO3() {
        String able = HotmeApplication.getContext().getResources().getConfiguration().locale
            .getISO3Country();
        return able;
    }

    public static String getSimCountryIso() {
        TelephonyManager telManager = (TelephonyManager) HotmeApplication.getContext()
            .getSystemService(Context.TELEPHONY_SERVICE);
        return telManager.getSimCountryIso();
    }

    /**
     * get phone 分辨率
     */

    public static String getPhoneDensity() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        return displayMetrics.density + "";
    }

    public static String getPhoneDensityDpi(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.densityDpi + "";
    }

    public static String getPhoneWidth() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        return width + "";
    }

    public static String getPhoneHeight() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        return "" + height;
    }

    public static int getWidth() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        return width;
    }

    public static int getHeight() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        return height;
    }

    // 获取屏幕分辨率

    public static String getPhoneResolution() {
        DisplayMetrics displayMetrics = HotmeApplication.getContext().getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        return width + "x" + height;
    }

    public static String getSimSerialNumber(Context context) {
        TelephonyManager mTelePhonyManager = (TelephonyManager) context
            .getSystemService(Context.TELEPHONY_SERVICE);
        String sn = null;
        try {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                sn = mTelePhonyManager.getSimSerialNumber();
            } else {
                sn = "";
            }
        } catch(RuntimeException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(sn == null)
            sn = "";
        return sn;
    }

    public static int getScreenChange(Context context) {

        Configuration mConfiguration = context.getResources()
            .getConfiguration(); // 获取设置的配置信息
        int ori = mConfiguration.orientation; // 获取屏幕方向

        if(ori == Configuration.ORIENTATION_LANDSCAPE) {

            // 横屏
            return 1;
        } else if(ori == Configuration.ORIENTATION_PORTRAIT) {

            // 竖屏
            return 0;
        }
        return 0;
    }

    public static String getAndroidIDMethod(Context context) {
        String androidId = Settings.Secure.getString(
            context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

    public static String getCarrier() {
        return Build.MANUFACTURER;
    }

    public static boolean isSystem(Context context) {
        if(context == null) {
            return false;
        }

        return isSystemApplication(context, context.getPackageName());
    }

    /**
     * whether packageName is system application
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isSystemApplication(Context context,
                                              String packageName) {
        if(context == null) {
            return false;
        }

        return isSystemApplication(context.getPackageManager(), packageName);
    }

    /**
     * whether packageName is system application
     *
     * @param packageManager
     * @param packageName
     * @return <ul>
     * <li>if packageManager is null, return false</li>
     * <li>if package name is null or is empty, return false</li>
     * <li>if package name not exit, return false</li>
     * <li>if package name exit, but not system app, return false</li>
     * <li>else return true</li>
     * </ul>
     */
    public static boolean isSystemApplication(PackageManager packageManager,
                                              String packageName) {
        if(packageManager == null || packageName == null
            || packageName.isEmpty()) {
            return false;
        }

        try {
            ApplicationInfo app = packageManager.getApplicationInfo(
                packageName, 0);
            return (app != null && (app.flags & ApplicationInfo.FLAG_SYSTEM) > 0);
        } catch(NameNotFoundException e) {
        }
        return false;
    }

    public static String getBaseIMEI(Context context) {

        String imei = "";
        try {
            TelephonyManager mTelePhonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
            if(mTelePhonyManager != null) {
                if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    imei = mTelePhonyManager.getDeviceId();
                }
            }
        } catch(Exception e) {

        }

        return imei;
    }


    public static String getUserAgent(Context context) {
        String userAgent = "";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                userAgent = WebSettings.getDefaultUserAgent(context);
            } catch(Exception e) {
                userAgent = System.getProperty("http.agent");
            }
        } else {
            userAgent = System.getProperty("http.agent");
        }
        StringBuffer sb = new StringBuffer();
        for(int i = 0, length = userAgent.length(); i < length; i++) {
            char c = userAgent.charAt(i);
            if(c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    private static String parsePacketName(String buff) {
        int begin = buff.indexOf('{') + 1;
        int end = buff.indexOf('}');
        if(begin > end)
            return null;
        String temp = buff.substring(begin + 1, end);
        String[] words = temp.split(" ");
        if(words.length != 4)
            return null;
        return ComponentName.unflattenFromString(words[2]).getPackageName();
    }


    public static String getIPAddress(Context context) {
        try {
            NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if(info != null && info.isConnected()) {
                if(info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                    try {
//                    Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                        for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en != null && en.hasMoreElements(); ) {
                            NetworkInterface intf = en.nextElement();
                            if(intf != null) {
                                for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr != null && enumIpAddr.hasMoreElements(); ) {
                                    InetAddress inetAddress = enumIpAddr.nextElement();
                                    if(inetAddress != null && !inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                        return inetAddress.getHostAddress();
                                    }
                                }
                            }
                        }
                    } catch(SocketException e) {
                        e.printStackTrace();
                    } catch(Throwable e) {
                        e.printStackTrace();
                    }


                } else if(info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                    return ipAddress;
                }
            } else {
                //当前无网络连接,请在设置中打开网络
            }
        } catch(Throwable e) {

        }
        return "0.0.0.0";
    }


    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
            ((ip >> 8) & 0xFF) + "." +
            ((ip >> 16) & 0xFF) + "." +
            (ip >> 24 & 0xFF);
    }

    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     *
     * @param context
     * @return 平板返回 True，手机返回 False
     */
    public static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
