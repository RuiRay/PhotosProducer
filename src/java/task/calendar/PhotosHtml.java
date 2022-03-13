package task.calendar;

import entity.Photo;
import entity.SummaryInfo;
import utils.CalendarPrinter;
import utils.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 输出Html内容
 */
public class PhotosHtml {

    // 相对目录：System.getProperty("user.dir")
    private static final String ASSETS_TEMPLATE_INDEX_HTML = "./src/assets/template/index.html";
    private static final String ASSETS_TEMPLATE_PATCH_CALENDAR = "./src/assets/template/patch_calendar.html";
    private static final String ASSETS_TEMPLATE_PATCH_HEAD_YEAR = "./src/assets/template/patch_head_year.html";
    private static final int LESS_PHOTO_COUNT = 10;
    private static final int LESS_PHOTO_DAY_COUNT = 3;

    private SummaryInfo summaryInfo;

    public void setSummaryInfo(SummaryInfo summaryInfo) {
        this.summaryInfo = summaryInfo;
    }

    // 转成 JavaScript 代码，定义日期与照片的映射参数，如：photos20220101: ["xxx.jpg", "bbb.jpg", "ccc.jpg"]
    private String convertPhotosMapJS() {
        StringBuilder result = new StringBuilder();
        for (Integer year : summaryInfo.yyyyMMddPhotosMap.keySet()) {
            for (Integer month : summaryInfo.yyyyMMddPhotosMap.get(year).keySet()) {
                for (Integer day : summaryInfo.yyyyMMddPhotosMap.get(year).get(month).keySet()) {
                    List<Photo> photoList = summaryInfo.yyyyMMddPhotosMap.get(year).get(month).get(day);

                    if (result.length() > 0) {
                        result.append(",\n\t\t\t");
                    }
                    String yyyyMMdd = String.format("%04d%02d%02d", year, month, day);
                    result.append("photos").append(yyyyMMdd).append(": ");

                    Collections.sort(photoList, new Comparator<Photo>() {
                        @Override
                        public int compare(Photo o1, Photo o2) {
                            return o1.name.compareTo(o2.name);
                        }
                    });
                    int iMax = photoList.size() - 1;
                    if (iMax == -1)
                        result.append("[]");
                    result.append('[');
                    for (int i = 0; ; i++) {
                        result.append("\"").append(String.valueOf(photoList.get(i).filePath)).append("\"");
                        if (i == iMax) {
                            result.append(']').toString();
                            break;
                        }
                        result.append(", ");
                    }
                }
            }
        }
        return result.toString();
    }

