package entity;

public class Photo {

    public String name;
    public String filePath;

    public int year;
    public int month;
    public int day;

    public long fileSize;

    public PhotoType photoType;

    public String getDate() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    public void setDate(String yyyyMMdd) {
        if (yyyyMMdd == null || "null".equals(yyyyMMdd)) {
            return;
        }
        yyyyMMdd = yyyyMMdd.replace("-", "");
        year = Integer.parseInt(yyyyMMdd.substring(0, 4));
        month = Integer.parseInt(yyyyMMdd.substring(4, 6));
        day = Integer.parseInt(yyyyMMdd.substring(6, 8));
    }

    public void setDate(int... yyyyMMdd) {
        if (yyyyMMdd.length != 3) {
            return;
        }
        year = yyyyMMdd[0];
        month = yyyyMMdd[1];
        day = yyyyMMdd[2];
    }

    public boolean isExplicitYYMM() {
        return year != 0 && month != 0;
    }

    public boolean isExplicitYYMMDD() {
        return year != 0 && month != 0 && day != 0;
    }
}
