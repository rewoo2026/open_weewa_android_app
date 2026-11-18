package com.xh.hotme;

import org.junit.Test;

import static org.junit.Assert.*;

import android.util.Log;

import com.xh.hotme.bluetooth.BleConstants;
import com.xh.hotme.utils.AppTrace;

import java.util.ArrayList;

public class JsonUnitTest {
    @Test
    public void addition_isCorrect() {
        String sourceData = "\u0001{\"c\":\"softap_open\",\"r\":\"ok\",\"p\":\"00000000\",\"s\":\"weewa03\"}\u0004\u0001{\"c\":\"usage_info\",\"e\":97,\"t\":61,\"s\":{\"total\":107330945024,\"used\":37178802176,\"free\":70152142848}}\u0004";
        byte[] dataByte = sourceData.getBytes();
        ArrayList<String> dataList = new ArrayList();
        StringBuilder cmd = new StringBuilder();
        int start = 1;
        for (int i = 1; i < sourceData.length(); i++) {
            if (dataByte[i] == BleConstants.BLE_CMD_CHAR_END) {
                if (start < i) {
                    String data = sourceData.substring(start, i);
                    dataList.add(data);
                }
            } else if (dataByte[i] == BleConstants.BLE_CMD_CHAR_START) {
                start = i + 1;
            }
        }
        if (dataList != null && dataList.size() > 0) {
            for (int i = 0; i < dataList.size(); i++) {
                String data = dataList.get(i);
                System.out.println("cmd:" + data);
            }
            dataList.clear();
        }
        dataList = null;
    }
}
