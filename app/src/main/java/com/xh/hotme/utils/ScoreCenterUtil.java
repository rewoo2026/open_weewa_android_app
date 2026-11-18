//
// Copyright (c) 2017, ledong.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice, this
// list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation
// and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//


package com.xh.hotme.utils;


import android.content.Context;

import androidx.annotation.Keep;

import com.xh.hotme.R;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.camera.CameraManager;
import com.xh.hotme.record.bean.DotStates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * 颜色操作工具类
 */
@Keep
public class ScoreCenterUtil {

    public static boolean isGoalType(int type) {
        if (type == Constants.STAT_GOAL_0 || type == Constants.STAT_GOAL_1 ||
                type == Constants.STAT_GOAL_2 || type == Constants.STAT_GOAL_3 ||
                type == Constants.STAT_GOAL_4 ) {
            return true;
        }
        return false;
    }

    public static String getNameById(int id) {
        String name = "";
        switch (id) {
            case Constants.STAT_GOAL_0:
                return "进球";
            case Constants.STAT_SHOOT_0:
                return "射正"; // 射门 - 射正
            case Constants.STAT_DEFENSE_0:
                return "扑救";
            case Constants.STAT_PENALTY_0:
                return "普通防守";
            case Constants.STAT_GOAL_1:
                return "任意球";
            case Constants.STAT_CORNER_BALL:
                return "角球";
            case Constants.STAT_ATTACK_0:
                return "过人";
            case Constants.STAT_FOUL_0:
                return "普通犯规";
            case Constants.STAT_GOAL_2:
                return "乌龙";
            case Constants.STAT_GOAL_3:
                return "头球";
            case Constants.STAT_GOAL_4:
                return "点球";
            case Constants.STAT_GOAL_5:
                return "点球射失";
            case Constants.STAT_SHOOT_1:
                return "门框";
            case Constants.STAT_SHOOT_2:
                return "射偏";
            case Constants.STAT_FOUL_1:
                return "红牌";
            case Constants.STAT_FOUL_2:
                return "黄牌";
            case Constants.STAT_FOUL_3:
                return "越位";
            case Constants.STAT_EXCHANGE:
                return "换人";
            case Constants.STAT_CONTROL:
                return "控球";
            case Constants.STAT_PASS_0:
                return "传球";
            case Constants.STAT_PASS_1:
                return "直塞";
            case Constants.STAT_PASS_2:
                return "传中";
            case Constants.STAT_PASS_3:
                return "传球失败";
            case Constants.STAT_ATTACK_1:
                return "尝试过人";
            case Constants.STAT_ATTACK_2:
                return "危险进攻";
            case Constants.STAT_ATTACK_3:
                return "普通进攻";
            case Constants.STAT_DEFENSE_1:
                return "抢断";
            case Constants.STAT_DEFENSE_2:
                return "拦截";
            case Constants.STAT_DEFENSE_3:
                return "解围";
            case Constants.STAT_DEFENSE_4:
                return "封堵";
        }

        return name;
    }

    public static int getIdByName(String name) {
        if (name.equalsIgnoreCase("进球")) {
            return Constants.STAT_GOAL_0;
        } else if (name.equalsIgnoreCase("射正")) {
            return Constants.STAT_SHOOT_0;
        } else if (name.equalsIgnoreCase("扑救")) {
            return Constants.STAT_DEFENSE_0;
        } else if (name.equalsIgnoreCase("普通防守")) {
            return Constants.STAT_PENALTY_0;
        } else if (name.equalsIgnoreCase("任意球")) {
            return Constants.STAT_GOAL_1;
        } else if (name.equalsIgnoreCase("角球")) {
            return Constants.STAT_CORNER_BALL;
        } else if (name.equalsIgnoreCase("过人")) {
            return Constants.STAT_ATTACK_0;
        } else if (name.equalsIgnoreCase("普通犯规")) {
            return Constants.STAT_FOUL_0;
        } else if (name.equalsIgnoreCase("乌龙")) {
            return Constants.STAT_GOAL_2;
        } else if (name.equalsIgnoreCase("头球")) {
            return Constants.STAT_GOAL_3;
        } else if (name.equalsIgnoreCase("点球")) {
            return Constants.STAT_GOAL_4;
        } else if (name.equalsIgnoreCase("点球射失")) {
            return Constants.STAT_GOAL_5;
        } else if (name.equalsIgnoreCase("门框")) {
            return Constants.STAT_SHOOT_1;
        } else if (name.equalsIgnoreCase("射偏")) {
            return Constants.STAT_SHOOT_2;
        } else if (name.equalsIgnoreCase("红牌")) {
            return Constants.STAT_FOUL_1;
        } else if (name.equalsIgnoreCase("黄牌")) {
            return Constants.STAT_FOUL_2;
        } else if (name.equalsIgnoreCase("越位")) {
            return Constants.STAT_FOUL_3;
        } else if (name.equalsIgnoreCase("换人")) {
            return Constants.STAT_EXCHANGE;
        } else if (name.equalsIgnoreCase("控球")) {
            return Constants.STAT_CONTROL;
        } else if (name.equalsIgnoreCase("传球")) {
            return Constants.STAT_PASS_0;
        } else if (name.equalsIgnoreCase("直塞")) {
            return Constants.STAT_PASS_1;
        } else if (name.equalsIgnoreCase("传中")) {
            return Constants.STAT_PASS_2;
        } else if (name.equalsIgnoreCase("传球失败")) {
            return Constants.STAT_PASS_3;
        } else if (name.equalsIgnoreCase("尝试过人")) {
            return Constants.STAT_ATTACK_1;
        } else if (name.equalsIgnoreCase("危险进攻")) {
            return Constants.STAT_ATTACK_2;
        } else if (name.equalsIgnoreCase("普通进攻")) {
            return Constants.STAT_ATTACK_3;
        } else if (name.equalsIgnoreCase("抢断")) {
            return Constants.STAT_DEFENSE_1;
        } else if (name.equalsIgnoreCase("拦截")) {
            return Constants.STAT_DEFENSE_2;
        } else if (name.equalsIgnoreCase("解围")) {
            return Constants.STAT_DEFENSE_3;
        } else if (name.equalsIgnoreCase("封堵")) {
            return Constants.STAT_DEFENSE_4;
        }

        return -1;
    }

