package com.xh.hotme.record;

import androidx.annotation.Keep;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xh.hotme.bean.RecordStatusResultBean;
import com.xh.hotme.http.RecordingApi;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.live.bean.CommonResponse;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.utils.SharedPrefs;
import java.util.ArrayList;
import java.util.List;

/** Durable state reports: a phone connection failure must not stop a local recording. */
public final class LocalRecordingReports {
    private static final String KEY = "local_recording_reports";
    @Keep private static class Report { String id, session; long version; boolean started; }
    private static boolean sending;
    private static final Runnable retry = LocalRecordingReports::flush;
    private LocalRecordingReports() { }
    private static List<Report> read() {
        String raw = SharedPrefs.getImpl().getString(KEY, "[]");
        try { List<Report> result = new Gson().fromJson(raw, new TypeToken<List<Report>>() {}.getType()); return result == null ? new ArrayList<>() : result; }
        catch (Exception ignored) { return new ArrayList<>(); }
    }
    private static void save(List<Report> reports) { SharedPrefs.getImpl().putString(KEY, new Gson().toJson(reports)); }
    public static void enqueue(RecordStatusResultBean status, boolean started) {
        String id = status.competitionId == null || status.competitionId.isEmpty() ? status.getLiveCId() : status.competitionId;
        enqueue(id, status.sessionId, status.deviceVersion, started);
    }
    public static void enqueue(String id, String session, long version, boolean started) {
        if (id == null || id.isEmpty() || session == null || session.isEmpty()) return;
        MainHandler.runOnUiThread(() -> {
            List<Report> reports = read();
            reports.removeIf(r -> id.equals(r.id) && session.equals(r.session) && r.started == started);
            Report report = new Report(); report.id = id; report.session = session; report.version = version; report.started = started;
            reports.add(report); save(reports); flush();
        });
    }
    public static void flush() {
        MainHandler.removeUITask(retry);
        List<Report> reports = read();
        if (reports.isEmpty()) return;
        if (sending || Constants.HEADER_TOKEN == null || Constants.HEADER_TOKEN.isEmpty()) { MainHandler.runOnUiThread(retry, 15000); return; }
        Report report = reports.get(0); sending = true;
        RecordingApi.reportLocal(report.id, report.session, report.version, report.started, new IGenericApiCallback<CommonResponse>() {
            public void onSuccess(CommonResponse response) { remove(); }
            public void onFail(String code, String message) {
                // Business rejection means the session was superseded/ended;
                // transport and login failures remain queued for another try.
                if ("501".equals(code)) remove();
            }
            private void remove() {
                List<Report> current = read();
                current.removeIf(r -> report.id.equals(r.id) && report.session.equals(r.session)
                        && report.started == r.started && report.version == r.version); save(current);
            }
            public void onFinish() { sending = false; MainHandler.runOnUiThread(retry, 15000); }
        });
    }
}
