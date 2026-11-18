package com.xh.hotme.utils;

import android.text.TextUtils;

import androidx.annotation.Keep;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create by zhaozhihui on 2018/11/28
 **/

@Keep
public class TimeUtil {
    public static final int SECONDS_IN_DAY = 60 * 60 * 24;
    public static final long MILLIS_IN_DAY = 1000L * SECONDS_IN_DAY;

    private static final long ONE_MINUTE = 60;
    private static final long ONE_HOUR = 3600;
    private static final long ONE_DAY = 86400;
    private static final long ONE_MONTH = 2592000;
    private static final long ONE_YEAR = 31104000;

    private static final SimpleDateFormat zhYMD = new SimpleDateFormat("yyyy年MM月dd日");
    private static final SimpleDateFormat zhMD = new SimpleDateFormat("MM月dd日");
    private static final SimpleDateFormat generalYMDHMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat isoYMDHMSZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static boolean isSameDayOfMillis(final long ms1, final long ms2) {
        final long interval = ms1 - ms2;
        return interval < MILLIS_IN_DAY
            && interval > -1L * MILLIS_IN_DAY
            && toDay(ms1) == toDay(ms2);
    }

    private static long toDay(long millis) {
        return (millis + TimeZone.getDefault().getOffset(millis)) / MILLIS_IN_DAY;
    }

    public static boolean isThirtyBetween(long oldTime, long currentTime) {
        long interval = currentTime - oldTime;
        return interval <= 60 * 30 * 1000;

    }

    public static boolean isSevenDayBetween(long oldTime, long currentTime) {
        long interval = currentTime - oldTime;
        return interval <= 7 * 24 * 60 * 60 * 1000;

    }