    public static String loadCustomDotRecord() {
        String deviceId = CameraManager.getDeviceId();
        return AppFileUtil.loadStringFromFile(deviceId + "_custom_dot");
    }

    public static void saveCustomDotRecord(List<DotStates> dotStateList) {
        String deviceId = CameraManager.getDeviceId();
        AppFileUtil.saveString(GsonUtils.GsonString(dotStateList), deviceId + "_custom_dot");
    }

    public static void removeCustomDotRecord() {
        String deviceId = CameraManager.getDeviceId();
        AppFileUtil.deleteFile(deviceId + "_custom_dot");
    }

    public static int getMarkIndex(Context context, String markName) {

        String[] tagTypeList = context.getResources().getStringArray(R.array.game_tag_list);
        for (int i = 0; i < tagTypeList.length; i++) {
            if (tagTypeList[i].equalsIgnoreCase(markName)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 根据比赛阶段，判断当前阶段是否需要暂停计时
     * @param tagIndex
     * @return
     */
    public static boolean isMatchPause(int tagIndex){
        if(tagIndex==0 || tagIndex==2 || tagIndex ==6){
            return true;
        }
        return false;
    }

    /*
     * 解析mstat.log文件
     * 最新的格式：section,type,host flag,frame id,minutes,seconds,name|id|mark name|mark id
     */
    public static ArrayList<GameStatusListBean> getStateListFromFile(String filePath, int appendNumber) {
        ArrayList<GameStatusListBean> statusList = new ArrayList<>();
        try {
            File statFile = new File(filePath);
            if(statFile == null || !statFile.exists()){
                return statusList;
            }
            FileInputStream fileInputStream = new FileInputStream(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader in = new BufferedReader(inputStreamReader);
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    //最新的格式：section,type,host flag,frame id,minutes,seconds,name|id|mark name|mark id
                    String[] split = line.split(",");
                    int appendIndex = Integer.parseInt(split[0].trim());
                    if (appendIndex == appendNumber) {
                        if (split.length > 4) {
                            int type = Integer.parseInt(split[1].trim());
                            GameStatusListBean gameStatusListBean = new GameStatusListBean(type);
                            gameStatusListBean.setHost(Integer.parseInt(split[2].trim()));
                            int minute = Integer.parseInt(split[4].trim());
                            int second = Integer.parseInt(split[5].trim());
                            gameStatusListBean.setTime(60 * minute + second);

                            String extendInfoStr = split[6].trim();
                            String[] extendArray = extendInfoStr.split("\\|");
                            if(extendArray.length >= 1){
                                gameStatusListBean.setName(extendArray[0].trim());
                            }
                            if(extendArray.length >= 2){
                                gameStatusListBean.setId(extendArray[1].trim());
                            }
                            if(extendArray.length >= 3){
                                gameStatusListBean.setMark_name(extendArray[2].trim());
                            }
                            if(extendArray.length >= 4){
                                gameStatusListBean.setMark_id(extendArray[3].trim());
                            }

                            statusList.add(gameStatusListBean);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return statusList;
    }

}
