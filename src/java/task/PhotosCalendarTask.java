package task;

import entity.Photo;
import entity.SummaryInfo;
import parser.*;
import task.calendar.PhotosCalendar;
import task.calendar.PhotosHtml;
import utils.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class PhotosCalendarTask {

    private static final boolean USE_CACHE = Boolean.parseBoolean(System.getProperty("useCache", "true"));

    static {
        println("PhotosCalendar property USE_CACHE " + USE_CACHE);
    }

    /**
     * 「程序入口」分析照片的拍摄时间，生成日历主线的相册
     * @param args  指定的照片目录
     *              传1个参数：为照片目录，生成的 html 在该目录下；
     *              传2个以上参数：参数1为 html 路径，参数2开始都为照片目录；
     */
    public static void main(String[] args) {
        // 照片目录，支持多个
        String[] inputPhotoDirs;
        // 生成的Html文件路径，与照片目录相同时JavaScript中的引用为相对路径
        String outputHtmlPath;

        // 解析args，支持命令行传入的值
        if (args.length == 0) {
            inputPhotoDirs = new String[]{"./example/"};
            outputHtmlPath = "./example/photosCalendar.html";
            println("PhotosCalendarTask 生成照片日历 参数为空，生成示例的HTML");
        } else if (args.length == 1) {
            inputPhotoDirs = args;
            String today = new SimpleDateFormat("yyyyMMdd").format(System.currentTimeMillis());
            outputHtmlPath = new File(args[0], String.format("photosCalendar_%s.html", today)).getAbsolutePath();
        } else {
            outputHtmlPath = args[0];
            if (!new File(outputHtmlPath).getName().contains(".")) {
                println("java: param 1 must be file, such as index.html");
                return;
            }
            inputPhotoDirs = new String[args.length - 1];
            System.arraycopy(args, 1, inputPhotoDirs, 0, inputPhotoDirs.length);
        }
        println("java: input " + outputHtmlPath + ", " + Arrays.toString(inputPhotoDirs));

        startTask(inputPhotoDirs, outputHtmlPath);
    }

    private static void startTask(String[] inputPhotoDirs, String outputHtmlPath) {
        // 根据照片日期生成HTML日历
        PhotosCalendar photosCalendar = new PhotosCalendar(FileDateParser.Provider.getParser());
        String relativeDir = new File(outputHtmlPath).getParent();
        File cacheFile = !USE_CACHE ? null : new File(relativeDir, "photosCalendar.cache");

        List<Photo> allPhotoList = photosCalendar.findPhotos(inputPhotoDirs, relativeDir, cacheFile);
        if (allPhotoList.isEmpty()) {
            println("The photos in the directory are empty! " + Arrays.toString(inputPhotoDirs));
            return;
        }

        SummaryInfo summaryInfo = photosCalendar.computeSummaryInfo(allPhotoList);
        PhotosHtml photosHtml = new PhotosHtml();
        photosHtml.setSummaryInfo(summaryInfo);

        FileUtil.writeFile(new File(outputHtmlPath), photosHtml.toString());

        println("java: html output done " + outputHtmlPath);
    }

    private static void println(String message) {
        System.out.println(message);
    }

}
