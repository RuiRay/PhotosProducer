package utils;

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileUtil {

    public static String readFile(String filePath) {
        return readFile(new File(filePath));
    }

    public static String readFile(File file) {
        final StringBuilder sBuilder = new StringBuilder();
        readFile(file, new ReaderCallback() {
            final String LINE_SEPARATOR = System.getProperty("line.separator");

            @Override
            public boolean onReadLine(String line) {
                sBuilder.append(line).append(LINE_SEPARATOR);
                return true;
            }
        });
        return sBuilder.toString();
    }

    public static void readFile(File file, ReaderCallback readerCallback) {
        if (!file.exists()) {
            return;
        }
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bReader.readLine()) != null) {
                if (!readerCallback.onReadLine(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(bReader);
        }
    }

    public static boolean writeFile(File file, String content) {
        checkCreateFile(file);
        if (content == null) {
            content = "";
        }
        BufferedWriter bWriter = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
            bWriter = new BufferedWriter(outputStreamWriter);
            bWriter.write(content);
            bWriter.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(bWriter);
        }
        return false;
    }

    private static boolean checkCreateFile(File todoFile) {
        if (todoFile.exists()) {
            return true;
        }
        File parentFile = todoFile.getParentFile();
        if (!parentFile.exists()) {
            boolean success = parentFile.mkdirs();
            if (!success) {
                return false;
            }
        }
        try {
            return todoFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean copyFile(File fromFile, File toFile) {
        boolean success = checkCreateFile(toFile);
        if (!success) {
            return false;
        }
        BufferedInputStream buffIns = null;
        BufferedOutputStream buffOut = null;
        try {
            buffIns = new BufferedInputStream(new FileInputStream(fromFile));
            buffOut = new BufferedOutputStream(new FileOutputStream(toFile));
            byte[] buff = new byte[1024 * 4];
            int length;
            while ((length = buffIns.read(buff)) != -1) {
                buffOut.write(buff, 0, length);
            }
            buffOut.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(buffIns);
            closeIO(buffOut);
        }
        return false;
    }

    public interface ReaderCallback {

        boolean onReadLine(String line);
    }

    public static int removeDir(File dir, String... excludeDir) {
        Set<String> excludeSet = new HashSet<>();
        excludeSet.addAll(Arrays.asList(excludeDir));
        return removeDir(dir, excludeSet);
    }

    public static int removeDir(File dir, Set<String> excludeSet) {
        if (!dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        int totalRemovedCount = 0;
        for (File file : files) {
            if (excludeSet != null && excludeSet.contains(file.getName())) {
                continue;
            }
            if (file.isDirectory()) {
                totalRemovedCount += removeDir(file, excludeSet);
            } else {
                if (file.delete()) {
                    totalRemovedCount++;
                }
            }
        }
        if (dir.delete()) {
            totalRemovedCount++;
        }
        return totalRemovedCount;
    }

    public static int copyDir(File fromDir, File toDir) {
        File[] listFiles = fromDir.listFiles();
        if (listFiles == null) {
            return 0;
        }
        int totalCopyCount = 0;
        for (File file : listFiles) {
            if (file.isFile()) {
                boolean success = FileUtil.copyFile(file, new File(toDir, file.getName()));
                if (success) {
                    totalCopyCount++;
                }
            } else if (file.isDirectory()) {
                totalCopyCount += copyDir(file, new File(toDir, file.getName()));
            }
        }
        return totalCopyCount;
    }

    public static void closeIO(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将字节数转成字符串
     * @param size  字节数，单位：byte
     * @return  eg: 6.07 GB, 529.99 MB
     */
    public static String convertFileSize(long size) {
        final long SIZE_1KB = 1024;
        final long SIZE_1MB = SIZE_1KB * 1024;
        final long SIZE_1GB = SIZE_1MB * 1024;

        if (size >= SIZE_1GB) {
            return String.format("%.2f GB", (float) size / SIZE_1GB);
        } else if (size >= SIZE_1MB) {
            float f = (float) size / SIZE_1MB;
            return String.format(f > 100 ? "%.2f MB" : "%.2f MB", f);
        } else if (size >= SIZE_1KB) {
            float f = (float) size / SIZE_1KB;
            return String.format(f > 100 ? "%.0f KB" : "%.0f KB", f);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * 计算文件的 MD5 值，结果字符串的长度是32
     * @return  eg: b8d1918f3ba631d6bec87541f042df42
     */
    public static String getFileMD5(String filePath) {
        FileInputStream fileInputStream = null;
        try {
            byte[] buffer = new byte[8192];
            int length;
            MessageDigest md = MessageDigest.getInstance("MD5");
            File file = new File(filePath);
            fileInputStream = new FileInputStream(file);
            while ((length = fileInputStream.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            return byte2hex(md.digest());
        } catch (Exception e) {
            return "";
        } finally {
            closeIO(fileInputStream);
        }
    }

    /**
     * 转换 byte 数组为 16 进制字符串，结果长度为 bytes.length * 2
     * @return eg: b8d1918f3ba631d6bec87541f042df42
     */
    public static String byte2hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        String hex;
        for (int n = 0; bytes != null && n < bytes.length; n++) {
            hex = Integer.toHexString(bytes[n] & 0xFF);
            if (hex.length() == 1)
                result.append('0');
            result.append(hex);
        }
        return result.toString();
    }

}
