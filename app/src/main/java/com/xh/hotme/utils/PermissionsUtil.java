package com.xh.hotme.utils;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Keep
public class PermissionsUtil {

    public static final int REQUEST_READ_PHONE_STATE = 0x00000001;
    public static final int REQUEST_PERMISSION_STORAGE = 0x00000002;
    public static final int REQUEST_PERMISSION = 0x00000003;

    public static final int REQUEST_PERMISSION_BLUE = 0x00000004;
    public static final int REQUEST_PERMISSION_LOCATION = 0x00000005;

    public static final int REQUEST_LOCATION_SERVICE = 0x00000006;

    public static final int REQUEST_CAMERA = 6;
    public static final int REQUEST_EXTERNAL_READ = 7;
    public static final int REQUEST_EXTERNAL_WRITE = 8;

    public static final int REQUEST_PERMISSION_NEARBY_WIFI_DEVICES = 8;


    public static final String[] PERMISSIONS_CAMERA = {
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final String[] PERMISSIONS_EXTERNAL_WRITE = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static final String[] PERMISSIONS_EXTERNAL_READ = {
        Manifest.permission.READ_EXTERNAL_STORAGE
    };


    private static Handler HANDLER = null;

    //需要申请的权限
    private static final String[] permissions = new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };
    private static final String[] ble_permissions = new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    };
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] ble_permissons_s = new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
    };

    public static final String[] location_permissions = new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // 记录正在延迟检查的权限
    private static final Map<String, DelayCheckRunnable> _delayMap = new HashMap<>();

    /**
     * class to check permission
     */
    private static class DelayCheckRunnable implements Runnable {
        private final String _perm;
        private final WeakReference<Activity> _act;
        private final int _delay;
        private final boolean _repeat;

        public DelayCheckRunnable(Activity act, int delay, boolean repeat, String p) {
            _perm = p;
            _repeat = repeat;
            _act = new WeakReference<>(act);
            _delay = delay;

            // add to map
            _delayMap.put(p, this);
        }

        @Override
        public void run() {
            Activity act = _act.get();
            if(act != null && !act.isDestroyed() && !act.isFinishing()) {
                int checkSelfPermission = ContextCompat.checkSelfPermission(act, _perm);
                if(checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                    // if repeat, re-schedule, otherwise remove from map
                    if(_repeat) {
                        HANDLER.postDelayed(this, _delay);
                    } else {
                        _delayMap.remove(_perm);
                    }

                    // request
                    ActivityCompat.requestPermissions(act, new String[] { _perm }, 44444);
                } else {
                    // if already granted, remove from map
                    _delayMap.remove(_perm);
                }
            } else {
                // if activity is gone, remove from map
                _delayMap.remove(_perm);
            }
        }
    }

    /**
     * 延迟检查一个权限
     *
     * @param act        activity
     * @param delay      延迟时间
     * @param repeat     是否重复检查直到获得授权, 如果为true, 则会按照delay间隔不停检查
     * @param permission 权限名称
     */
    public static void delayCheckPermission(Activity act, int delay, boolean repeat, String permission) {
        // lazy create handler
        if(HANDLER == null) {
            HANDLER = new Handler(Looper.getMainLooper());
        }

        // delay must > 0
        if(delay <= 0) {
            return;
        }

        // if already in checking, remove it
        if(_delayMap.containsKey(permission)) {
            HANDLER.removeCallbacks(_delayMap.remove(permission));
        }

        // delay
        HANDLER.postDelayed(new DelayCheckRunnable(act, delay, repeat, permission), delay);
    }

    //检测权限
    public static String[] checkPermission(Context context) {
        List<String> data = new ArrayList<>();//存储未申请的权限
        for(String permission : permissions) {
            int checkSelfPermission = ContextCompat.checkSelfPermission(context, permission);
            if(checkSelfPermission != PackageManager.PERMISSION_GRANTED) {//未申请
                data.add(permission);
            }
        }
        return data.toArray(new String[data.size()]);
    }

    public static String[] checkPermissions(Context context, String[] permissions) {
        List<String> data = new ArrayList<>();//存储未申请的权限
        for(String permission : permissions) {
            int checkSelfPermission = ContextCompat.checkSelfPermission(context, permission);
            if(checkSelfPermission != PackageManager.PERMISSION_GRANTED) {//未申请
                data.add(permission);
            }
        }
        return data.toArray(new String[data.size()]);
    }


    public static boolean checkReadStoragePermission(Activity activity) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int readStoragePermissionState =
            ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE);

        boolean readStoragePermissionGranted = readStoragePermissionState == PackageManager.PERMISSION_GRANTED;

        if(!readStoragePermissionGranted) {
            ActivityCompat.requestPermissions(activity,
                PERMISSIONS_EXTERNAL_READ,
                REQUEST_EXTERNAL_READ);
        }
        return readStoragePermissionGranted;
    }

    public static boolean checkWriteStoragePermission(Activity context) {

        int writeStoragePermissionState =
            ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE);

        boolean writeStoragePermissionGranted = writeStoragePermissionState == PackageManager.PERMISSION_GRANTED;

        if(!writeStoragePermissionGranted) {
            ActivityCompat.requestPermissions(context, PERMISSIONS_EXTERNAL_WRITE,
                REQUEST_EXTERNAL_WRITE);
        }
        return writeStoragePermissionGranted;
    }

    public static boolean checkStoragePermission(Activity activity) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int readStoragePermissionState =
            ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE);

        boolean readStoragePermissionGranted = readStoragePermissionState == PackageManager.PERMISSION_GRANTED;

        if(!readStoragePermissionGranted) {
            ActivityCompat.requestPermissions(activity,
                PERMISSIONS_EXTERNAL_READ,
                REQUEST_EXTERNAL_READ);
        }
        return readStoragePermissionGranted;
    }

    public static boolean checkWriteStoragePermission(Fragment fragment) {

        int writeStoragePermissionState =
            ContextCompat.checkSelfPermission(fragment.getContext(), WRITE_EXTERNAL_STORAGE);

        boolean writeStoragePermissionGranted = writeStoragePermissionState == PackageManager.PERMISSION_GRANTED;

        if(!writeStoragePermissionGranted) {
            fragment.requestPermissions(PERMISSIONS_EXTERNAL_WRITE,
                REQUEST_EXTERNAL_WRITE);
        }
        return writeStoragePermissionGranted;
    }

    public static boolean checkCameraPermission(Fragment fragment) {
        int cameraPermissionState = ContextCompat.checkSelfPermission(fragment.getContext(), CAMERA);

        boolean cameraPermissionGranted = cameraPermissionState == PackageManager.PERMISSION_GRANTED;

        if(!cameraPermissionGranted) {
            fragment.requestPermissions(PERMISSIONS_CAMERA,
                REQUEST_CAMERA);
        }
        return cameraPermissionGranted;
    }

    public static boolean hasPermission(Context ctx, String perm) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int state = ContextCompat.checkSelfPermission(ctx, perm);
        return state == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Android 31 已经移除了这两个权限的声明要求 （Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,）
     *
     * @param context
     * @return
     */
    // 蓝牙权限
    public static boolean requestBlePermission(Activity context) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = checkPermissions(context, ble_permissons_s);
            if(permissions.length > 0) {
                ActivityCompat.requestPermissions(context, permissions, REQUEST_PERMISSION_BLUE);
                return true;
            }

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = checkPermissions(context, ble_permissions);
            if(permissions.length > 0) {
                ActivityCompat.requestPermissions(context, permissions, REQUEST_PERMISSION_BLUE);
                return true;
            }
        }

        return false;
    }

    public static void requestPermission(Activity context, int requestCode, String[] permission) {

        ActivityCompat.requestPermissions(context, permission, requestCode);
    }


    public static String[] getBlePermissions() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ble_permissons_s;
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ble_permissions;
        } else {
            return location_permissions;
        }
    }

    // 蓝牙权限
    public static boolean hasBlePermission(Activity context) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = checkPermissions(context, ble_permissons_s);
            return permissions.length <= 0;

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = checkPermissions(context, ble_permissions);
            return permissions.length <= 0;
        }

        return true;
    }

    /**
     * 手机是否开启位置服务，如果没有开启那么所有app将不能使用定位功能
     */
    public static boolean isLocServiceEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    public static void openLocationService(Activity activity) {
        Intent intent = new Intent();
        //定位服务页面
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch(ActivityNotFoundException ex) {
            //如果页面无法打开，进入设置页面
            intent.setAction(Settings.ACTION_SETTINGS);
            try {
                activity.startActivity(intent);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openLocationServiceForResult(Activity activity, int requestCode) {
        Intent intent = new Intent();
        //定位服务页面
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch(ActivityNotFoundException ex) {
            //如果页面无法打开，进入设置页面
            intent.setAction(Settings.ACTION_SETTINGS);
            try {
                activity.startActivity(intent);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
