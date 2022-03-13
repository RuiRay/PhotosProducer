package task;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import entity.Photo;
import parser.FileDateParser;
import task.calendar.PhotosCalendar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 使用照片的拍摄日期命名
 * 依赖解析库：https://github.com/drewnoakes/metadata-extractor
 * <dependency>
 * <groupId>com.drewnoakes</groupId>
 * <artifactId>metadata-extractor</artifactId>
 * <version>2.16.0</version>
 * </dependency>
 */
public class RenameFileTask {

    // 照片的拍摄时区，如位置在中国是：TimeZone.getTimeZone("GMT+8")
    private final static TimeZone PHOTO_CREATED_TIME_ZONE = TimeZone.getDefault();
    private final static SimpleDateFormat FILE_NAME_SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String DIR_RENAME_CREATED_DATE = "renameCreatedDate";
    private static final String DIR_RENAME_MODIFIED_DATE = "renameModifiedDate";

    /**
     * param 1: photos directory    需要使用照片拍摄日期命名的目录1
     * param 2: photos directory    需要使用照片拍摄日期命名的目录2
     * ...
     */
    public static void main(String[] args) {

        // 先检查是否有处理命名状态的目录，避免再次生成重命名状态与用户修改冲突
        if (checkRenameFile(args)) {
            return;
        }

        // 将目录中未识别到年月日的照片，改成命名状态
        int totalCount = startPrepareRename(args);

        // 进入问询用户修改命名的模式
        if (totalCount > 0) {
            checkRenameFile(args);
        }
    }

    private static int startPrepareRename(String[] inputPhotoDirs) {
        PhotosCalendar photosCalendar = new PhotosCalendar(FileDateParser.Provider.getParser());
        int totalCount = 0;

        List<Photo> allPhotoList = photosCalendar.findPhotos(inputPhotoDirs, null, null);
        if (allPhotoList.isEmpty()) {
            System.out.println("The photos in the directory are empty! " + Arrays.toString(inputPhotoDirs));
            return 0;
        }

        for (Photo photo : allPhotoList) {
            // 只对无法识别出年月日的照片，解析拍摄时间
            if (photo.isExplicitYYMM()) {
                continue;
            }

            totalCount ++;
            File photoFile = new File(photo.filePath);
            Metadata metadata;
            try {
                metadata = ImageMetadataReader.readMetadata(photoFile);
            } catch (Exception e) {
                System.err.println("RenameFileTask readMetadata error " + photoFile + e.getMessage());
                continue;
            }

            // 解析照片文件中的拍摄时间
            String createdDate = parseContentCreatedDate(metadata);
            if (createdDate != null) {
                prepareRename(photoFile, createdDate, DIR_RENAME_CREATED_DATE);
                continue;
            }

            // 解析照片文件中的修改时间（准确度不高，可作为最晚拍摄时间的参考）
            String modifiedDate = parseContentModifiedDate(metadata);
            if (modifiedDate != null) {
                prepareRename(photoFile, modifiedDate, DIR_RENAME_MODIFIED_DATE);
                continue;
            }
        }

        System.out.println("需要重命名的照片数：" + totalCount);
        return totalCount;
    }

    private static boolean checkRenameFile(String[] inputPhotoDirs) {
        boolean existRenameDateDir = false;
        for (String inputPhotoDir : inputPhotoDirs) {
            existRenameDateDir |= findRenameDirectory(new File(inputPhotoDir));
        }
        return existRenameDateDir;
    }

    private static boolean findRenameDirectory(File renameDir) {
        File[] listFiles = renameDir.listFiles();
        if (listFiles == null) {
            return false;
        }
        boolean existRenameDateDir = false;
        for (File file : listFiles) {
            if (!file.isDirectory()) {
                continue;
            }
            String dirName = file.getName();
            if (dirName.equals(DIR_RENAME_CREATED_DATE) || dirName.equals(DIR_RENAME_MODIFIED_DATE)) {
                rename(file);
                existRenameDateDir = true;
            } else {
                existRenameDateDir |= findRenameDirectory(file);
            }
        }
        return existRenameDateDir;
    }

    private static void rename(File renameDir) {
        List<RenameFile> renameFileList = findRenameFileList(renameDir);

        System.out.println("\n================================================");
        for (RenameFile filePair : renameFileList) {
            System.out.println(String.format("%s ---> %s", filePair.oldFile.getName(), filePair.newName == null ? "" : filePair.newName));
        }
        System.out.println(String.format("请检查该目录下文件名是否正确：【%s】（%d张）", renameDir, renameFileList.size()));
        System.out.print("即将重命名，确定请输入【yes】，跳过【no】，重置【reset】，退出【exit】：");

        String inputText = new Scanner(System.in).next().trim().toLowerCase();
        switch (inputText) {
            case "yes":
                renameImmediately(renameFileList, renameDir.getParentFile());
                deleteEmptyDir(renameDir);
                System.out.println("重命名成功！");
                break;
            case "reset":
                resetNameImmediately(renameFileList, renameDir.getParentFile());
                deleteEmptyDir(renameDir);
                System.out.println("重置成功！");
                break;
            case "no":
                System.out.println("跳过！");
                return;
            case "exit":
                System.exit(0);
                break;
            default:
                System.out.println("命令不理解，跳过！");
                break;
        }
    }

