package entity;

import java.util.List;
import java.util.Map;

public class SummaryInfo {

    public int totalPhotosCount = 0;
    public long totalPhotosSize = 0;

    public int startYear = 0;
    public int startMonth = 0;
    public int endYear = 0;
    public int endMonth = 0;

    public Map<Integer, Map<Integer, Map<Integer, List<Photo>>>> yyyyMMddPhotosMap;

    public int getYearPhotoCount(int year) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        int totalPhotoCount = 0;
        for (Integer month : yearMap.keySet()) {
            for (Integer day : yearMap.get(month).keySet()) {
                totalPhotoCount += yearMap.get(month).get(day).size();
            }
        }
        return totalPhotoCount;
    }

    public long getYearFileSize(int year) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        long totalFileSize = 0;
        for (Integer month : yearMap.keySet()) {
            Map<Integer, List<Photo>> monthMap = yearMap.get(month);
            for (Integer day : monthMap.keySet()) {
                List<Photo> photos = monthMap.get(day);
                for (Photo photo : photos) {
                    totalFileSize += photo.fileSize;
                }
            }
        }
        return totalFileSize;
    }

    public int getMonthPhotoCount(int year, int month) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        Map<Integer, List<Photo>> monthMap = yearMap.get(month);
        if (monthMap == null)
            return 0;
        int totalPhotoCount = 0;
        for (Integer day : monthMap.keySet()) {
            totalPhotoCount += monthMap.get(day).size();
        }
        return totalPhotoCount;
    }

    public int getMonthPhotoDayCount(int year, int month) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        Map<Integer, List<Photo>> monthMap = yearMap.get(month);
        if (monthMap == null)
            return 0;
        return monthMap.size();
    }

    public long getMonthFileSize(int year, int month) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        Map<Integer, List<Photo>> monthMap = yearMap.get(month);
        if (monthMap == null)
            return 0;
        long totalFileSize = 0;
        for (Integer day : monthMap.keySet()) {
            List<Photo> photos = monthMap.get(day);
            for (Photo photo : photos) {
                totalFileSize += photo.fileSize;
            }
        }
        return totalFileSize;
    }

    public int getDayPhotoCount(int year, int month, int day) {
        Map<Integer, Map<Integer, List<Photo>>> yearMap = yyyyMMddPhotosMap.get(year);
        if (yearMap == null)
            return 0;
        Map<Integer, List<Photo>> monthMap = yearMap.get(month);
        if (monthMap == null)
            return 0;
        List<Photo> photos = monthMap.get(day);
        if (photos == null)
            return 0;
        return photos.size();
    }
}
