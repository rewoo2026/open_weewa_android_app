package com.xh.hotme.storage;

import androidx.annotation.Keep;
import com.xh.hotme.live.bean.CommonResponse;
import java.util.List;

@Keep
public class MatchAnalysisTask {
    public String competitionId,deviceId,competitionName,state,message;
    public int uploadPercent,analysisPercent;
    public long uploadedBytes,totalBytes,processedFrames,totalFrames;
    public boolean active(){return !"done".equals(state)&&!"failed".equals(state);}
    public String label(){
        if(state==null)return "任务已创建";
        switch(state){case "waiting_network":return "等待相机联网";case "uploading":return "相机上传中";case "queued":return "等待分析";case "preparing":return "服务器准备中";case "analyzing":return "AI分析中";case "finalizing":return "正在生成结果";case "done":return "分析完成 · 查看结果";case "failed":return "任务失败 · 重试";default:return "任务已创建";}
    }
    @Keep public static class Response extends CommonResponse {public List<MatchAnalysisTask> data;}
}
