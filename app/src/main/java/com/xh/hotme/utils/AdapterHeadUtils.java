package com.xh.hotme.utils;

import com.xh.hotme.bean.CameraVideoListBean;
import com.xh.hotme.bean.HmCompetition;
import com.xh.hotme.bean.ITimeHolder;

import java.util.List;

/**
 * date   : 2023/7/11
 * desc   :
 */
public class AdapterHeadUtils {
    public static boolean shouldShowDateHeader(List<? extends ITimeHolder> items, int curPosition) {
        int prevPos = curPosition - 1;
        if(curPosition == 0) {
            return true;
        }
        if(prevPos >= 0 && prevPos < items.size()) {
            ITimeHolder cur = items.get(curPosition);
            ITimeHolder prev = items.get(prevPos);
            try {
                if(!TimeUtil.isSameDay(cur.getTimeInMillis(), prev.getTimeInMillis())) {
                    return true;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        return false;
    }

    public static String getHeadTitle(CameraVideoListBean.VideosBean cur) {
        return TimeUtil.zhHumanDay(cur.time);
    }

    public static String getHeadTitle(HmCompetition bean) {
        return TimeUtil.zhHumanDay(bean.getCompetitionTime());
    }
}
