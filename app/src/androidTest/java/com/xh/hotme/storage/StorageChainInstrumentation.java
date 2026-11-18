package com.xh.hotme.storage;

import android.app.Activity;
import android.app.Instrumentation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.FileInputStream;
import java.util.List;

/** Reads the real cloud list through accessibility, including a signed release APK. */
public class StorageChainInstrumentation extends Instrumentation {
    private String competitionName;
    private String expectedStatus;
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        competitionName=args.getString("competitionName", "E2E_1791386072610");
        expectedStatus=args.getString("expectedStatus", "AI分析成功");
        start();
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();
            info.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(info);
            try(ParcelFileDescriptor command=getUiAutomation().executeShellCommand(
                    "am start -n com.xh.hotme/.main.MainActivity");
                FileInputStream output=new FileInputStream(command.getFileDescriptor())) {
                byte[] bytes=new byte[1024];
                while(output.read(bytes)!=-1) { }
            }
            boolean selected=false;
            for(int i=0;i<60&&!selected;i++) {
                AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
                if(root!=null)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByViewId("com.xh.hotme:id/tab_icloud")) {
                    selected=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if(selected)break;
                }
                if(!selected)Thread.sleep(1000);
            }
            if(!selected)throw new AssertionError("Cloud navigation unavailable");
            Thread.sleep(2000);
            selected=false;
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("云存储")) {
                if("com.xh.hotme:id/tab_icloud".equals(node.getViewIdResourceName()))continue;
                AccessibilityNodeInfo target=node;
                for(int i=0;i<4&&target!=null&&!target.isClickable();i++)target=target.getParent();
                if(target!=null&&target.isClickable()) {
                    selected=target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if(selected)break;
                }
            }
            if(!selected)throw new AssertionError("Cloud storage tab unavailable");
            boolean verified=false;
            for(int i=0;i<60&&!verified;i++) {
                Thread.sleep(1000);
                root=getUiAutomation().getRootInActiveWindow();
                if(root==null)continue;
                for(AccessibilityNodeInfo name:root.findAccessibilityNodeInfosByText(competitionName)) {
                    if(!competitionName.contentEquals(name.getText()))continue;
                    AccessibilityNodeInfo row=name;
                    for(int depth=0;depth<5&&row!=null;depth++,row=row.getParent()) {
                        List<AccessibilityNodeInfo> statuses=row.findAccessibilityNodeInfosByText(expectedStatus);
                        if(statuses.size()==1) {
                            verified=expectedStatus.contentEquals(statuses.get(0).getText());
                            break;
                        }
                    }
                }
            }
            if(!verified)throw new AssertionError("Test competition did not display "+expectedStatus);
            result.putString("stream","\nPASS: signed release app shows "+competitionName+" with "+expectedStatus+" from the real backend\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            result.putString("stream","\nFAIL: "+e+"\n");
            finish(Activity.RESULT_CANCELED,result);
        }
    }
}
