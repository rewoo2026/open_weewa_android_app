//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.xh.hotme.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private static boolean showToast = true;

    public StringUtils() {
    }

    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
//        if (str != null) {
//            str = str.trim();
//        }
//
//        return str == null || str.equals("") || str.equals("null");
    }

    public static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public static boolean stringsEquals(String str1, String str2) {
        if(!isNotEmpty(str1) && !isNotEmpty(str2)) {
            return true;
        } else {
            return isNotEmpty(str1) && str1.equals(str2);
        }
    }

    public static String lastPathComponent(String s) {
        if(TextUtils.isEmpty(s)) return "";
        int idx = s.lastIndexOf('/');
        return idx == -1 ? s : s.substring(idx + 1);
    }

    public static boolean isNotEmpty(String string) {
        return null != string && !string.isEmpty() && !"".equals(string) && !"null".equals(string) && !"(null)".equals(string);
    }

    public static String formatNowYMD(Date date) {
        return (new SimpleDateFormat("yyyy-MM-dd")).format(date);
    }

    public static boolean isHave(String strs, String s) {
        return strs.length() >= s.length() && strs.contains(s);
    }

    public static String url2uft8(String url) {
        if(url == null) {
            return "";
        } else {
            String utf8Url = "";

            try {
                for(int i = 0; i < url.length(); ++i) {
                    if(url.charAt(i) > 255) {
                        utf8Url = utf8Url + URLEncoder.encode(url.charAt(i) + "", "UTF-8");
                    } else {
                        utf8Url = utf8Url + url.charAt(i);
                    }
                }

                return utf8Url;
            } catch(UnsupportedEncodingException var3) {
                return "";
            }
        }
    }

    public static boolean bPassword(String password) {
        if(TextUtils.isEmpty(password)) {
            return false;
        } else if(password.length() >= 6 && password.length() <= 20) {
            for(int i = 0; i < password.length(); ++i) {
                if(password.charAt(i) > 255) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static int getCharCount(String src) {
        if(src == null) {
            return 0;
        } else {
            int count = 0;

            for(int i = 0; i < src.length(); ++i) {
                if(src.charAt(i) > 255) {
                    count += 2;
                } else {
                    ++count;
                }
            }

            return count;
        }
    }

    public static String getSubString(String src, int count) {
        if(src == null) {
            return "";
        } else {
            String des = "";
            int curCount = 0;

            for(int i = 0; i < src.length(); ++i) {
                if(src.charAt(i) > 255) {
                    curCount += 2;
                } else {
                    ++curCount;
                }

                if(curCount >= count) {
                    break;
                }

                des = des + src.charAt(i);
            }

            return des;
        }
    }

    public static void limitEditLength(final Context context, final EditText editText, final int maxLen, final String des) {
        editText.addTextChangedListener(new YTextWatcher() {
            public void afterTextChanged(Editable s) {
                int count = StringUtils.getCharCount(editText.getText().toString());
                if(count > maxLen) {
                    String tempStr = StringUtils.getSubString(editText.getText().toString(), maxLen + 1);
                    editText.setText(tempStr);
                    editText.setSelection(editText.getText().length());
                    if(!TextUtils.isEmpty(des) && StringUtils.showToast) {
                        ToastUtil.s(des);
                    }
                } else if(count < maxLen) {
                }

            }
        });
    }

    public static void limitEditLengthToastOnce(final Context context, final EditText editText, final int maxLen, final String des) {
        editText.addTextChangedListener(new YTextWatcher() {
            public void afterTextChanged(Editable s) {
                int count = editText.getText().length();
                if(count > maxLen) {
                    String tempStr = editText.getText().toString().substring(0, maxLen);
                    editText.setText(tempStr);
                    editText.setSelection(editText.getText().length());
                    if(!TextUtils.isEmpty(des) && StringUtils.showToast) {
                        ToastUtil.s(des);
                        StringUtils.showToast = false;
                    }
                } else if(count < maxLen) {
                    StringUtils.showToast = true;
                }

            }
        });
    }

    public static void showRemainsLength(final TextView editText, final TextView textView, final int maxLen, final int showType) {
        editText.addTextChangedListener(new YTextWatcher() {
            public void afterTextChanged(Editable s) {
                int count = editText.getText().length();
                if(showType == 0) {
                    textView.setText(count + "/" + maxLen);
                } else {
                    textView.setText(String.valueOf(maxLen - count));
                }

            }
        });
    }

    public static String formatNumber(String text) {
        if(text == null) {
            return "";
        } else {
            DecimalFormat df;
            if(text.indexOf(".") > 0) {
                if(text.length() - text.indexOf(".") - 1 == 0) {
                    df = new DecimalFormat("###,##0.");
                } else if(text.length() - text.indexOf(".") - 1 == 1) {
                    df = new DecimalFormat("###,##0.0");
                } else {
                    df = new DecimalFormat("###,##0.00");
                }
            } else {
                df = new DecimalFormat("###,##0");
            }

            double number;
            try {
                number = Double.parseDouble(text);
            } catch(Exception var5) {
                number = 0.0D;
            }

            return df.format(number);
        }
    }

    public static String formatMoney(String text) {
        if(text == null) {
            return "";
        } else {
            DecimalFormat df = new DecimalFormat("###,##0.00");

            double number;
            try {
                number = Double.parseDouble(text);
            } catch(Exception var5) {
                number = 0.0D;
            }

            return number >= 1000000.0D ? df.format(number / 10000.0D) + "W" : df.format(number);
        }
    }

    public static String getFormatSize(double size) {

        double kiloByte = size / 1024;
        if(kiloByte < 1) {
            return size + "B";
        }

        double megaByte = kiloByte / 1024;
        if(megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, RoundingMode.HALF_UP).toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if(gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, RoundingMode.HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if(teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, RoundingMode.HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);

        return result4.setScale(2, RoundingMode.HALF_UP).toPlainString() + "TB";
    }

    public static String formatDouble(String text) {
        if(text == null) {
            return "0";
        } else {
            BigDecimal bigDecimal = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
            double number = bigDecimal.doubleValue();
            DecimalFormat df = new DecimalFormat("###,##0.0");
            DecimalFormat df2 = new DecimalFormat("#.0");
            if(number >= 10000.0D) {
                return df2.format(number / 10000.0D) + "W";
            } else {
                String s = df.format(number);
                if(s.endsWith(".0")) {
                    s = s.replace(".0", "");
                }
                return s;
            }
        }
    }

    /**
     * B类直播间用的格式化
     * 云币、积分数据
     * （1）当数字小于10000时，数字全部显示；举例：6852
     * （2）当数字大于10000时，显示XX.X；举例325.4w 小数点后保留一位小数
     * 当数字超过1亿时，仍然以w为单位，举例：54323w
     *
     * @return
     */
    public static String formatNum(String text) {
        if(text == null) {
            return "0";
        }
        BigDecimal bigDecimal = new BigDecimal(text).setScale(2, RoundingMode.HALF_UP);
        double number = bigDecimal.doubleValue();
        if(number >= 10000.0D) {
            DecimalFormat df2 = new DecimalFormat("#.0");
            return df2.format(number / 10000.0D) + "W";
        } else {
            return (int) number + "";
        }
    }

    public static String formatScore(String text) {
        if(text == null) {
            return "";
        } else {
            DecimalFormat df = new DecimalFormat("###,##0");

            double number;
            try {
                number = Double.parseDouble(text);
            } catch(Exception var5) {
                number = 0.0D;
            }

            return Math.abs(number) >= 1000000.0D ? df.format(number / 10000.0D) + "W" : df.format(number);
        }
    }

    public static boolean isDouble(String doubleStr) {
        try {
            double d = Double.parseDouble(doubleStr);
            return true;
        } catch(Exception var3) {
            var3.printStackTrace();
            return false;
        }
    }

    public static boolean isFree(String fee) {
        return "0".equals(fee) || "0.0".equals(fee) || "0.00".equals(fee) || isEmpty(fee);
    }


    /**
     * 版本号对比， 默认字符大于数字: 如("10.0.1b", "10.0.1a")
     *
     * @param ver1 版本号1
     * @param ver2 版本号2
     * @return 版本号1 是否大于等于 版本号2
     */
    public static boolean versionCompare(String ver1, String ver2) {

        ver1 = StringUtils.isEmpty(ver1) ? "99.99" : ver1;
        ver2 = StringUtils.isEmpty(ver2) ? "99.99" : ver2;

        String[] verA = ver1.split("\\.", -1);
        String[] verB = ver2.split("\\.", -1);

        int com;
        int len = Math.max(verA.length, verB.length);
        // 补齐字符串数组
        if(verA.length < len)
            verA = stringExt(verA, len);
        else if(verB.length < len)
            verB = stringExt(verB, len);

        for(int i = 0; i < len; i++) {
            try {
                com = Integer.parseInt(verA[i]) - Integer.parseInt(verB[i]);
            } catch(Exception e) {
                AppTrace.e(String.format("versionCompare err: ver1=>%s, ver1=>%s", ver1, ver2), e);
                com = numAndCharCompare(verA[i], verB[i]);
            }

            if(com > 0) return true;
            else if(com < 0) return false;
        }

        // if equal, return false
        return false;
    }

    /**
     * 数字与字符组成字符串大小比较，如：(10a 9a)，(9a 9)，(9b 9a)
     *
     * @param left
     * @param right
     * @return 1、0、-1
     */
    public static int numAndCharCompare(String left, String right) {

        left = left.isEmpty() ? "0" : left;
        right = right.isEmpty() ? "0" : right;

        // 判断字符串首字符是否为数字
        boolean bL = left.charAt(0) >= 48 && left.charAt(0) <= 57;
        boolean bR = right.charAt(0) >= 48 && right.charAt(0) <= 57;
        if(bL && !bR)
            return -1;
        else if(!bL && bR)
            return 1;

        // 获取数字
        String[] numLeft = Pattern.compile("[^0-9]").matcher(left).replaceAll(",").split(",");
        String[] numRight = Pattern.compile("[^0-9]").matcher(right).replaceAll(",").split(",");

        // 获取字符串
        List<String> charLeft = new ArrayList<>();
        List<String> charRight = new ArrayList<>();
        Matcher matcherL = Pattern.compile("\\D+").matcher(left);
        Matcher matcherR = Pattern.compile("\\D+").matcher(right);
        while(matcherL.find()) {
            charLeft.add(matcherL.group(0));
        }
        while(matcherR.find()) {
            charRight.add(matcherR.group(0));
        }

        int numL = numLeft.length;
        int numR = numRight.length;
        int numLen = Math.max(numL, numR);
        // 补齐字符串数组
        if(numL < numLen)
            numLeft = stringExt(numLeft, numLen);
        else if(numR < numLen)
            numRight = stringExt(numRight, numLen);


        // 补齐List
        int charL = charLeft.size();
        int charR = charRight.size();
        int charMax = Math.max(charL, charR);
        if(charL < charMax)
            listExt(charLeft, charL, charMax);
        else if(charR < charMax)
            listExt(charRight, charR, charMax);

        int com = 0;
        if(bL) // 同为数字开头
        {
            for(int i = 0; i < numLen; i++) {
                com = Integer.parseInt(numLeft[i]) - Integer.parseInt(numRight[i]);
                if(com == 0 && i < charMax)
                    com = charLeft.get(i).compareTo(charRight.get(i));

                if(com > 0) return 1;
                else if(com < 0) return -1;
            }
        } else {
            for(int i = 0; i < charMax; i++) {
                com = charLeft.get(i).compareTo(charRight.get(i));
                if(com == 0 && i < numLen)
                    com = Integer.parseInt(numLeft[i + 1]) - Integer.parseInt(numRight[i + 1]);

                if(com > 0) return 1;
                else if(com < 0) return -1;
            }
        }

        return 0;
    }

    /**
     * 补齐字符串数组长度
     *
     * @param data 字符串数组
     * @param len  补齐长度
     * @return 补齐后的数组
     */
    public static String[] stringExt(String[] data, int len) {
        String[] res = new String[len];

        int index = 0;
        for(String datum : data) {
            res[index++] = datum;
        }

        while(index < len) {
            res[index++] = "0";
        }

        return res;
    }

    /**
     * 补齐List长度
     *
     * @param data list
     * @param size 自身长度
     * @param len  补齐长度
     */
    public static void listExt(List<String> data, int size, int len) {
        while(size < len) {
            data.add("0");
            size++;
        }
    }


    public static boolean isIdentification(String string) {
        String regex = "^(\\d{6})(\\d{4})(\\d{2})(\\d{2})(\\d{3})([0-9]|X)$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(string);
        return m.matches();
    }


    public static boolean isNull(CharSequence cs) {
        if(TextUtils.isEmpty(cs)) {
            return true;
        }
        if(cs == "null") {
            return true;
        }
        return TextUtils.isEmpty(cs.toString().trim());
    }


    public static boolean isPhone(String phone) {
        String regex = "^((13[0-9])|(14[0-9])|(15[0-9])|(16[0-9])|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(phone);
        return m.matches();
    }


    public static String hidePhoneMid(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }


    //获取String中的数字

    public static String getStringNum(String str) {

        str = str.trim();
        String str2 = "";
        if(str != null && !"".equals(str)) {
            for(int i = 0; i < str.length(); i++) {
                if(str.charAt(i) >= 48 && str.charAt(i) <= 57) {
                    str2 += str.charAt(i);
                }
            }
        }
        return str2;
    }


    public static String getStringType(String str) {

        str = str.trim();
        String str2 = "";
        if(str != null && !"".equals(str)) {
            for(int i = 0; i < str.length(); i++) {

                if((str.charAt(i)) == '月') {
                    return "月";
                }
                if((str.charAt(i)) == '天') {
                    return "天";
                }
                if((str.charAt(i)) == '年') {
                    return "年";
                }

            }
        }
        return str2;
    }

    /**
     * 判断对象或对象数组中每一个对象是否为空: 对象为null，字符序列长度为0，集合类、Map为empty
     *
     * @param obj
     * @return
     */
    public static boolean isNullOrEmpty(Object obj) {
        if(obj == null)
            return true;

        if(obj instanceof CharSequence)
            return ((CharSequence) obj).length() == 0;

        if(obj instanceof Collection)
            return ((Collection) obj).isEmpty();

        if(obj instanceof Map)
            return ((Map) obj).isEmpty();

        if(obj instanceof Object[]) {
            Object[] object = (Object[]) obj;
            if(object.length == 0) {
                return true;
            }
            boolean empty = true;
            for(Object o : object) {
                if(!isNullOrEmpty(o)) {
                    empty = false;
                    break;
                }
            }
            return empty;
        }
        return false;
    }

    //是否是数字
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }
}
