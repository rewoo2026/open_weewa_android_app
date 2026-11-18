package com.xh.hotme.bean;

import java.io.Serializable;
import java.util.List;

/**
 * date   : 2023/6/27
 * desc   :
 * c	string	video_detail_list	固定值
 * videos	Arrays[Video]		视频文件列表
 * Video.type	int	0	[0, 1, 2,3]0 全场视频，1精彩集锦
 * 2 精彩回放 3主队射门，4客队射门
 * Video.name	string	全场视频_01	视频名称
 * Video.duration	int	30	视频时长，单位秒
 * Video.link	string	/2023/桃源_向华v恒大	视频文件路径
 * Video.video_width	int	1902	视频分辨率 宽
 * Video.video_height	int	1080	视频分辨率 高
 */
public class CameraVideoDetailListBean implements Serializable {

    public String c;
    public List<VideosBean> videos;

    public static class VideosBean extends CameraVideosBean {
        public String videoId;
        public String competitionId;
        public String domainId;

        // 本地json专用, 表示是否还在下载
        public boolean downloading = false;
        public int videoStatus = 1;

        public VideosBean() {
        }

        public VideosBean(VideosBean bean) {
            super(bean);
            videoId = bean.videoId;
            competitionId = bean.competitionId;
            domainId = bean.domainId;
            downloading = bean.downloading;
        }

        public VideosBean(HmVideo bean) {
            super(bean);
            videoId = bean.getVideoId();
            competitionId = bean.getCompetitionId();
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public String getCompetitionId() {
            return competitionId;
        }

        public void setCompetitionId(String competitionId) {
            this.competitionId = competitionId;
        }

        public String getDomainId() {
            return domainId;
        }

        public void setDomainId(String domainId) {
            this.domainId = domainId;
        }

        public int getVideoStatus() {
            return videoStatus;
        }

        public void setVideoStatus(int videoStatus) {
            this.videoStatus = videoStatus;
        }
    }
}