    private static void renameImmediately(List<RenameFile> renameFileList, File destDir) {
        for (RenameFile namePair : renameFileList) {
            String newName = namePair.newName;
            if (newName == null) {
                continue;
            }
            File oldFile = namePair.oldFile;
            File newFile = new File(destDir, newName);
            boolean success = oldFile.renameTo(newFile);
            System.out.println(String.format("rename file %s: %s ---> %s", success ? "success" : "error", oldFile.getAbsolutePath(), newFile.getName()));
        }
    }

    private static void resetNameImmediately(List<RenameFile> renameFileList, File destDir) {
        for (RenameFile namePair : renameFileList) {
            File oldFile = namePair.oldFile;
            String[] split = oldFile.getName().split("___");
            if (split.length != 2) {
                System.out.println("file reset error: " + oldFile.getName());
                continue;
            }
            File newFile = new File(destDir, split[1]);
            boolean success = oldFile.renameTo(newFile);
            System.out.println(String.format("rename file %s: %s ---> %s", success ? "success" : "error", oldFile.getAbsolutePath(), newFile.getName()));
        }
    }

    private static void deleteEmptyDir(File renameDir) {
        File[] listFiles = renameDir.listFiles();
        int fileCount = 0;
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isHidden()) {
                    continue;
                }
                System.out.println("remain file ---> " + renameDir.getAbsolutePath());
                fileCount ++;
            }
        }
        if (fileCount > 0) {
            rename(renameDir);
            return;
        }
        boolean success = renameDir.delete();
        System.out.println("delete directory " + (success ? "success" : "error") + renameDir);
    }

    private static List<RenameFile> findRenameFileList(File renameDir) {
        List<RenameFile> fileList = new ArrayList<>();
        File[] childFiles = renameDir.listFiles();
        if (childFiles == null) {
            return fileList;
        }
        for (File file : childFiles) {
            if (!file.isFile() || file.isHidden()) {
                continue;
            }
            String fileName = file.getName();
            String newFileName = parseNewFileName(fileName);
            fileList.add(new RenameFile(file, newFileName));
        }
        return fileList;
    }

    private static String parseNewFileName(String fileName) {
        String[] split = fileName.split("___");
        if (split.length != 2) {
            System.out.println("file rename error: " + fileName);
            return null;
        }

        String date = split[0];
        String oldName = split[1];
        // 文件名过长时，选择只要日期部分
        if (oldName.length() >= 16) {
            date = date.split("_")[0];
        }

        String newName = oldName.replaceFirst("(20[0-9]{2}(-?(0[1-9]|1[012])-?([012][0-9]|3[01]))?)", date);
        if (!newName.equals(oldName)) {
            return newName;
        }

        int beginIndex = oldName.lastIndexOf(".");
        if (beginIndex > 0) {
            String name = oldName.substring(0, beginIndex);
            String suffix = oldName.substring(beginIndex);
            return name + "_" + date + suffix;
        }
        return null;
    }

    private static void prepareRename(File file, String date, String prepareDir) {
        if (file.getParentFile().getName().equals(prepareDir)) {
            // 已经在准备重命名的文件夹，不需要再次创建，避免多次运行导致的嵌套
            return;
        }
        File prepareFolder = new File(file.getParent(), prepareDir);
        if (!prepareFolder.exists()) {
            boolean success = prepareFolder.mkdirs();
            System.out.println("RENAME DIR: " + prepareFolder + ", " + success);
        }
        boolean success = file.renameTo(new File(prepareFolder, date + "___" + file.getName()));
        System.out.println("FILE: " + success + "   " + file.getName() + "  --->  " + date);
    }

    private static String parseContentModifiedDate(Metadata metadata) {
        FileSystemDirectory fileSystemDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
        if (fileSystemDirectory == null) {
            return null;
        }
        Date modifiedDate = fileSystemDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, PHOTO_CREATED_TIME_ZONE);
        if (modifiedDate == null) {
            return null;
        }
        return FILE_NAME_SDF.format(modifiedDate);
    }

    private static String parseContentCreatedDate(Metadata metadata) {
        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Date createdDate = null;
        if (exifIFD0Directory != null) {
            createdDate = exifIFD0Directory.getDate(ExifDirectoryBase.TAG_DATETIME, PHOTO_CREATED_TIME_ZONE);
        }
        if (createdDate == null) {
            // TAG_DATETIME_ORIGINAL, TAG_DATETIME_DIGITIZED
            ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIFDDirectory != null) {
                createdDate = subIFDDirectory.getDate(ExifDirectoryBase.TAG_DATETIME_ORIGINAL, PHOTO_CREATED_TIME_ZONE);
            }
        }
        if (createdDate == null) {
            return null;
        }
        return FILE_NAME_SDF.format(createdDate);
    }

    public static class RenameFile {
        public final File oldFile;
        public final String newName;

        public RenameFile(File oldFile, String newName) {
            this.oldFile = oldFile;
            this.newName = newName;
        }
    }
}
