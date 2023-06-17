package task;

import utils.CmdUtil;
import utils.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class SyncPhotosTask {

    private static final long SIZE_4_GB = 4L * 1024 * 1024 * 1024;

    // 识别的文件格式，仅将手机中如下格式的资源同步到本地磁盘
    private final Set<String> SUPPORT_MEDIA_TYPE = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif",
            "mp4", "m4a", "mov", "m4v"
    ));

    /**
     * param 1: export disk dir     导出到本地磁盘的目录
     * param 2: export file log     导出文件名日志，避免下次重复导出
     * param 3: photos directory    手机中的扩展目录1
     * param 4: photos directory    手机中的扩展目录2
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("SyncPhotosTask 同步手机照片 参数不能低于2个！");
            return;
        }

        String[] phonePhotosDirs = new String[]{
                "/sdcard/DCIM/Camera",
                "/sdcard/DCIM/Screenshots",
                "/sdcard/tencent/MicroMsg/WeiXin",
                "/sdcard/Picture/知乎",
                "/sdcard/Tencent/QQ_Images",
                "/sdcard/DJI/dji.mimo/Camera",
        };
        String exportDiskDir = args[0];
        String exportLogFilePath = args[1];

        // 将第3个以上参数添设置为要访问的手机目录，并消除重复的路径
        if (args.length > 2) {
            Set<String> dirSet = new HashSet<>();
            dirSet.addAll(Arrays.asList(phonePhotosDirs));
            dirSet.addAll(Arrays.asList(args).subList(2, args.length));

            phonePhotosDirs = new String[dirSet.size()];
            phonePhotosDirs = dirSet.toArray(phonePhotosDirs);
        }

        System.out.println(String.format("SyncPhotosTask start %s, %s, %s", exportDiskDir, exportLogFilePath, Arrays.toString(phonePhotosDirs)));

        startTask(phonePhotosDirs, exportDiskDir, exportLogFilePath);
    }

    private static void startTask(String[] phonePhotosDirs, String exportDiskDir, String exportLogFilePath) {
        final SyncPhotosTask syncPhotosTask = new SyncPhotosTask();
        File exportFileDir = new File(exportDiskDir);
        if (!exportFileDir.exists() && !exportFileDir.mkdir()) {
            System.out.println("照片输出目录无法访问！");
            return;
        }

        // 从手机目录中扫描照片和视频
        List<MediaFile> phoneMediaFileList = syncPhotosTask.findMediaFile(phonePhotosDirs);
        if (phoneMediaFileList.isEmpty()) {
            System.out.println("目录下找到的照片是空！");
            return;
        }

        // 过滤已经同步到本地的资源，避免重复拉取
        List<MediaFile> prepareExportMediaFileList = syncPhotosTask.filterPulledFile(phoneMediaFileList, exportLogFilePath);
        if (prepareExportMediaFileList.isEmpty()) {
            System.out.println(String.format("无需要同步的照片，总照片数：%d", phoneMediaFileList.size()));
            return;
        }

        // 统计打印
        long totalFileSize = getTotalFileSize(phoneMediaFileList);
        long prepareFileSize = getTotalFileSize(prepareExportMediaFileList);
        System.out.println("================================================");
        System.out.println(String.format("=== 照片数：（%d/%d），文件大小：（%s/%s）",
                phoneMediaFileList.size() - prepareExportMediaFileList.size(), phoneMediaFileList.size(),
                FileUtil.convertFileSize(totalFileSize - prepareFileSize), FileUtil.convertFileSize(totalFileSize)));
        System.out.println("================================================");

        // 从手机上同步到本地磁盘
        syncPhotosTask.pullMediaFile(exportFileDir, phonePhotosDirs, exportLogFilePath, prepareExportMediaFileList);
    }

    private List<MediaFile> findMediaFile(String[] phonePhotosDirs) {
        List<MediaFile> mediaFileList = new ArrayList<>();
        for (String photosDir : phonePhotosDirs) {
            findMediaFile(mediaFileList, photosDir);
        }
        return mediaFileList;
    }

    private void findMediaFile(List<MediaFile> mediaFileList, String photosDir) {
        String lsResult = CmdUtil.execCmd(null, true, "adb", "shell", "ls", "-al", photosDir);
        String[] lsArray = lsResult.split("\n");
        for (String item : lsArray) {
            MediaFile mediaFile = parseLsCmdItem(photosDir, item);
            if (mediaFile == null) {
                continue;
            }
            mediaFileList.add(mediaFile);
        }
    }

    // -rw-rw----  1 root sdcard_rw   10393596 2021-02-02 14:43 IMG_20210202_144346.jpg
    private MediaFile parseLsCmdItem(String photosDir, String lsCmdItem) {
        final int columnCount = 8;
        String[] columnArray = new String[columnCount];
        int columnIndex = 0;
        int splitIndex = 0;
        while (splitIndex != -1 && columnIndex + 1 < columnCount) {
            int index = lsCmdItem.indexOf(" ", splitIndex + 1);
            if (index != -1 && splitIndex + 1 != index) {
                columnArray[columnIndex++] = lsCmdItem.substring(splitIndex + 1, index);
            }
            splitIndex = index;
        }
        if (columnIndex + 2 < columnCount) {
            System.out.println("Unknown item: " + lsCmdItem);
            return null;
        }
        columnArray[columnIndex] = lsCmdItem.substring(splitIndex + 1);
        long fileSize = Long.parseLong(columnArray[4]);
        String fileName = columnArray[7];

        if (!isSupportMediaType(fileName)) {
            return null;
        }
        MediaFile mediaFile = new MediaFile();
        mediaFile.filePath = photosDir + "/" + fileName;
        mediaFile.name = fileName;
        mediaFile.fileSize = fileSize;
        return mediaFile;
    }

    private List<MediaFile> filterPulledFile(List<MediaFile> mediaFileList, String... exportLogs) {
        final Set<String> fileNameSet = new HashSet<>();
        for (String exportLog : exportLogs) {
            FileUtil.readFile(new File(exportLog), new FileUtil.ReaderCallback() {
                @Override
                public boolean onReadLine(String line) {
                    fileNameSet.add(line.trim());
                    return true;
                }
            });
        }
        System.out.println("=== filterPulledFile() " + fileNameSet.size());
        if (fileNameSet.isEmpty()) {
            return mediaFileList;
        }
        List<MediaFile> resultList = new ArrayList<>();
        for (MediaFile mediaFile : mediaFileList) {
            if (fileNameSet.contains(mediaFile.name)) {
                continue;
            }
            resultList.add(mediaFile);
        }
        return resultList;
    }

    private void pullMediaFile(File exportDir, String[] phonePhotosDirs, String logFilePath, List<MediaFile> mediaFileList) {
        StringBuilder logSB = new StringBuilder();
        logSB.append(FileUtil.readFile(logFilePath)).append("\n");
        logSB.append(createExportHeadInfo(exportDir, phonePhotosDirs, logFilePath));
        File logFile = new File(logFilePath);
        final long totalFileSize = getTotalFileSize(mediaFileList);
        long pullFileSize = 0;

        for (int i = 0; i < mediaFileList.size(); i++) {
            MediaFile mediaFile = mediaFileList.get(i);
            if (mediaFile.fileSize > SIZE_4_GB) {
                System.err.println("=== 文件超过4GB，FAT32格式的U盘不支持！" + mediaFile.filePath);
                continue;
            }
            System.out.println(String.format("=== pullMediaFile() start %d/%d, size=%s, pull: %s/%s, name=%s",
                    i, mediaFileList.size(),
                    FileUtil.convertFileSize(mediaFile.fileSize),
                    FileUtil.convertFileSize(pullFileSize),
                    FileUtil.convertFileSize(totalFileSize),
                    mediaFile.name));
            CmdUtil.execCmd(null, false, "adb", "pull", mediaFile.filePath, new File(exportDir, mediaFile.name).getAbsolutePath());
            logSB.append(mediaFile.name).append("\n");
            pullFileSize += mediaFile.fileSize;

            FileUtil.writeFile(logFile, logSB.toString());
        }
    }

    private String createExportHeadInfo(File exportDir, String[] phonePhotosDirs, String logFilePath) {
        StringBuilder result = new StringBuilder();
        result.append("\n\n\n================================================\n");
        result.append(String.format("=== 导出目录：%s \n", exportDir.getAbsolutePath()));
        result.append(String.format("=== 日志文件：%s \n", logFilePath));
        result.append(String.format("=== 传输时间：%s \n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())));
        for (String phonePhotosDir : phonePhotosDirs) {
            result.append(String.format("=== 原始目录：%s \n", phonePhotosDir));
        }
        result.append("================================================\n");
        return result.toString();
    }

    private boolean isSupportMediaType(String fileName) {
        int suffixIndex = fileName.lastIndexOf(".");
        if (suffixIndex == -1) {
            return false;
        }
        String suffix = fileName.substring(suffixIndex + 1).toLowerCase();
        return SUPPORT_MEDIA_TYPE.contains(suffix);
    }

    private static long getTotalFileSize(List<MediaFile> phoneMediaFileList) {
        long totalFileSize = 0;
        for (MediaFile mediaFile : phoneMediaFileList) {
            totalFileSize += mediaFile.fileSize;
        }
        return totalFileSize;
    }

    public class MediaFile {

        public String name;
        public String filePath;
        public long fileSize;
    }
}
