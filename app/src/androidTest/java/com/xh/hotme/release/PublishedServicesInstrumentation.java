package com.xh.hotme.release;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.*;

/** Read-only acceptance from the real handset against the published services and APK. */
public class PublishedServicesInstrumentation extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle args) { super.onCreate(args); this.args=args; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle(); int outcome=Activity.RESULT_OK;
        try {
            String base="http://223.109.239.30:61004";
            verifyWeb(base+"/");
            verifyWeb("http://223.109.239.30:61002/");
            JSONObject health=new JSONObject(read(base+"/hotme-api/hm/appVersion/apiTest",null,null));
            check(health.getInt("code")==200,"Java service health from handset");
            String token=getTargetContext().getSharedPreferences("com.xh.hotme.utils.LoginSpUtil",0)
                    .getString("prfs_user_token",null);
            check(token!=null&&!token.isEmpty(),"Existing authenticated handset session");
            String cid=args.getString("competitionId");
            check(cid!=null&&cid.matches("900[0-9]{16}"),"Named real acceptance recording ID");
            JSONObject body=new JSONObject().put("deviceId","zzg20230bde63f46f0").put("competitionId",cid);
            JSONObject response=new JSONObject(read(base+"/hotme-api/hm/matchAnalysis/result",body.toString(),token));
            check(response.getInt("code")==200,"Authenticated GPU result delivered to handset");
            JSONObject data=response.getJSONObject("data");
            long frames=data.getLong("totalFrames");
            check(frames>0&&data.getLong("processedFrames")==frames,"All native analysis frames completed");
            JSONObject version=new JSONObject(read(base+"/hotme-api/hm/appVersion/getLatestAppByVersion?appType=5&versionCode=124",null,token));
            check(version.getInt("code")==200,"Update discovery from handset");
            JSONObject release=version.getJSONObject("data");
            check(release.getInt("versionCode")==125&&"1.2.5".equals(release.getString("version")),"Published update metadata");
            check("0".equals(release.getString("forceUpdateFlag")),"Release is optional");
            String expected=args.getString("apkSha256");
            check(expected!=null&&expected.matches("[a-f0-9]{64}"),"Expected signed APK digest provided");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            HttpURLConnection connection=open(release.getString("pkgUrl"),null,null);
            try(InputStream in=connection.getInputStream()) {
                byte[] buffer=new byte[65536];int count;
                while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);
            } finally { connection.disconnect(); }
            StringBuilder actual=new StringBuilder();for(byte value:digest.digest())actual.append(String.format("%02x",value&255));
            check(expected.equals(actual.toString()),"Published APK downloads intact on real handset");
            result.putString("stream","\nPASS: real handset loads analysis/admin web bundles, Java health, authenticated completed GPU result ("+frames+" frames), update metadata and exact APK SHA-256.\n");
        } catch(Throwable error) {
            outcome=Activity.RESULT_CANCELED;result.putString("stream","\nFAIL: "+error+"\n");
        } finally { finish(outcome,result); }
    }
    private void verifyWeb(String url)throws Exception {
        String html=read(url,null,null);check(html.contains("id=\"app\"")||html.contains("id=app"),"Published web entry: "+url);
        Matcher scripts=Pattern.compile("<script[^>]+src=[\"']?([^\"' >]+)").matcher(html);
        int count=0;while(scripts.find()) {
            String asset=new URL(new URL(url),scripts.group(1)).toString();
            check(read(asset,null,null).length()>0,"Web JavaScript asset loads from handset");count++;
        }
        check(count>0,"Published web references actual JavaScript bundles");
    }
    private HttpURLConnection open(String url,String body,String token)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(20000);connection.setReadTimeout(60000);
        if(token!=null)connection.setRequestProperty("Authorization",token.startsWith("Bearer ")?token:"Bearer "+token);
        if(body!=null) { connection.setRequestMethod("POST");connection.setRequestProperty("Content-Type","application/json");connection.setDoOutput(true);try(OutputStream out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));} }
        check(connection.getResponseCode()==200,"HTTP status for "+new URL(url).getPath());return connection;
    }
    private String read(String url,String body,String token)throws Exception {
        HttpURLConnection connection=open(url,body,token);
        try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[16384];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            return out.toString("UTF-8");
        } finally { connection.disconnect(); }
    }
    private void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
