package task;

import utils.FileUtil;
import utils.imgscalr.Scalr;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 缩减图片尺寸（会保留拍摄日期、位置等 EXIF 信息）
 * 依赖库和参考资料：
 * https://github.com/rkalla/imgscalr
 * https://stackoverflow.com/questions/8972357/manipulate-an-image-without-deleting-its-exif-data
 */
public class ReduceImageSizeTask {

    // 缩减后图片高/宽的最大值，如：2K
    private static final int TARGET_DIMENSION = 1024 * 2;
    // 当文件尺寸超出多少时进行缩减，如：1.2MB
    private static final int TARGET_FILE_SIZE = (int) (1024 * 1024 * 1.2f);
    // 缩减后的图片是否覆盖原图
    private static final boolean OVERWRITE_FILE = true;
    // 保存的 jpg 图片质量
    private static final float JPG_OUTPUT_QUALITY = 0.8f;

    // 支持减少图片尺寸的后缀
    private static final Set<String> SUPPORT_FILE_SUFFIX = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png"
    ));

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("ReduceImageSizeTask 缩减图片尺寸 请指定输入目录");
            return;
        }
        String reduceDir = args[0];
        File outputDir = null;

        if (args.length > 1) {
            outputDir = new File(args[1]);
        }

        System.out.println("java: input dir: " + args[0] + (outputDir == null ? "" : (", output dir: " + outputDir)));

        File[] listFiles = new File(reduceDir).listFiles();
        if (listFiles == null) {
            System.out.println("file dir is empty!");
            return;
        }

        final long beginTime = System.currentTimeMillis();
        long totalSavedFile = 0;
        ReduceImageSizeTask task = new ReduceImageSizeTask();
        for (int i = 0; i < listFiles.length; i++) {
            final File file = listFiles[i];
            String fileSuffix = getFileSuffix(file.getName()).toLowerCase();
            if (!SUPPORT_FILE_SUFFIX.contains(fileSuffix)) {
                continue;
            }
            if (file.length() < TARGET_FILE_SIZE) {
                continue;
            }
            long savedSize = task.scaleDownImage(file, outputDir);
            totalSavedFile += savedSize;
            System.out.println(String.format("Progress: %d/%d, elapse: %s, file: %s, saved: %s",
                    i + 1, listFiles.length,
                    convertTime(System.currentTimeMillis() - beginTime),
                    file.getName(), FileUtil.convertFileSize(savedSize)));
        }
        System.out.println("done. saved: " + FileUtil.convertFileSize(totalSavedFile));
    }

    public long scaleDownImage(File imageFile) {
        File outputDirectory = new File(imageFile.getParentFile(), "target");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File outputFile = new File(outputDirectory, imageFile.getName());

        final long imageOriginSize = imageFile.length();
        boolean success = scaleDownImage(imageFile, outputFile, TARGET_DIMENSION);
        if (!success) {
            outputFile.delete();
            return 0;
        }

        File trashDirectory = new File(imageFile.getParentFile(), "trash");
        if (!trashDirectory.exists()) {
            trashDirectory.mkdirs();
        }
        imageFile.renameTo(new File(trashDirectory, imageFile.getName()));

        if (OVERWRITE_FILE) {
            outputFile.renameTo(new File(outputDirectory.getParent(), outputFile.getName()));
        }
        return imageOriginSize - outputFile.length();
    }

    // 缩减图片到不同的目录
    public long scaleDownImage(File imageFile, File outputDirectory) {
        if (outputDirectory == null) {
            return scaleDownImage(imageFile);
        }

        File outputFile = new File(outputDirectory, imageFile.getName());

        final long imageOriginSize = imageFile.length();
        boolean success = scaleDownImage(imageFile, outputFile, TARGET_DIMENSION);
        if (!success) {
            outputFile.delete();
            return 0;
        }
        return imageOriginSize - outputFile.length();
    }

    public boolean scaleDownImage(File imageFile, File outputFile, int targetSize) {
        final String fileSuffix = getFileSuffix(imageFile.getName()).toLowerCase();
        if (fileSuffix.isEmpty()) {
            System.out.println(String.format("scaleDownImage() file suffix not found %s", imageFile));
            return false;
        }
        try {
            ImageReader reader = ImageIO.getImageReadersBySuffix(fileSuffix).next();
            reader.setInput(ImageIO.createImageInputStream(imageFile));
            IIOMetadata metadata = reader.getImageMetadata(0);
            BufferedImage bufferedImage = reader.read(0);

            // 图片只缩小，不放大
            if (Math.max(bufferedImage.getWidth(), bufferedImage.getHeight()) < targetSize) {
                System.out.println(String.format("scaleDownImage() small image (%d*%d) %s", bufferedImage.getWidth(), bufferedImage.getHeight(), imageFile));
                return false;
            }

            bufferedImage = Scalr.resize(bufferedImage, Scalr.Method.AUTOMATIC, targetSize);

            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputFile);
            Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName(fileSuffix);
            ImageWriter writer = iterator.next();
            writer.setOutput(imageOutputStream);

            ImageWriteParam iwParam = null;
            if ("jpg".equals(fileSuffix) || "jpeg".equals(fileSuffix)) {
                iwParam = writer.getDefaultWriteParam();
                iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwParam.setCompressionQuality(JPG_OUTPUT_QUALITY);
            }
            writer.write(null, new IIOImage(bufferedImage, null, metadata), iwParam);
            writer.dispose();
            return true;
        } catch (IOException e) {
            System.err.println("scaleDownImage() error " + imageFile + "\n" + e.getMessage());
        }
        return false;
    }

    private static String getFileSuffix(String fileName) {
        int suffixStartIndex = fileName.lastIndexOf(".");
        if (suffixStartIndex < 0) {
            return "";
        }
        return fileName.substring(suffixStartIndex + 1);
    }

    // 2H 2:22s .222ms
    private static String convertTime(long milliseconds) {
        final long SECOND = 1000;
        final long MINUTE = SECOND * 60;
        final long HOUR = MINUTE * 60;

        if (milliseconds >= HOUR) {
            return String.format("%dH %02d:%02ds", milliseconds / HOUR, (milliseconds % HOUR) / MINUTE, (milliseconds % MINUTE) / SECOND);
        } else if (milliseconds >= MINUTE) {
            return String.format("%02d:%02ds", (milliseconds % HOUR) / MINUTE, (milliseconds % MINUTE) / SECOND);
        } else if (milliseconds >= SECOND) {
            return String.format("%d.%dms", (milliseconds % MINUTE) / SECOND, milliseconds % SECOND);
        } else {
            return String.format("%dms", milliseconds);
        }
    }

}
