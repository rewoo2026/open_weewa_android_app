package com.xh.hotme.utils;


import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ReTextUtils {


    //修改最后一个文字的文字大小
    public static void changeLastTextSize(TextView tv, String str, int size) {

        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        builder.setSpan(new AbsoluteSizeSpan(size, true), str.length() - 1, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(builder);
    }


    /**
     * 修改部分字体颜色
     *
     * @param tv   textview
     * @param str2 要替换的文字
     */
    public static void changeLastTextColor(TextView tv, String str2, int color) {
        changeLastTextColor(tv, tv.getText().toString(), str2, color);
    }

    /**
     * 修改部分字体颜色
     *
     * @param tv   textview
     * @param str  显示的文字
     * @param str2 要替换的文字
     */
    public static void changeLastTextColor(TextView tv, String str, String str2, int color) {
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        String[] split = str2.split(",");

        for(String s : split) {
            Matcher matcher = Pattern.compile(s).matcher(str);
            while(matcher.find()) {
                builder.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.start() + s.length(), 0);
            }
        }
        tv.setText(builder);
    }


    /**
     * 修改部分字体大小
     *
     * @param tv   textview
     * @param str  显示的文字 多个,隔开
     * @param str2 要替换的文字
     */
    public static void changeLastTextSize(TextView tv, String str, String str2, int size) {
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        String[] split = str2.split(",");
        for(String s : split) {
            Matcher matcher = Pattern.compile(s).matcher(str);
            while(matcher.find()) {
                builder.setSpan(new AbsoluteSizeSpan(size, true), matcher.start(), matcher.start() + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(builder);
    }

//    int number = new Random().nextInt(10);
//    viewPager.setId(context.getResources().getIdentifier("xxx" + number, "id", context.getPackageName()));

//        FragmentManager manager = activity.getSupportFragmentManager();
//        YTBaseFragment homeFragment = (YTBaseFragment) manager.findFragmentByTag("0");


    private static int getDayofWeek(String dateTime) {

        Calendar cal = Calendar.getInstance();
        if(dateTime.equals("")) {
            cal.setTime(new Date(System.currentTimeMillis()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Date date;
            try {
                dateTime = dateTime.replaceAll("年", "-");
                dateTime = dateTime.replaceAll("月", "-");
                dateTime = dateTime.replaceAll("日", "");
                date = sdf.parse(dateTime);
            } catch(Exception e) {
                date = null;
                e.printStackTrace();
            }
            if(date != null) {
                cal.setTime(new Date(date.getTime()));
            }
        }
        return cal.get(Calendar.DAY_OF_WEEK);
    }


    public static String Week(String dateTime) {
        String week = "";
        switch(getDayofWeek(dateTime)) {
            case 1:
                week = "周日";
                break;
            case 2:
                week = "周一";
                break;
            case 3:
                week = "周二";
                break;
            case 4:
                week = "周三";
                break;
            case 5:
                week = "周四";
                break;
            case 6:
                week = "周五";
                break;
            case 7:
                week = "周六";
                break;
        }
        return week;
    }


}
