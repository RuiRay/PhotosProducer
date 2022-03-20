package task;

import utils.FileUtil;

import java.io.File;
import java.util.*;

/**
 * 重复文件清理
 */
public class DuplicateFileCleanTask {

    private int scanTotalFileCount = 0;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("DuplicateFileCleanTask 重复文件清理 请输入清理目录");
            return;
        }

        String inputDir = args[0];
        String reportPath = null;
        if (args.length > 1) {
            reportPath = args[1];
        }
        System.out.println("java: input dir: " + inputDir + (reportPath == null ? "" : ("report path: " + reportPath)));


        DuplicateFileCleanTask task = new DuplicateFileCleanTask();
        List<DuplicateFile> fileList = task.findDuplicateFile(inputDir);
        if (fileList.isEmpty()) {
            System.out.println("未找到重复文件！");
            return;
        }
        task.saveReport(fileList, reportPath);
        task.cleanDuplicateFile(fileList);
    }

    public void cleanDuplicateFile(List<DuplicateFile> duplicateFileList) {
        final List<File> deleteFile = new ArrayList<>();

        // 删除策略（这里保留第一个文件）
        for (DuplicateFile duplicateFile : duplicateFileList) {
            for (int i = 1; i < duplicateFile.fileList.size(); i++) {
                deleteFile.add(duplicateFile.fileList.get(i));
            }
        }

        // 打印删除提示
        long totalFileSize = 0;
        System.out.println("\n\n========================");
        for (File file : deleteFile) {
            System.out.println(String.format("Delete: %s, %s", file.getAbsolutePath(), FileUtil.convertFileSize(file.length())));
            totalFileSize += file.length();
        }
        System.out.println(String.format("上列 %d 个重复文件，共占用存储 %s，输入 del 进行删除，输入其它退出程序：", deleteFile.size(), FileUtil.convertFileSize(totalFileSize)));
        String inputText = new Scanner(System.in).next().trim().toLowerCase();
        if (!inputText.equals("del")) {
            System.out.println("已退出程序！");
            System.exit(1);
            return;
        }

        // 执行删除
        int deleteSuccessCount = 0;
        int deleteSuccessFileSize = 0;
        for (int i = 0; i < deleteFile.size(); i++) {
            File file = deleteFile.get(i);
            long fileSize = file.length();
            boolean success = file.delete();
            if (success) {
                deleteSuccessCount++;
                deleteSuccessFileSize += fileSize;
            }
            System.out.println(String.format("Delete Progress: %d/%d, file=%s, %s", i, deleteFile.size(), file.getAbsolutePath(), success ? "Success" : "Failure"));
        }

        System.out.println(String.format("共 %d 个文件，已成功删除 %d 个，节省存储空间 %s", deleteFile.size(), deleteSuccessCount, FileUtil.convertFileSize(deleteSuccessFileSize)));
    }

    public List<DuplicateFile> findDuplicateFile(String... inputDir) {
        Map<Long, List<File>> sameSizeFileMap = new HashMap<>();
        for (String rootPath : inputDir) {
            findSameSizeFile(sameSizeFileMap, new File(rootPath));
        }

        int sameSizeFileCount = 0;
        for (Long keySize : sameSizeFileMap.keySet()) {
            List<File> fileList = sameSizeFileMap.get(keySize);
            if (fileList.size() < 2) {
                continue;
            }
            sameSizeFileCount ++;
        }

        System.out.println(String.format("findDuplicateFile() same count: %d, total count: %d ", sameSizeFileCount, scanTotalFileCount));

        List<DuplicateFile> duplicateFileList = new ArrayList<>();
        int keySizeIndex = 0;
        for (Long keySize : sameSizeFileMap.keySet()) {
            List<File> fileList = sameSizeFileMap.get(keySize);
            if (fileList.size() < 2) {
                continue;
            }
            keySizeIndex++;

            // 优先使用 文件大小+文件名 相等，来判读文件相同
            String lastFileName = fileList.get(0).getName();
            boolean allFileNameEqual = lastFileName.length() > 10;
            for (int i = 1; i < fileList.size(); i++) {
                allFileNameEqual &= fileList.get(i).getName().equals(lastFileName);
            }
            if (allFileNameEqual) {
                duplicateFileList.add(new DuplicateFile(DuplicateType.NAME, lastFileName, keySize, fileList));
                continue;
            }

            // 计算 文件大小 相同的文件，各自的 MD5 值，归类到 Map 中
            Map<String, List<File>> sameMD5Map = new HashMap<>();
            for (File file : fileList) {
                String fileMD5 = FileUtil.getFileMD5(file);
                List<File> sameMD5List = sameMD5Map.get(fileMD5);
                if (sameMD5List == null) {
                    sameMD5List = new ArrayList<>();
                    sameMD5Map.put(fileMD5, sameMD5List);
                }
                sameMD5List.add(file);
                System.out.println(String.format("TEMP: md5=%s, size=%s, file=%s", fileMD5, FileUtil.convertFileSize(file.length()), file.getAbsolutePath()));
            }
            for (String md5 : sameMD5Map.keySet()) {
                List<File> md5List = sameMD5Map.get(md5);
                if (md5List.size() < 2) {
                    continue;
                }
                duplicateFileList.add(new DuplicateFile(DuplicateType.MD5, md5, keySize, md5List));
            }

            System.out.println(String.format("Progress: (%d/%d) same size: %s", keySizeIndex, sameSizeFileCount, fileList));
        }

        return duplicateFileList;
    }

    private void findSameSizeFile(Map<Long, List<File>> resultMap, File baseDir) {
        File[] listFiles = baseDir.listFiles();
        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isFile()) {
                List<File> fileList = resultMap.get(file.length());
                if (fileList == null) {
                    fileList = new ArrayList<>();
                    resultMap.put(file.length(), fileList);
                }
                fileList.add(file);
                scanTotalFileCount++;
                if (scanTotalFileCount % 100 == 1) {
                    System.out.println("Progress: find file count: " + scanTotalFileCount);
                }
            } else if (file.isDirectory()) {
                findSameSizeFile(resultMap, file);
            }
        }
    }

    public void saveReport(List<DuplicateFile> duplicateFileList, String reportPath) {
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return;
        }
        StringBuilder sameMD5Result = new StringBuilder();
        final String SPLIT = "######";
        for (DuplicateFile duplicateFile : duplicateFileList) {
            sameMD5Result.append(duplicateFile.getDuplicateType()).append(SPLIT);
            sameMD5Result.append(duplicateFile.key).append(SPLIT);
            sameMD5Result.append(duplicateFile.fileSize).append(SPLIT);
            List<File> fileList = duplicateFile.fileList;
            int iMax = fileList.size() - 1;
            for (int i = 0; ; i++) {
                File file = fileList.get(i);
                sameMD5Result.append(file.getAbsolutePath());
                if (i == iMax)
                    break;
                sameMD5Result.append(SPLIT);
            }
            sameMD5Result.append("\n");
        }
        FileUtil.writeFile(new File(reportPath), sameMD5Result.toString());
    }

    enum DuplicateType {
        MD5,    // 依据 文件大小 + MD5 判定文件相同
        NAME    // 依据 文件大小 + 文件名 判定文件相同
    }

    public class DuplicateFile {
        private final DuplicateType duplicateType;
        private final String key;
        private final long fileSize;
        private final List<File> fileList;

        public DuplicateFile(DuplicateType duplicateType, String key, long fileSize, List<File> fileList) {
            this.duplicateType = duplicateType;
            this.key = key;
            this.fileSize = fileSize;
            this.fileList = fileList;
        }

        public String getDuplicateType() {
            if (duplicateType == DuplicateType.MD5) {
                return "md5";
            } else if (duplicateType == DuplicateType.NAME) {
                return "name";
            }
            return "";
        }
    }

}
