package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析年月日命名的文件，示例如：
 * C360_2015-03-27.jpg
 * IMG_20140114.jpg
 * 20200601_IMG.jpg
 */
public class YYMMDDFileDateParser implements FileDateParser {

    // 匹配：C360_2015-03-27、IMG_20140114、20200601_IMG
    private final Pattern yyyy_MM_ddPattern = Pattern.compile("(^|.+?_)(20[0-9]{2}-?(0[1-9]|1[012])-?([012][0-9]|3[01])).*");

    @Override
    public String parse(String fileName, String filePath) {
        Matcher matcher = yyyy_MM_ddPattern.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(2).replace("-", "");
        }
        return null;
    }
}
