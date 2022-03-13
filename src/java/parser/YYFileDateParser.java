package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析只包含年份和月份的文件名，示例如：
 * C360_2015-03.jpg
 * IMG_201401.jpg
 * 202006_IMG.jpg
 */
public class YYFileDateParser implements FileDateParser {

    private final Pattern yyyyPattern = Pattern.compile("(^|.+?_)(20[0-9]{2}[-_]).*");

    @Override
    public String parse(String fileName, String filePath) {
        Matcher matcher = yyyyPattern.matcher(fileName);
        if (matcher.matches()) {
            String yyyy = matcher.group(2);
            return String.format("%04d%02d%02d",
                    Integer.parseInt(yyyy.substring(0, 4)),
                    0,
                    0);
        }
        return null;
    }
}
