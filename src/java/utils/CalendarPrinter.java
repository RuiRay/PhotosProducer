package utils;

import java.util.Scanner;

public class CalendarPrinter {

    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);
        System.out.print("请输入年份（大于1800年）：");
        int year = in.nextInt();
        System.out.print("请输入月份（1-12月）：");
        int month = in.nextInt();
        in.close();

        printDate(year, month);
    }

    /**
     * 打印某年某月的日历
     *
     * @param year  年
     * @param month 月
     */
    public static void printDate(final int year, final int month) {

        System.out.println("==================================================");
        System.out.println("日\t一\t二\t三\t四\t五\t六");

        // 计算某年某月1日是星期几
        final int startWeekday = calcWeekday(year, month);
        // 获取某年某月的天数
        final int numberOfDaysInMonth = getNumberOfDaysInMonth(year, month);

        // 打印偏移
        for (int i = 0; i < startWeekday; i++) {
            System.out.print(" \t");
        }

        // 打印日期
        for (int i = 1; i <= numberOfDaysInMonth; i++) {
            System.out.print(i + "\t");

            // 一行显示7列，当日期+偏移是7的倍数时换行
            if ((i + startWeekday) % 7 == 0) {
                System.out.println();
            }
        }
        System.out.println("\n==================================================");
    }

    /**
     * 计算某年某月1日，是星期几
     *
     * @param year  年，不能小于 1800 年
     * @param month 月
     * @return 星期几（0-6，周日、周一~周六）
     */
    public static int calcWeekday(int year, int month) {
        final int START_YEAR = 1800;
        final int START_DAY_FOR_JAN_1_1800 = 3;

        // 从 1800年 算起的总天数
        long totalNumberOfDays = calcTotalNumberOfDays(START_YEAR, year, month);

        // （总天数 + 起始星期偏移）% 7 = 总天数的星期偏移
        return (int) ((totalNumberOfDays + START_DAY_FOR_JAN_1_1800) % 7);
    }

    /**
     * 传入数值 @calcWeekday(year, month) 方法
     *
     * @param weekday 值
     * @return 星期
     */
    public static String getWeekdayText(int weekday) {
        return new String[]{"周日", "周一", "周二", "周三", "周四", "周五", "周六"}[weekday];
    }

    /**
     * 计算从 起始年1月1日 到 某年某月 的天数
     *
     * @param startYear 起始年
     * @param year      年
     * @param month     月
     * @return 天数
     */
    public static long calcTotalNumberOfDays(int startYear, int year, int month) {
        long total = 0;

        // 计算从 起始年的1月1日 - 当前年1月1日 的天数
        for (int i = startYear; i < year; i++) {
            if (isLeapYear(i))
                total = total + 366;
            else
                total = total + 365;
        }

        // 计算从 1月份 - 当前月 的天数
        for (int i = 0; i < month; i++)
            total += getNumberOfDaysInMonth(year, i);

        return total;
    }

    /**
     * 返回某年某月的天数
     *
     * @param year  年
     * @param month 月
     * @return 天（28，29，30，31）
     */
    public static int getNumberOfDaysInMonth(int year, int month) {
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
            return 31;
        if (month == 4 || month == 6 || month == 9 || month == 11)
            return 30;
        if (month == 2)
            return isLeapYear(year) ? 29 : 28;
        return 0;
    }

    /**
     * 判断是否为闰年
     *
     * @param year 年，如 2000
     * @return true 表示是闰年
     */
    public static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0);
    }
}
