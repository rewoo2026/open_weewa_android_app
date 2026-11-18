package com.xh.hotme.bean;

import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.StringUtils;
import com.xh.hotme.utils.TimeUtil;

import java.io.Serializable;
import java.util.List;

/**
 * date   : 2023/6/27
 * desc   :
 * c	string	videolist	固定值
 * videos	Array[Video]		视频列表
 * Video.time	string	2023-06-26 10:30	视频时间
 * Video.address	string	桃源球场	拍摄地点
 * Video.teams	string	向华vs恒大	比赛队伍
 * Video.competition_name	string	向华vs恒大	比赛名称
 * Video.thumb	string	/20230625/1030/桃源_向华v恒大_thumb.jpg	视频缩略图
 * Video.path	string	/20230625/1030	视频文件目录
 * Video.video_width	int	1902	视频分辨率 宽
 * Video.video_height	int	1080	视频分辨率 高
 * Video。full_video_number	int	4	全场视频数
 * Video。highlights_number	int	3	集锦视频数
 * Video。playback_number	int	3	回放视频数
 * Video.home_goals	int	2	主队进球视频数
 * Video.away_goals	int	1	客队进球视频数
 */
public class CameraVideoListBean implements Serializable {


    public String c;
    public String request_id;
    public String device_id;
    public int video_delete_protocol;
    public List<CameraVideoListBean.VideosBean> videos;

    public static class VideosBean implements Serializable, ITimeHolder {
        public String time;
        public String address;
        public String teams;
        public String competition_name;
        public String competition_id;
        public int competition_type;
        public long competition_duration;
        public String motion_type;
        public String place_type;
        public String city;
        public String host_team;
        public String guest_team;
        public String path;
        public String link;
        public String thumb;
        public String video_width;
        public String video_height;
        public int full_video_number;
        public int highlights_number;
        public int playback_number;
        public int ai_highlights_number;
        public int home_goals;
        public int away_goals;

        public String deviceId;
        public String delete_block_reason;
        public int delete_protocol;
        public String delete_identity;
        public int player_num;

        public VideosBean() {
        }

        public VideosBean(HmVideo result) {
            path = result.getPath();

            // 视频数量先不设置, 因为有可能不是下载全部视频
            HmCompetition bean = result.getHmCompetition();
            time = bean.getCompetitionTime();
            address = bean.getPlace();
            city = bean.getCityCodeDisplay();
            teams = String.format("%s vs %s", bean.getHomeTeamName(), bean.getAwayTeamName());
            competition_name = bean.getCompetitionName();
            competition_id = bean.getCompetitionId();
            competition_type = result.getCompetitionType();
            motion_type = bean.getMotionType();
            place_type = String.valueOf(bean.getPlaceType());
            host_team = bean.getHomeTeamName();
            guest_team = bean.getAwayTeamName();
            thumb = StringUtils.lastPathComponent(bean.getCompetitionImage());
            deviceId = bean.getDeviceId();
            full_video_number = 0;
            highlights_number = 0;
            playback_number = 0;
            ai_highlights_number = 0;
            home_goals = 0;
            away_goals = 0;
        }

        public VideosBean(VideosBean bean) {
            time = bean.time;
            address = bean.address;
            teams = bean.teams;
            competition_name = bean.competition_name;
            competition_id = bean.competition_id;
            competition_type = bean.competition_type;
            competition_duration = bean.competition_duration;
            motion_type = bean.motion_type;
            place_type = bean.place_type;
            city = bean.city;
            host_team = bean.host_team;
            guest_team = bean.guest_team;
            path = bean.path;
            link = bean.link;
            thumb = bean.thumb;
            video_width = bean.video_width;
            video_height = bean.video_height;
            full_video_number = bean.full_video_number;
            highlights_number = bean.highlights_number;
            playback_number = bean.playback_number;
            ai_highlights_number = bean.ai_highlights_number;
            home_goals = bean.home_goals;
            away_goals = bean.away_goals;
            deviceId = bean.deviceId;
            player_num = bean.player_num;
        }

        public int getVideoTotalCount() {
            return full_video_number + highlights_number + playback_number + home_goals + away_goals + ai_highlights_number;
        }

        @Override
        public long getTimeInMillis() {
            return TimeUtil.str2Millis(time);
        }
    }

}
