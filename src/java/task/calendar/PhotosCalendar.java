package task.calendar;

import entity.Photo;
import entity.PhotoType;
import entity.SummaryInfo;
import parser.FileDateParser;
import utils.FileUtil;

import java.io.File;
import java.util.*;

public class PhotosCalendar {

    private final FileDateParser fileDateParser;

    public PhotosCalendar(FileDateParser fileDateParser) {
        this.fileDateParser = fileDateParser;
    }

    public SummaryInfo computeSummaryInfo(List<Photo> allPhotoList) {
        SummaryInfo summaryInfo = new SummaryInfo();
        summaryInfo.yyyyMMddPhotosMap = convertDatePhotosMap(allPhotoList);
        summaryInfo.totalPhotosCount = allPhotoList.size();
        for (Photo photo : allPhotoList) {
            summaryInfo.totalPhotosSize += photo.fileSize;
        }

        int[] photosStartEndDate = computePhotosStartEndDate(summaryInfo.yyyyMMddPhotosMap);
        summaryInfo.startYear = photosStartEndDate[0];
        summaryInfo.startMonth = photosStartEndDate[1];
        summaryInfo.endYear = photosStartEndDate[2];
        summaryInfo.endMonth = photosStartEndDate[3];
        return summaryInfo;
    }

    private int[] computePhotosStartEndDate(Map<Integer, Map<Integer, Map<Integer, List<Photo>>>> yyyyMMddPhotosMap) {
        int startYear = Integer.MAX_VALUE;
        int endYear = 0;
        Set<Integer> yearSet = yyyyMMddPhotosMap.keySet();
        for (Integer year : yearSet) {
            if (year != 0 && year < startYear)
                startYear = year;
            if (year > endYear)
                endYear = year;
        }
        startYear = startYear == Integer.MAX_VALUE ? 0 : startYear;

        int startMonth = 12;
        Set<Integer> startMonthSet = yyyyMMddPhotosMap.get(startYear).keySet();
        for (Integer month : startMonthSet) {
            if (month != 0 && month < startMonth)
                startMonth = month;
        }

        int endMonth = 1;
        Set<Integer> endMonthSet = yyyyMMddPhotosMap.get(endYear).keySet();
        for (Integer month : endMonthSet) {
            if (month > endMonth)
                endMonth = month;
        }

        return new int[]{startYear, startMonth, endYear, endMonth};
    }

    private Map<Integer, Map<Integer, Map<Integer, List<Photo>>>> convertDatePhotosMap(List<Photo> allPhotoList) {
        final Map<Integer, Map<Integer, Map<Integer, List<Photo>>>> yyyyMMddPhotosMap = new TreeMap<>();

        for (Photo photo : allPhotoList) {
            Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(photo.year);
            if (yearMap == null) {
                yearMap = new TreeMap<>();
                yyyyMMddPhotosMap.put(photo.year, yearMap);
            }

            Map<Integer, List<Photo>> monthMap = yearMap.get(photo.month);
            if (monthMap == null) {
                monthMap = new TreeMap<>();
                yearMap.put(photo.month, monthMap);
            }

            List<Photo> photoList = monthMap.get(photo.day);
            if (photoList == null) {
                photoList = new ArrayList<>();
                monthMap.put(photo.day, photoList);
            }
            photoList.add(photo);
        }

        return yyyyMMddPhotosMap;
    }

    public List<Photo> findPhotos(String[] inputPhotoDirs, String relativeDir, File cacheFile) {
        List<Photo> resultList = readPhotoListCache(cacheFile);
        if (!resultList.isEmpty()) {
            return resultList;
        }
        for (String rootPath : inputPhotoDirs) {
            findChildFile(resultList, new File(rootPath), (relativeDir != null && rootPath.startsWith(relativeDir)) ? relativeDir : null);
        }
        writePhotoListCache(cacheFile, resultList);
        return resultList;
    }

    private void findChildFile(List<Photo> resultList, File baseDir, String validRelativeDir) {
        File[] listFiles = baseDir.listFiles();
        if (listFiles == null) {
            return;
        }
        final boolean isProjectTestDir = validRelativeDir != null && validRelativeDir.startsWith(".");
        for (File file : listFiles) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                PhotoType photoType = getPhotoType(fileName);
                if (photoType == null) {
                    continue;
                }
                Photo photo = new Photo();
                photo.photoType = photoType;
                if (validRelativeDir != null) {
                    String imgPath = isProjectTestDir ? file.getPath() : file.getAbsolutePath();
                    photo.filePath = imgPath.replace(validRelativeDir, ".");
                } else {
                    photo.filePath = file.getAbsolutePath();
                }
                photo.name = file.getName();
                photo.setDate(fileDateParser.parse(file.getName(), photo.filePath));
                photo.fileSize = file.length();
                resultList.add(photo);
                if (resultList.size() % 100 == 0) {
                    System.out.println("find photos progress " + resultList.size());
                }
            } else if (file.isDirectory()) {
                findChildFile(resultList, file, validRelativeDir);
            }
        }
    }

    private PhotoType getPhotoType(String fileName) {
        PhotoType photoType = null;
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")
                || fileName.endsWith(".gif")) {
            photoType = PhotoType.IMAGE;
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".m4a")
                || fileName.endsWith(".mov")
                || fileName.endsWith(".m4v")) {
            photoType = PhotoType.VIDEO;
        }
        return photoType;
    }

    // 【测试用】缓存文件信息，避免媒体资源较多时花费较长时间
    private List<Photo> readPhotoListCache(File cacheFile) {
        final List<Photo> result = new ArrayList<>();
        if (cacheFile == null || !cacheFile.exists()) {
            return result;
        }
        FileUtil.readFile(cacheFile, new FileUtil.ReaderCallback() {
            @Override
            public boolean onReadLine(String line) {
                String[] split = line.split("######");
                if (split.length != 5) {
                    System.out.println("readPhotoListCache() error " + line);
                    return true;
                }
                Photo photo = new Photo();
                photo.setDate(split[0]);
                photo.fileSize = Long.parseLong(split[1]);
                photo.photoType = PhotoType.valueOf(split[2]);
                photo.name = split[3];
                photo.filePath = split[4];
                if (!photo.isExplicitYYMMDD() && fileDateParser != null) {
                    photo.setDate(fileDateParser.parse(photo.name, photo.filePath));
                }
                result.add(photo);
                return true;
            }
        });
        return result;
    }

    private void writePhotoListCache(File cacheFile, List<Photo> resultList) {
        if (cacheFile == null) {
            return;
        }
        StringBuilder result = new StringBuilder();
        for (Photo photo : resultList) {
            result.append(photo.getDate())
                    .append("######").append(photo.fileSize)
                    .append("######").append(photo.photoType)
                    .append("######").append(photo.name)
                    .append("######").append(photo.filePath)
                    .append("\n");
        }
        FileUtil.writeFile(cacheFile, result.toString());
    }

}
