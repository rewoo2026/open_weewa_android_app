package com.xh.hotme.record.bean;

public class CameraRecordInfoBean extends RecordInfoBean {
    public String deviceId;

    public CameraRecordInfoBean(String name, long rate) {
        super(name, rate);
    }

    public static CameraRecordInfoBean newInstance(RecordInfoBean recordInfoBean) {
        CameraRecordInfoBean cameraRecordInfoBean = new CameraRecordInfoBean(recordInfoBean.name, recordInfoBean.maxRate);
        cameraRecordInfoBean.name = recordInfoBean.name;
        cameraRecordInfoBean.author = recordInfoBean.author;
        cameraRecordInfoBean.city = recordInfoBean.city;
        cameraRecordInfoBean.height = recordInfoBean.height;
        cameraRecordInfoBean.width = recordInfoBean.width;
        cameraRecordInfoBean.motionType = recordInfoBean.motionType;
        cameraRecordInfoBean.placeType = recordInfoBean.placeType;
        cameraRecordInfoBean.place = recordInfoBean.place;
        cameraRecordInfoBean.time = recordInfoBean.time;

        cameraRecordInfoBean.guestGoal = recordInfoBean.guestGoal;
        cameraRecordInfoBean.guestTeam = recordInfoBean.guestTeam;

        cameraRecordInfoBean.hostGoal = recordInfoBean.hostGoal;
        cameraRecordInfoBean.hostTeam = recordInfoBean.hostTeam;
        cameraRecordInfoBean.player_num = recordInfoBean.player_num;
        cameraRecordInfoBean.avs_mbr = recordInfoBean.avs_mbr;
        cameraRecordInfoBean.live_mbr = recordInfoBean.live_mbr;
        cameraRecordInfoBean.provinceId = recordInfoBean.provinceId;
        cameraRecordInfoBean.cityId = recordInfoBean.cityId;

        return cameraRecordInfoBean;

    }
}