    public static Date str2Date(String s) {
        if(TextUtils.isEmpty(s)) return null;
        boolean iso = s.endsWith("Z");
        SimpleDateFormat simpleDateFormat = iso ? isoYMDHMSZ : generalYMDHMS;
        Date date = null;
        try {
            date = simpleDateFormat.parse(s);
        } catch(ParseException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    public static long str2Millis(String s) {
        Date date = str2Date(s);
        return date == null ? 0 : date.getTime();
    }

    /*
     * 将时间戳转换为时间
     */
    public static Date stampToDateObj(String s) {
        try {
            return generalYMDHMS.parse(s);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * 将时间戳转换为时间
     * format : e.g.  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
     */
    public static String stampToDate(String s, SimpleDateFormat format) {
        try {
            String res;
            long lt = Long.valueOf(s);
            Date date = new Date(lt);
            res = format.format(date);
            return res;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return "";
    }


    /*
     * 将时间戳转换为时间
     * format : e.g.  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
     */
    public static String stampToDate(String s, SimpleDateFormat format, int days) {
        try {
            String res;
            long lt = Long.valueOf(s);
            Date date = new Date(lt);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, days);//1表示明天,-1表示昨天
            date = calendar.getTime();
            res = format.format(date);
            return res;
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 距离今天多少天
     *
     * @param date
     * @return
     */
    public static long fromToday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        long time = date.getTime() / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;

        long day = ago / ONE_DAY;
        if(day < 0) {
            day = -day;
        }
        return day;

    }


    /**
     * 距离今天多少天
     *
     * @param timeStamp
     * @return
     */
    public static long fromToday(String timeStamp) {

        Date date;
        date = new Date(timeStamp);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        long time = date.getTime() / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;

        long day = ago / ONE_DAY;
        if(day < 0) {
            day = -day;
        }
        return day;

    }


    /**
     * 距离当前时间戳相差多少天
     *
     * @param timeStamp
     * @return
     */
    public static long fromTodays(long timeStamp) {

        Date date;
        date = new Date(timeStamp);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        long time = date.getTime() / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;

        long day = ago / ONE_DAY;
        if(day < 0) {
            day = -day;
        }
        return day;

    }


    public static String stringToTime_new(long seconds) {
        String time = "";
        if(seconds <= 0) {
//            return "0天00小时00分";
            return "1秒";
        }
        if(seconds < 60) {
            time = seconds + "秒";
            return time;
        }


        long d = seconds / (3600 * 24);
        if(d > 0)
            time = time + d + "天";

        long h = seconds / 3600 - d * 24;
        if(h > 0) {
            if(h < 10) {
                time = time + "0" + h + "小时";
            } else {
                time = time + h + "小时";
            }
        }


        long min = seconds / 60 - d * 24 * 60 - h * 60;
        if(min > 0) {
            if(min < 10) {
                time = time + "0" + min + "分";
            } else {
                time = time + min + "分";
            }
        }

        long millisecond = seconds / 60 - d * 24 * 60 - h * 60 - min * 60;
        if(millisecond > 0) {
            if(millisecond < 10) {
                time = time + "0" + millisecond + "秒";
            } else {
                time = time + millisecond + "秒";
            }
        }

        if(TextUtils.isEmpty(time))
            time = "0秒";
        return time;
    }


    /**
     * 距离当前时间戳相差多少时间
     *
     * @param timeStamp
     * @return
     */
    public static String fromToday(long timeStamp) {

        Date date;
        date = new Date(timeStamp);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        long time = date.getTime() / 1000;
        long now = new Date().getTime() / 1000;
        long ago = now - time;

        if(ago < 0) {
            ago = -ago;
        }

        if(ago <= ONE_MINUTE)
            return ago + "秒";
        else if(ago <= ONE_HOUR)
            return ago / ONE_MINUTE + "分钟";
        else if(ago <= ONE_DAY)
            return ago / ONE_HOUR + "小时";
        else {
            long day = ago / ONE_DAY;
            return day + "天";
        }
    }

    public static boolean isSameDay(long currentTime, long lastTime) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);
        int y1 = c.get(Calendar.YEAR);
        int m1 = c.get(Calendar.MONTH);
        int d1 = c.get(Calendar.DAY_OF_MONTH);
        c.setTimeInMillis(lastTime);
        int y2 = c.get(Calendar.YEAR);
        int m2 = c.get(Calendar.MONTH);
        int d2 = c.get(Calendar.DAY_OF_MONTH);
        return y1 == y2 && m1 == m2 && d1 == d2;
    }

    public static int getStartDuration(int start_time) {
        int duration = 0;
        long end_time = System.currentTimeMillis();
        if(start_time != 0) {
            duration = Math.round((end_time - start_time) / 1000);
        }

        return duration;
    }

    public static long getStartDuration(long start_time) {
        int duration = 0;
        long end_time = System.currentTimeMillis();
        if(start_time != 0) {
            duration = Math.round((end_time - start_time) / 1000);
        }

        return duration;
    }

    public static long getStartDurationMs(String clientKey) {
        long duration = 0;
        long start_time = 0;
        long end_time = System.currentTimeMillis();
        try {
            start_time = Long.parseLong(clientKey);
        } catch(Exception e) {

        }
        if(start_time != 0) {
            duration = end_time - start_time;
        }

        return duration;
    }

    public static long getStartDuration(String clientKey) {
        int duration = 0;
        long start_time = 0;
        long end_time = System.currentTimeMillis();
        try {
            start_time = Long.parseLong(clientKey);
        } catch(Exception e) {

        }
        if(start_time != 0) {
            duration = Math.round((end_time - start_time) / 1000);
        }

        return duration;
    }

    /**
     * 判断当前时间是否在间隔天数以内
     *
     * @param timeStamp
     * @param day
     * @return
     */
    public static boolean isOnTime(long timeStamp, int day) {

        long curTimestamp = System.currentTimeMillis();
        return timeStamp + (long) day * 24 * 60 * 60 * 1000 > curTimestamp;
    }

    /***
     * 判断字符串是否是yyyyMMdd格式
     * @param mes 字符串
     * @return boolean 是否是日期格式
     */
    public static boolean isRqFormat(String mes) {
        String format = "([0-9]{4})-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])";
        Pattern pattern = Pattern.compile(format);
        Matcher matcher = pattern.matcher(mes);
        if(matcher.matches()) {
            pattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
            matcher = pattern.matcher(mes);
            if(matcher.matches()) {
                int y = Integer.valueOf(matcher.group(1));
                int m = Integer.valueOf(matcher.group(2));
                int d = Integer.valueOf(matcher.group(3));
                if(d > 28) {
                    Calendar c = Calendar.getInstance();
                    c.set(y, m - 1, 1);
                    //每个月的最大天数
                    int lastDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    return (lastDay >= d);
                }
            }
            return true;
        }
        return false;

    }

    public static String zhHumanDay(String tstr) {
        if(TextUtils.isEmpty(tstr)) {
            return "";
        }
        long t = str2Millis(tstr);
        if(isThisYear(t)) {
            if(isToday(t)) {
                return "今天";
            } else if(isYesterday(t)) {
                return "昨天";
            } else {
                return zhMD.format(new Date(t));
            }
        } else {
            return zhYMD.format(new Date(t));
        }
    }

    /*
     * 求两个日期（时间戳）相差多少天，
     */
    public static long diffDate(long startTimeStamp, long endTimeStamp) {

        Calendar nowCal = Calendar.getInstance();
        Calendar dataCal = Calendar.getInstance();
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
        String data1 = df1.format(startTimeStamp);
        String data2 = df2.format(endTimeStamp);
        try {
            Date startDate = df1.parse(data1);
            Date endDate = df2.parse(data2);
            nowCal.setTime(startDate);
            dataCal.setTime(startDate);
            long daysBetween = (endDate.getTime() - startDate.getTime() + 1000000) / (60 * 60 * 24 * 1000);
            return daysBetween;
        } catch(ParseException e) {

        }

        return 0;
    }

    public static String getCountDownTime(long seconds) {
        String countDownContent = "";
        int day = 0;
        int hour = 0;
        int minute = 0;
        long temp = seconds;
        if(seconds > 24 * 60 * 60) {
            day = (int) seconds / (24 * 60 * 60);
            countDownContent += day + ":";
            temp = seconds % (24 * 60 * 60);
        } else {
            temp = seconds;
        }
        long hours = temp % 3600;
        if(hours > 3600) {
            hour = (int) hours / 3600;
            if(hour < 10) {
                countDownContent += "0" + hour + ":";
            } else {
                countDownContent += hour + ":";
            }
        } else {
            if(day > 0) {
                countDownContent += "00:";
            }
        }
        long minutes = hours / 60;
        if(minutes > 0) {
            if(minutes < 10) {
                countDownContent += "0" + minutes + ":";
            } else {
                countDownContent += minutes + ":";
            }
        } else {
            countDownContent += "00:";
        }
        long second = hours % 60;
        if(second < 10) {
            countDownContent += "0" + second;
        } else {
            countDownContent += String.valueOf(second);
        }

        return countDownContent;
    }

    /**
     * 判断是否为今天(效率比较高)
     *
     * @param day 传入的 时间  "2016-06-28 10:10:30" "2016-06-28" 都可以
     * @return true今天 false不是
     * @throws ParseException
     */
    public static boolean isToday(String day) {
        return isToday(str2Millis(day));
    }

    /**
     * 判断是否为今天(效率比较高)
     *
     * @param timestamp 传入的 时间戳
     * @return true今天 false不是
     * @throws ParseException
     */
    public static boolean isToday(long timestamp) {
        Calendar c = Calendar.getInstance();
        int y1 = c.get(Calendar.YEAR);
        int d1 = c.get(Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(timestamp);
        int y2 = c.get(Calendar.YEAR);
        int d2 = c.get(Calendar.DAY_OF_YEAR);
        return y1 == y2 && d1 == d2;
    }

    public static boolean isYesterday(long t) {
        Calendar c = Calendar.getInstance();
        int y1 = c.get(Calendar.YEAR);
        int yd1 = c.get(Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(t);
        int y2 = c.get(Calendar.YEAR);
        int m2 = c.get(Calendar.MONTH);
        int d2 = c.get(Calendar.DAY_OF_MONTH);
        int yd2 = c.get(Calendar.DAY_OF_YEAR);
        if(yd1 > 1 && y1 == y2 && yd1 == yd2 + 1) {
            return true;
        } else return yd1 == 1 && y1 == y2 + 1 && m2 == Calendar.DECEMBER && d2 == 31;
    }

    public static boolean isYesterday(String s) {
        return isYesterday(str2Millis(s));
    }

    public static boolean isThisYear(long t) {
        Calendar c = Calendar.getInstance();
        int y1 = c.get(Calendar.YEAR);
        c.setTimeInMillis(t);
        int y2 = c.get(Calendar.YEAR);
        return y1 == y2;
    }

    public static int diffDate(String day) throws ParseException {

        Calendar pre = Calendar.getInstance();
        Date predate = new Date(System.currentTimeMillis());
        pre.setTime(predate);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
        Date date = df1.parse(day);
        cal.setTime(date);
        if(cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {
            int diffDay = pre.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR);
            return diffDay;
        } else {
            int diffDay = pre.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR);
            return diffDay;
        }

    }

    public static String millis2YMDHMS(long timeStamp) {
        Date date = new Date(timeStamp);
        return generalYMDHMS.format(date);
    }


    public static String intToTimeMin(int seconds) {
        if(seconds <= 0) {
            return "00:00";
        }
        String time = "";

        int min = seconds / 60;
        if(min < 10) {
            time = time + "0" + min + ":";
        } else {
            time = time + min + ":";
        }

        int sec = seconds - min * 60;
        if(sec < 10) {
            time = time + "0" + sec;
        } else {
            time = time + sec;
        }

        return time;
    }
    //支持分钟分隔符‘和"
    public static String intToTimeMin2(int seconds) {
        if(seconds <= 0) {
            return "00:00";
        }
        String time = "";

        int min = seconds / 60;
        if(min < 10) {
            time = time + "0" + min + "‘";
        } else {
            time = time + min + "‘";
        }

        int sec = seconds - min * 60;
        if(sec < 10) {
            time = time + "0" + sec;
        } else {
            time = time + sec;
        }

        return time+"''";
    }

    public static String intToTimeHour(int seconds) {
        if(seconds <= 0) {
            return "00:00:00";
        }
        String time = "";

        int hour = seconds / 3600;
        if(hour < 10) {
            time = time + "0" + hour + ":";
        } else {
            time = time + hour + ":";
        }
        seconds = seconds % 3600;
        int min = seconds / 60;
        if(min < 10) {
            time = time + "0" + min + ":";
        } else {
            time = time + min + ":";
        }

        int sec = seconds - min * 60;
        if(sec < 10) {
            time = time + "0" + sec;
        } else {
            time = time + sec;
        }

        return time;
    }

    public static String intToTimeHourOrMin(int seconds) {
        if(seconds >= 3600) {
            return intToTimeHour(seconds);
        } else {
            return intToTimeMin(seconds);
        }
    }

    public static int timeHourToInt(String timeStr) {

        String[] timeSplit = timeStr.split(":");
        if(timeSplit.length == 1) {
            return Integer.parseInt(timeSplit[0]);
        } else if(timeSplit.length == 2) {
            int minute = Integer.parseInt(timeSplit[0]);
            int second = Integer.parseInt(timeSplit[1]);
            return 60 * minute + second;
        } else if(timeSplit.length == 3) {
            int hour = Integer.parseInt(timeSplit[0]);
            int minute = Integer.parseInt(timeSplit[1]);
            int second = Integer.parseInt(timeSplit[2]);
            return 3600 * hour + 60 * minute + second;
        }

        return -1;
    }

    public static String getTodayString() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        return formatter.format(currentTime);
    }

    public static String getTodayEndString() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        return formatter.format(currentTime);
    }
}
