package com.xh.hotme.bean;


import androidx.annotation.Keep;


/**
 * {
 * "competitionId": "string",
 * "createTime": "2024-01-02T04:45:27.592Z",
 * "createUserId": "string",
 * "deviceId": "string",
 * "deviceMac": "string",
 * "hmCompetition": {
 * "awayTeamLogo": "string",
 * "awayTeamName": "string",
 * "cityCode": "string",
 * "cityCodeDisplay": "string",
 * "competitionId": "string",
 * "competitionName": "string",
 * "competitionStatus": 0,
 * "competitionTime": "2024-01-02T04:45:27.592Z",
 * "createTime": "2024-01-02T04:45:27.592Z",
 * "createUserId": "string",
 * "deviceId": "string",
 * "homeTeamLogo": "string",
 * "homeTeamName": "string",
 * "motionType": "string",
 * "place": "string",
 * "placeType": "string",
 * "provinceCode": "string",
 * "provinceCodeDisplay": "string",
 * "updateTime": "2024-01-02T04:45:27.592Z",
 * "updateUserId": "string",
 * "videoHighlightCount": 0,
 * "videoReplayCount": 0,
 * "videoTotalCount": 0
 * },
 * "hmVideoUploadProgress": {
 * "createTime": "2024-01-02T04:45:27.592Z",
 * "createUserId": "string",
 * "domainId": "string",
 * "updateTime": "2024-01-02T04:45:27.592Z",
 * "updateUserId": "string",
 * "uploadProgress": 0,
 * "uploadSpeed": 0,
 * "uploadStatus": 0,
 * "uploadType": 0,
 * "videoId": "string",
 * "videoUrl": "string"
 * },
 * "updateTime": "2024-01-02T04:45:27.592Z",
 * "updateUserId": "string",
 * "uploadType": 0,
 * "videoClassify": 0,
 * "videoDuration": 0,
 * "videoHeight": 0,
 * "videoId": "string",
 * "videoName": "string",
 * "videoSize": 0,
 * "videoStatus": 0,
 * "videoTime": "2024-01-02T04:45:27.592Z",
 * "videoUrl": "string",
 * "videoWidth": 0
 * }
 */

@Keep
public class VideoDurationUpdateBean {

    private String videoId;
    private int videoDuration;


    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(int videoDuration) {
        this.videoDuration = videoDuration;
    }
}