    private String convertSummaryText() {
        final StringBuilder result = new StringBuilder();

        result.append("<h3 class=\"card-header\">总览</h3>\n");
        result.append(String.format("\t\t<h5>照片总数：%d张【%s】</h5>\n",
                summaryInfo.totalPhotosCount, FileUtil.convertFileSize(summaryInfo.totalPhotosSize)));
        result.append(String.format("\t\t<h5>起止日期：%d年%d月 - %d年%d月</h5>\n",
                summaryInfo.startYear, summaryInfo.startMonth,
                summaryInfo.endYear, summaryInfo.endMonth));
        result.append(String.format("\t\t<h5>文档创建日期：%s</h5>\n",
                new SimpleDateFormat("yyyy-MM-dd HH:mm").format(System.currentTimeMillis())));

        int lastYear = 0;
        Map<Integer, String> emptyPhotoMap = new TreeMap<>();
        Map<Integer, String> lessPhotoMap = new TreeMap<>();
        StringBuilder dateDirText = new StringBuilder();
        dateDirText.append("\t\t<br/><br/><h3 class=\"card-header\">日期目录</h3>\n");
        for (int year = summaryInfo.startYear; year <= summaryInfo.endYear; year++) {
            final int startMonth = (year == summaryInfo.startYear) ? summaryInfo.startMonth : 1;
            final int endMonth = (year == summaryInfo.endYear) ? summaryInfo.endMonth : 12;
            for (int month = startMonth; month <= endMonth; month++) {
                int photoCount = summaryInfo.getMonthPhotoCount(year, month);
                if (photoCount == 0) {
                    String content = emptyPhotoMap.get(year);
                    emptyPhotoMap.put(year, content == null ? String.format("%d月", month) : String.format("%s, %d月", content, month));
                    continue;
                }
                if (photoCount < LESS_PHOTO_COUNT || summaryInfo.getMonthPhotoDayCount(year, month) < LESS_PHOTO_DAY_COUNT) {
                    String content = lessPhotoMap.get(year);
                    lessPhotoMap.put(year, content == null ? String.format("%d月", month) : String.format("%s, %d月", content, month));
                }

                long fileSize = summaryInfo.getYearFileSize(year);
                if (lastYear != year) {
                    lastYear = year;
                    dateDirText.append(String.format("\t\t<h5 class=\"card-header\"><a href=\"#idYear%04d\">%04d年【%s】</a></h5>\n",
                            year, year, FileUtil.convertFileSize(fileSize)));
                }
                dateDirText.append(String.format("\t\t<li><a href=\"#idMonth%04d-%02d\">%04d-%02d（%d张）</a></li>\n",
                        year, month, year, month, photoCount));
            }
        }

        result.append("\t\t<h5 class=\"card-header\">无照片的月份</h5>\n");
        for (Integer year : emptyPhotoMap.keySet()) {
            result.append(String.format("\t\t<li><a href=\"#idYear" + year + "\">%d年：%s</a></li>\n", year, emptyPhotoMap.get(year)));
        }

        result.append("\t\t<h5 class=\"card-header\">照片较少的月份</h5>\n");
        for (Integer year : lessPhotoMap.keySet()) {
            result.append(String.format("\t\t<li><a href=\"#idYear" + year + "\">%d年：%s</a></li>\n", year, lessPhotoMap.get(year)));
        }

        result.append(dateDirText);
        result.append("\t\t<br/><br/>");
        return result.toString();
    }

    private String convertCalendar() {
        StringBuilder bodyHtml = new StringBuilder();
        int lastYear = 0;

        int unrecognizedPhotoCount = summaryInfo.getDayPhotoCount(0, 0, 0);
        if (unrecognizedPhotoCount > 0) {
            bodyHtml.append(createUnrecognizedHTMLHead(String.format("未识别到年份的照片（%d张）", unrecognizedPhotoCount),
                    String.format("%04d%02d%02d", 0, 0, 0)));
        }

        for (int year = summaryInfo.startYear; year <= summaryInfo.endYear; year++) {
            final int startMonth = (year == summaryInfo.startYear) ? summaryInfo.startMonth : 1;
            final int endMonth = (year == summaryInfo.endYear) ? summaryInfo.endMonth : 12;
            int unrecognizedYearPhotoCount = summaryInfo.getMonthPhotoCount(year, 0);

            for (int month = startMonth; month <= endMonth; month++) {
                int monthPhotoCount = summaryInfo.getMonthPhotoCount(year, month);
                if (monthPhotoCount == 0)
                    continue;

                if (lastYear != year) {
                    lastYear = year;
                    bodyHtml.append(createHTMLHeadYear(year, summaryInfo.getYearPhotoCount(year), unrecognizedYearPhotoCount));
                }

                int unrecognizedMonthPhotoCount = summaryInfo.getDayPhotoCount(year, month, 0);
                bodyHtml.append(createHTMLTable(year, month, monthPhotoCount, unrecognizedMonthPhotoCount));
            }
        }

        return bodyHtml.toString();
    }

    private String createUnrecognizedHTMLHead(String title, String linkKey) {
        String html = "<h3 class=\"button-unrecognized\" onclick=\"previewPhotos('%s')\">%s</h3>\n";
        return String.format(html, linkKey, title);
    }

