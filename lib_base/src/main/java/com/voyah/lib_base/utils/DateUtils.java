package com.voyah.lib_base.utils;

import android.text.TextUtils;


import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateUtils {

    /*根据时间格式转星期*/
    public static String getWeek(String date) {
        String week="";
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(date, formatter);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            week=dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
        } catch (Exception e) {

        }
        return week;
    }


    /**
     * 获取当前时间戳（10位数-秒）
     *
     * @return
     */
    public static String getNewTime10() {
        long timestampMillis = System.currentTimeMillis();
        long timestampSeconds = timestampMillis / 1000;
        return timestampSeconds + "";
    }

    public static String calcTime(int time) {
        if (time < 60) {
            return time + "秒";
        } else if (time / 60 < 60) {
            return time / 60 + "分" + time % 60 + "秒";
        } else if (time / 60 / 60 < 24) {
            //return time / 60 / 60 + "小时" + time / 60 % 60 + "分" + time % 60 + "秒";
            return time / 60 / 60 + "小时" + time / 60 % 60 + "分"+ time % 60 + "秒";
        } else {
            //return time / 60 / 60 / 24 + "天" + time / 60 / 60 % 24 + "小时" + time / 60 % 60 + "分" + time % 60 + "秒";
            return time / 60 / 60 / 24 + "天" + time / 60 / 60 % 24 + "小时" + time / 60 % 60 + "分" + time % 60 + "秒";
        }
    }


    public static String calcTimeDHM(long time) {
        if (time / 60 < 60) {
            return time / 60 + "min";
        } else if (time / 60 / 60 < 24) {
            //return time / 60 / 60 + "小时" + time / 60 % 60 + "分" + time % 60 + "秒";
            return time / 60 / 60 + "h" + time / 60 % 60 + "min";
        } else {
            //return time / 60 / 60 / 24 + "天" + time / 60 / 60 % 24 + "小时" + time / 60 % 60 + "分" + time % 60 + "秒";
            return time / 60 / 60 / 24 + "d" + time / 60 / 60 % 24 + "h" + time / 60 % 60 + "min";
        }
    }

    public static String minutesToHoursAndMinutes(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("小时");
        }
        result.append(remainingMinutes).append("分钟");
        return result.toString();
    }

    //时间戳转早中晚
    public static String getTimePeriod(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        if (Integer.valueOf(sd) >= 5 && Integer.valueOf(sd) < 11) {
            return "早上好!";
        } else if (Integer.valueOf(sd) >= 11 && Integer.valueOf(sd) < 13) {
            return "午安!";
        } else if (Integer.valueOf(sd) >= 13 && Integer.valueOf(sd) < 19) {
            return "下午好!";
        } else if (Integer.valueOf(sd) >= 19 && Integer.valueOf(sd) < 24) {
            return "晚上好!";
        } else {
            return "夜深了，要好好休息哦!";
        }
    }

    public static String getTime(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
        return format.format(date);
    }

    public static String getTimeMDHM(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日HH点mm分");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    public static String getTimeMD(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("MM月dd日");
        return format.format(date);
    }
    public static String getTimeMD2(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("MM.dd");
        return format.format(date);
    }

    public static String hisstampToHourMinus() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        return sdf.format(new Date());
    }

    public static String getDateToString(String time) { //yyyy-MM-dd 转MM月dd日
        String overTime = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MM月dd日");
        Date date = null;
        try {
            date = sdf.parse(time);
            overTime = sdf2.format(date);
            return overTime;
        }catch (Exception e){

        }
        return overTime;
    }

    public static String getDateToStringYMDHMSToYMD(String time) { //yyyy-MM-dd HH:mm:ss 转 yyyy-MM-dd
        String overTime = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = sdf.parse(time);
            overTime = sdf2.format(date);
            return overTime;
        }catch (Exception e){

        }
        return overTime;
    }
    public static String getTimeYMD(String time) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date =  sdf.parse(time);
            return sdf.format(date);
        }catch (Exception e){

        }
        return "";
    }
    //时间戳转yyyy-MM-dd HH:mm
    public static String getTimeYMDHM(Long time) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date date =  new Date(time);
            return sdf.format(date);
        }catch (Exception e){

        }
        return "";
    }
    //时间戳转yyyy-MM-dd HH:mm:ss
    public static String getTimeYMDHMS(Long time) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date =  new Date(time);
            return sdf.format(date);
        }catch (Exception e){

        }
        return "";
    }
    public static String getNewDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    public static String getNewDate2() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        return sdf.format(new Date());
    }
    public static String getNewDateDD() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        return sdf.format(new Date());
    }
    //增加一天
    public static String getDateAddDay(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date =  sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, 1);
            date = calendar.getTime();
            return sdf.format(date);
        }catch (Exception e){

        }
        return "";
    }
    //减少一天
    public static String getDateMinusDay(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date =  sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, -1);
            date = calendar.getTime();
            return sdf.format(date);
        }catch (Exception e){

        }
        return "";
    }
    //是否是本月最后一天
    public static Boolean getDateEndDay(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date =  sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int currentMonth = calendar.get(Calendar.MONTH);

            // 增加1天
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            int nextDayMonth = calendar.get(Calendar.MONTH);

            // 月份变化说明原日期是月末
            return currentMonth != nextDayMonth;
        }catch (Exception e){

        }
        return true;
    }
    //是否是本月第一天
    public static Boolean getDateStartDay(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date =  sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int currentMonth = calendar.get(Calendar.MONTH);

            // 增加1天
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            int nextDayMonth = calendar.get(Calendar.MONTH);

            // 月份变化说明原日期是月末
            return currentMonth != nextDayMonth;
        }catch (Exception e){

        }
        return true;
    }
    //是否大于当天日期
    public static Boolean getDateDayNew(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date =  sdf.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);


            // 增加1天
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long newTime = calendar.getTimeInMillis();

            // 大于当前时间戳
            return newTime > System.currentTimeMillis();
        }catch (Exception e){

        }
        return true;
    }


    /**
     * 判断时间是否小于等于当前时间
     * @param timeA
     * @return
     */
    public static Boolean isSameDay(String timeA) {
        if (TextUtils.isEmpty(timeA)){
            return false;
        }

        try {
            // 定义时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            // 解析时间A
            LocalDateTime dateTimeA = LocalDateTime.parse(timeA, formatter);
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            // 比较时间A是否小于等于当前时间（同一天或过去的日期）
            return !dateTimeA.toLocalDate().isAfter(now.toLocalDate());
        } catch (Exception e) {

            return false;
        }
    }

    /**
     * 判断时间是否小于等于当前时间
     * @param timeA
     * @return
     */
    public static Boolean isToDay(String timeA) {
        if (TextUtils.isEmpty(timeA)){
            return false;
        }

        try {
            // 定义时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            // 解析时间A
            LocalDateTime dateTimeA = LocalDateTime.parse(timeA, formatter);
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            // 比较时间A是否小于等于当前时间（同一天或过去的日期）
            return dateTimeA.toLocalDate().equals(now.toLocalDate());
        } catch (Exception e) {

            return false;
        }
    }

    public static String getNewDateAddDay(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, day);//当前日加上day天
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }

    public static String getTimeYMD(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);

    }

    public static String getTimeYMDHMS(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);

    }

    public static String getTimeYMD(LocalDate date) {//可根据需要自行截取数据显示
       // SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = date.format(formatter);
        return formattedDate;

    }

    public static String getTimeM_D(String dateString) {//可根据需要自行截取数据显示
        // 定义原始日期格式
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 解析日期字符串为LocalDate对象
        LocalDate date = LocalDate.parse(dateString, inputFormatter);
        // 定义目标日期格式
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM-dd");
        // 格式化日期为指定的输出格式
        String formattedDate = date.format(outputFormatter);
        return formattedDate;
    }

    public static String getTimeH_m(String dateTimeStr) {
        try {
            // 定义日期时间格式器
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            // 解析字符串为LocalDateTime对象
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            // 提取时间部分（只包含小时和分钟）
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {

        }
        return "";
    }


    /**
     * 将yyy-MM-dd HH:mm 转成 yyyy-MM-dd HH:mm:ss
     * @param dateTimeStr
     * @return
     */
    public static String getTimeYmdHms(String dateTimeStr) {
        try {
            // 解析输入日期
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date date = inputFormat.parse(dateTimeStr);
            // 格式化为目标日期
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = outputFormat.format(date);
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将yyy-MM-dd HH:mm:ss 转成 yyyy-MM-dd HH:mm
     * @param dateTimeStr
     * @return
     */
    public static String getTimeYmdHm(String dateTimeStr) {
        try {
            // 解析输入日期
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(dateTimeStr);
            // 格式化为目标日期
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String formattedDate = outputFormat.format(date);
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String convertTimeFormat(String isoTime) {
        try {
            // 定义输入格式（ISO 8601格式）
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
            // 定义输出格式
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            // 解析时间字符串
            Date date = inputFormat.parse(isoTime);
            // 格式化为目标格式
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String getTimeHms(String dateTimeStr) {
        // 定义日期时间格式器，包含秒的信息
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            // 解析字符串为 LocalDateTime 对象
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            // 提取时间部分（只包含小时和分钟）
            String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            return timeStr;
        } catch (Exception e) {
            // 处理解析异常
            System.err.println("日期时间解析错误: " + e.getMessage());
            return null;
        }
    }
    /**
     * 比较两个日期字符串是否满足：dateA ≥ dateB
     * 日期格式必须为 yyyy-MM-dd（例如 2025-07-17）
     *
     * @return 当 dateA 晚于或等于 dateB 时返回 true
     * @throws DateTimeException 如果日期格式无效
     */
    public static Boolean  isDateGreaterThanOrEqual(String dateA ,String dateB ) {


        LocalDate localDateA = LocalDate.parse(dateA,  DateTimeFormatter.ISO_LOCAL_DATE );
        LocalDate localDateB = LocalDate.parse(dateB,  DateTimeFormatter.ISO_LOCAL_DATE );

        // 使用 isAfter 或 isEqual 判断，等同于 >=
        return localDateA.isAfter(localDateB) || localDateA.isEqual(localDateB);
    }

    public static Boolean  isDateGreaterThanOrEqual2(String dateA ,String dateB ) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime1 = LocalDateTime.parse(dateA, dateTimeFormatter);
        LocalDateTime dateTime2 = LocalDateTime.parse(dateB, dateTimeFormatter);
        return dateTime1.isAfter(dateTime2);
    }

    /**
     * 对比两个时间相差多少天
     * @param date1
     * @param date2
     * @return
     */
    public static long getAbsoluteDaysBetween(String date1, String date2) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate d1 = LocalDate.parse(date1, formatter);
        LocalDate d2 = LocalDate.parse(date2, formatter);
        return Math.abs(ChronoUnit.DAYS.between(d1, d2));
    }
    public static String getTimeHMS(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(date);
    }

    public static String getTimeYMDHM(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return format.format(date);
    }

    public static String getTimeHM(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(date);
    }

    public static String getTimeD(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("dd");
        return format.format(date);
    }

    public static String getTimeH(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("HH");
        return format.format(date);
    }

    public static String getTimeM(Date date) {//可根据需要自行截取数据显示
        SimpleDateFormat format = new SimpleDateFormat("mm");
        return format.format(date);
    }

    public static String getTimeUTC(Date date) {//可根据需要自行截取数据显示
        return getTimeYMD(date) + "T" + getTimeHMS(date) + "Z";
    }

    //时间戳转换日期
    public static String hisstampToTime(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd  HH:mm:ss");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    public static String hisstampToTime2(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }


    public static String hisstampToYear(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    public static String hisstampToTimeOneSpan(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampToTimenyr(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampToTimenyr2(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampToTimeAct(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampToTimeActDown(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期HH:mm
    public static String hisstampToTimeHM(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期HH:mm:ss
    public static String hisstampToTimeHMS(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampTopicTimeActDown(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampTopicTimeActDown2(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    //时间戳转换日期
    public static String hisstampTopicTimeActDown3(String stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        return sd;
    }

    public static String hisstampTopicTimeActDown2(Long stamp) {
        if (null == stamp) return "";
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd号 HH:mm");
        sd = sdf.format(new Date(stamp)); // 时间戳转换日期
        return sd;
    }

    //日期转时间戳
    public static long hisstampToTimeTopic(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);

        return datea.getTime() / 1000;
    }

    public static long hisstampToTimeTopic2(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        Date datea = simpleDateFormat.parse(stamp);
        return datea.getTime();
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic5(String stamp) throws Exception {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd mm:ss");
            Date datea = simpleDateFormat.parse(stamp);
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
            String sd = "";
            sd = simpleDateFormat1.format(datea);
            return sd;
        } catch (Exception e) {
            return "";
        }
    }

    public static String TimeTopicToTimeTopic4(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy年MM月dd");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    public static String TimeTopicToTimeTopic6(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic2(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy.MM.dd");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic3(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy.MM.dd");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }


    //日期转化为日期
    public static String TimeTopicToTimeTopic7(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy", Locale.ENGLISH);
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic8(String stamp) throws Exception {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date datea = simpleDateFormat.parse(stamp);
            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy/MM/dd");
            String sd = "";
            sd = simpleDateFormat1.format(datea);
            return sd;
        } catch (Exception e) {
            return "";
        }
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic9(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy年MM月dd日");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    //日期转化为日期
    public static String TimeTopicToTimeTopic10(String stamp) throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzzz yyyy", Locale.ENGLISH);
        Date datea = simpleDateFormat.parse(stamp);
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sd = "";
        sd = simpleDateFormat1.format(datea);
        return sd;
    }

    /**
     * 时间戳转换成日期格式字符串
     *
     * @param seconds 精确到秒的字符串
     * @param
     * @return
     */
    public static String timeStamp2Date(String seconds, String format) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }
        if (format == null || format.isEmpty()) {
            format = "yyyy年MM月dd日 HH:mm";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds + "000")));
    }


    /**
     * 判断时间是15天前
     */
    public static boolean currentTimeIs15daysago(String timer) {
        long startTime = Long.parseLong(timer);//获取毫秒数
        long endTime = System.currentTimeMillis();     //获取毫秒数
        //计算秒
        return endTime > startTime;
    }

    //Timestamp转化为String:
    public static String timestampToStr(long dateline) {
        Timestamp timestamp = new Timestamp(dateline * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        return df.format(timestamp);
    }
    public static String timestampToStr3(long dateline) {
        Timestamp timestamp = new Timestamp(dateline );
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        return df.format(timestamp);
    }

    //Timestamp转化为String:
    public static String timestampToStr2(long dateline) {
        Timestamp timestamp = new Timestamp(dateline * 1000);
        SimpleDateFormat df = new SimpleDateFormat("MM-dd");//定义格式，不显示毫秒
        return df.format(timestamp);
    }

    public static Date getDateByString(String time) {
        Date date = null;
        if (time == null)
            return date;
        String date_format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(date_format);
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
    public static Date getDateByString2(String time) {
        Date date = null;
        if (time == null)
            return date;
        String date_format = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(date_format);
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getNowTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        return simpleDateFormat.format(new Date());
    }

    /**
     * 格式化时间转时间戳
     *
     * @param dateStr
     * @return
     */
    public static long formatDateToLong(String dateStr) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        Date date = new Date();
        try {
            date = df.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long l = date.getTime();
        return l;
    }

    /**
     * 格式化时间转时间戳
     *
     * @param dateStr
     * @return
     */
    public static long formatDateToLong2(String dateStr) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd");
        Date date = new Date();
        try {
            date = df.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long l = date.getTime();
        return l;
    }

    /**
     * 加几天
     *
     * @param getDate
     * @param days
     * @return
     */
    public static Date addDays(Date getDate, int days) {
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.setTime(getDate);
        cal.add(Calendar.DAY_OF_MONTH, days);
        date = cal.getTime();
        return date;
    }

    /**
     * 把时间戳转成对应的格式
     *
     * @param dateline     milliseconds
     * @param time_format1 需要转换为什么格式？格式可以参考 "yyyy.MM.dd HH:mm"
     * @return
     */
    public static String timestampToStr(long dateline, String time_format1) {
        Timestamp timestamp = new Timestamp(dateline);
        SimpleDateFormat df = new SimpleDateFormat(time_format1);//定义格式，不显示毫秒
        return df.format(timestamp);
    }

    /**
     * 把string类型的时间转换为需要的格式，不支持转为 data 和 long
     *
     * @param time         需要转换的时间
     * @param time_format1 需要转换的是什么格式？ 格式可以参考 "yyyy.MM.dd HH:mm"
     * @param time_format2 需要转换为什么格式？
     * @return
     */
    public static String formatTimeString(String time, String time_format1, String time_format2) {
        if (TextUtils.isEmpty(time)) {
            return "";
        }
        if (time_format1 == null || time_format2 == null) {
            throw new NullPointerException();
        }
        SimpleDateFormat sdr1 = new SimpleDateFormat(time_format1);
        SimpleDateFormat sdr2 = new SimpleDateFormat(time_format2);
        String format = "";
        try {
            Date date = sdr1.parse(time);
            format = sdr2.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return format;
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public static String getTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND));
        String shur_time = dateFormat.format(calendar.getTime());
        return shur_time;
    }

    /**
     * 获取前后多少天的日期
     *
     * @param days
     * @return
     */
    public static String beforeAfterDate(int days) {
        long nowTime = System.currentTimeMillis();
        long changeTimes = days * 24L * 60 * 60 * 1000;
        return getStrTime(String.valueOf(nowTime + changeTimes), "yyyy-MM-dd");
    }


    public static String getStrTime(String timeStamp, String format) {
        String timeString;
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        long l = Long.valueOf(timeStamp);
        timeString = sdf.format(new Date(l));//单位秒
        return timeString;
    }


    /**
     * 判断两个时间间隔是否大于Month个月,currentDate要大于newDate
     *
     * @param currentDate 一般这里放当前时间
     * @param newDate     这里放需要比较的时间
     * @return true 不在范围内，false 在范围内
     */
    public static boolean isMonth(int month, LocalDate currentDate, LocalDate newDate) {
//        LocalDate currentDate = LocalDate.now();
//        LocalDate newDate = LocalDate.of(2019, 8, 23);
        Period period = Period.between(currentDate, newDate);
        if (period.getYears() <= -1 || period.getMonths() <= -month) {
            return true;
        }
        return false;
    }

    public static boolean isMinute(int minute, String timeStr1, String timeStr2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime time1 = LocalDateTime.parse(timeStr1, formatter);
        LocalDateTime time2 = LocalDateTime.parse(timeStr2, formatter);
        Duration duration = Duration.between(time1, time2);
        long minutes = duration.toMinutes();
        if (minutes>=minute){
            return true;
        }
        return false;
    }


    //时间戳转早中晚
    public static String getTimeMorning_Middle_Evening(String stamp) {
        String sd = "";
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        sd = sdf.format(new Date(Long.parseLong(stamp))); // 时间戳转换日期
        if (Integer.valueOf(sd) >= 5 && Integer.valueOf(sd) < 11) {
            return "早上";
        } else if (Integer.valueOf(sd) >= 11 && Integer.valueOf(sd) < 13) {
            return "中午";
        } else if (Integer.valueOf(sd) >= 13 && Integer.valueOf(sd) < 19) {
            return "下午";
        } else if (Integer.valueOf(sd) >= 19 && Integer.valueOf(sd) < 24) {
            return "晚上";
        } else {
            return "深夜";
        }
    }

    public static String TimeStringToTimeString(String time){
        DateTimeFormatter inputTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        LocalDateTime date = LocalDateTime.parse(time,inputTimeFormatter);
        return date.format(outTimeFormatter);
    }
    //字符串转时间戳
    /**
     * 将时间字符串转换为时间戳（毫秒）
     *
     * @param dateStr 时间字符串，例如 "2025-05-21 08:30:00"
     * @param format  时间格式，例如 "yyyy-MM-dd HH:mm:ss"
     * @return 时间戳（毫秒），如果解析失败则返回 -1
     */
    public static long stringToTimestamp(String dateStr, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        try {
            Date date = sdf.parse(dateStr);
            if (date != null) {
                return date.getTime();
            } else {
                return -1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }
    //时间戳转LocalDate
    public static LocalDate convertTimestampToLocalDate(long timestampMillis) {
        return Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static String getDateString(String dateString) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 设置时区
//         String str="";
//        try {
//            Date date = sdf.parse(dateString);
//            long timestamp = date.getTime();
//            str= String.valueOf(timestamp);
//            System.out.println("时间戳：" + timestamp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return dateString;
    }

    /**
     * 将 Calendar 对象格式化为 "yyyy-MM-dd HH:mm:ss" 字符串
     *
     * @param calendar 输入的 Calendar 对象
     * @return 格式化后的日期时间字符串
     */
    public static String formatCalendar(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }


    private static final SimpleDateFormat YMDHMS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * 获取指定日期 00:00 的格式化时间字符串
     * @param calendar 指定日期的 Calendar 对象
     * @return 格式化后的时间字符串
     */
    public static String getDayStart(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return YMDHMS_FORMAT.format(cal.getTime());
    }

    /**
     * 获取指定日期 23:59:59 的格式化时间字符串
     * @param calendar 指定日期的 Calendar 对象
     * @return 格式化后的时间字符串
     */
    public static String getDayEnd(Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return YMDHMS_FORMAT.format(cal.getTime());
    }


    /**
     * 获取当前月份剩余的所有日期
     * @return 当前月份剩余日期列表
     */
    public static List<LocalDate> getRemainingDaysOfCurrentMonth() {
        LocalDate today = LocalDate.now();
        return getRemainingDaysOfMonth(today);
    }

    /**
     * 获取指定日期所在月份剩余的所有日期
     * @param date 指定日期
     * @return 该月份剩余日期列表
     */
    public static List<LocalDate> getRemainingDaysOfMonth(LocalDate date) {
        List<LocalDate> remainingDays = new ArrayList<>();

        // 获取当月的最后一天
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        // 从今天天开始遍历到月末
        LocalDate currentDate = date;
        while (!currentDate.isAfter(lastDayOfMonth)) {
            remainingDays.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        return remainingDays;
    }

    /**
     * 获取下个月的所有日期
     * @return 下个月的所有日期列表
     */
    public static List<LocalDate> getDaysOfNextMonth() {
        LocalDate today = LocalDate.now();
        return getDaysOfMonth(today.plusMonths(1));
    }

    /**
     * 获取指定日期下一个月的所有日期
     * @param date 指定日期
     * @return 下一个月的所有日期列表
     */
    public static List<LocalDate> getDaysOfNextMonth(LocalDate date) {
        return getDaysOfMonth(date.plusMonths(1));
    }

    /**
     * 获取指定年月的所有日期
     * @param date 指定日期
     * @return 该年月的所有日期列表
     */
    public static List<LocalDate> getDaysOfMonth(LocalDate date) {
        List<LocalDate> days = new ArrayList<>();

        // 获取指定月份的第一天和最后一天
        YearMonth yearMonth = YearMonth.from(date);
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        // 从第一天开始遍历到月末
        LocalDate currentDate = firstDayOfMonth;
        while (!currentDate.isAfter(lastDayOfMonth)) {
            days.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    /**
     * 获取当前日期前后一个月的所有日期数据
     * 例如今天是2025-11-20，获取的数据为2025-10-01到2025-12-31的所有日期
     *
     * @return 包含前后一个月所有日期的列表
     */
    public static List<LocalDate> getMonthRangeWithPreviousAndNextAsList() {
        List<LocalDate> dateList = new ArrayList<>();

        LocalDate today = LocalDate.now();

        // 计算起始日期：上个月的第一天
        LocalDate startDate = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());

        // 计算结束日期：下个月的最后一天
        LocalDate endDate = today.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        // 从开始日期遍历到结束日期，将所有日期添加到列表中
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dateList.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        return dateList;
    }

    public static String getNowTimeYYYYMMDD() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(new Date());
    }



    /**
     * 判断时间是否小于等于当前时间
     * @param timeA
     * @return
     */
    public static Boolean isSameDayYYYYMMDD(String timeA) {
        if (TextUtils.isEmpty(timeA)) {
            return false;
        }

        try {
            // 定义时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 解析日期时间A
            LocalDate dateA = LocalDate.parse(timeA, formatter);
            // 获取当前日期
            LocalDate today = LocalDate.now();
            // 比较日期A是否小于等于当前日期（同一天或过去的日期）
            return !dateA.isAfter(today);
        } catch (Exception e) {
            // 可以考虑记录日志
            // Log.e("DateUtils", "Parse date failed: " + timeA, e);
            return false;
        }
    }

    /**
     * 获取当前月份
     * @return
     */
    public static String getMonthValue(){
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        // 格式化当前日期
        return currentDate.format(formatter);
    }

    /**
     * 获取当前月份减一个月
     * @return
     */
    public static String getMinusMonthValue(){
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        // 获取上一个月的日期
        LocalDate previousMonthDate = currentDate.minusMonths(1);
        // 格式化上一个月的日期并打印
        return previousMonthDate.format(formatter);
    }
}
