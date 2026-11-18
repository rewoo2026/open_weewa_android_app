package com.xh.hotme.http;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.record.bean.CameraRecordInfoBean;
import com.xh.hotme.record.bean.RecordInfoBean;
import com.xh.hotme.utils.AppFileUtil;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.GsonUtils;

import java.util.ArrayList;
import java.util.List;

public class CompetitionLiveManager {

    private final static String cache_file = "competition_name2";
    private final static int MAX_NUMBER = 5;


    public static void addCompetition(CameraRecordInfoBean competitionName) {
        List<CameraRecordInfoBean> nameList = getCompetitionList();
        if(nameList == null) {
            nameList = new ArrayList<>();
        }
        if(nameList == null || nameList.isEmpty()) {
            nameList.add(competitionName);
            saveCompetitionName(GsonUtils.GsonString(nameList));
            return;
        }
        boolean isExit = false;
        RecordInfoBean history = null;
        for(int i = 0; i < nameList.size(); i++) {
            if(nameList.get(i).getName().equalsIgnoreCase(competitionName.getName())) {
                isExit = true;
                history = nameList.get(i);
                break;
            }
        }
        if(isExit) {
            nameList.remove(history);
            nameList.add(0, competitionName);
        } else {
            nameList.add(0, competitionName);
            if(nameList.size() > MAX_NUMBER) {
                nameList.remove(MAX_NUMBER);
            }
        }

        saveCompetitionName(GsonUtils.GsonString(nameList));
    }


    private static void saveCompetitionName(String content) {
        if(TextUtils.isEmpty(content)) {
            AppTrace.w("save competition name is null");
            return;
        }
        AppFileUtil.saveJson(content, cache_file);
    }

    public static List<CameraRecordInfoBean> getCompetitionList() {
        List<CameraRecordInfoBean> nameList = null;

        String content = AppFileUtil.loadStringFromFile(cache_file);
        if(!TextUtils.isEmpty(content)) {
            try {
                nameList = new Gson().fromJson(content, new TypeToken<List<CameraRecordInfoBean>>() {
                }.getType());
            } catch(JsonSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            AppTrace.d("load local user info is null");
            nameList = new ArrayList<>();
        }

        return nameList;
    }


    public static boolean isAppend(String content, List<CameraRecordInfoBean> nameList, String bluetoothId) {
        if(nameList == null || nameList.isEmpty()) {
            return false;
        }
        CameraRecordInfoBean lastRecordInfo = nameList.get(0);
        if(lastRecordInfo.getName().equalsIgnoreCase(content) && !TextUtils.isEmpty(bluetoothId) && !TextUtils.isEmpty(lastRecordInfo.deviceId) && lastRecordInfo.deviceId.equalsIgnoreCase(bluetoothId)) {
            long interval = System.currentTimeMillis() - lastRecordInfo.getTime();
            return interval < 2 * 60 * 60 * 1000;
        }
        return false;
    }

}