    private String createHTMLHeadYear(int year, int totalCount, int unrecognizedCount) {
        String html = FileUtil.readFile(ASSETS_TEMPLATE_PATCH_HEAD_YEAR);
        html = html.replace("${year}", String.format("%04d", year));
        html = html.replace("${totalCount}", String.valueOf(totalCount));
        html = html.replace("${yyyyMMdd}", String.format("%04d%02d%02d", year, 0, 0));
        html = html.replace("${unrecognizedCount}", String.valueOf(unrecognizedCount));
        html = html.replace("${button_visibility}", unrecognizedCount > 0 ? "visible" : "hidden");
        return html;
    }

    private String createHTMLTable(int year, int month, int monthTotalCount, int unrecognizedCount) {
        final StringBuilder rowHtml = new StringBuilder();
        // 计算某年某月1日是星期几
        final int startWeekday = CalendarPrinter.calcWeekday(year, month);
        // 获取某年某月的天数
        final int numberOfDaysInMonth = CalendarPrinter.getNumberOfDaysInMonth(year, month);

        rowHtml.append("<tr>\n");
        // 打印偏移
        for (int i = 0; i < startWeekday; i++) {
            rowHtml.append("\t\t\t<td></td>\n");
        }

        // 打印日期
        for (int day = 1; day <= numberOfDaysInMonth; day++) {
            String classStyle = "";
            int photoCount = summaryInfo.getDayPhotoCount(year, month, day);
            if (photoCount > 10) {
                classStyle = " class=\"cell-day count10\"";
            } else if (photoCount > 5) {
                classStyle = " class=\"cell-day count5\"";
            } else if (photoCount > 0) {
                classStyle = " class=\"cell-day count1\"";
            }
            if (photoCount > 0) {
                String date = String.format("%04d%02d%02d", year, month, day);
                classStyle += " onclick=\"previewPhotos('" + date + "')\"";
            }
            rowHtml.append(String.format("\t\t\t<td%s>%d</td>\n", classStyle, day));

            // 一行显示7列，当日期+偏移是7的倍数时换行
            if ((day + startWeekday) % 7 == 0) {
                rowHtml.append("\t\t\t</tr>\n");
                rowHtml.append("\t\t\t<tr>\n");
            }
        }
        rowHtml.append("\t\t\t</tr>");

        final String yyyyMM = String.format("%04d-%02d", year, month);
        String tableHtml = FileUtil.readFile(ASSETS_TEMPLATE_PATCH_CALENDAR);
        tableHtml = tableHtml.replace("${calendar_month_id}", "idMonth" + yyyyMM);
        tableHtml = tableHtml.replace("${calendar_month_title}", String.format("%d-%02d (%d)", year, month, monthTotalCount));
        tableHtml = tableHtml.replace("${yyyyMMdd}", String.format("%04d%02d%02d", year, month, 0));
        tableHtml = tableHtml.replace("${unrecognizedCount}", String.valueOf(unrecognizedCount));
        tableHtml = tableHtml.replace("${button_visibility}", unrecognizedCount > 0 ? "visible" : "hidden");
        return tableHtml.replace("${calendar_month_body}", rowHtml.toString());
    }

    @Override
    public String toString() {
        String photosMapJS = convertPhotosMapJS();
        String summaryText = convertSummaryText();
        String calendarText = convertCalendar();

        File indexHtmlFile = new File(ASSETS_TEMPLATE_INDEX_HTML);
        if (!indexHtmlFile.exists()) {
            System.err.println("PhotosHtml toString() index html file not exist: " + indexHtmlFile.getAbsolutePath());
            return "";
        }
        String html = FileUtil.readFile(indexHtmlFile);
        html = html.replace("${photosMapJS}", photosMapJS);
        html = html.replace("${summaryHTML}", summaryText);
        html = html.replace("${calendarHTML}", calendarText);
        return html;
    }
}
