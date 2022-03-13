package parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析时间戳命名的文件，示例如：
 *  mmexport1453090969801.jpg
 *  1436931640964.jpg
 */
public class TimestampFileDateParser implements FileDateParser {

    // 时间戳的有效开始和结束时间（减少误匹配）
    private final long validStartTimestamp = parseTimestamp("2000-01-01");
    private final long validEndTimestamp = System.currentTimeMillis();

    // 匹配：mmexport1453090969801.jpg、1436931640964.jpg；时间戳12-13位数值区间是：1973-03-03 - 2286-11-21
    private final Pattern timestampPattern = Pattern.compile(".*?([0-9]{12,13})\\.[jpegnifJPEGNIF]{3,4}");
    private final SimpleDateFormat yyyyMMddSDF = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String parse(String fileName, String filePath) {
        Matcher matcher = timestampPattern.matcher(fileName);
        if (matcher.matches()) {
            final long timestamp = Long.parseLong(matcher.group(1));
            if (timestamp < validStartTimestamp || timestamp > validEndTimestamp) {
                return null;
            }
            return yyyyMMddSDF.format(timestamp);
        }
        return null;
    }

    private long parseTimestamp(String text) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(text).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
