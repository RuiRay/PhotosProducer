package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析只包含年份和月份的文件名，示例如：
 * C360_2015-03.jpg
 * IMG_201401.jpg
 * 202006_IMG.jpg
 */
public class YYMMFileDateParser implements FileDateParser {

    private final Pattern yyyyMMPattern = Pattern.compile("(^|.+?_)(20[0-9]{2}[-_]?(0[1-9]|1[012])).*");

    @Override
    public String parse(String fileName, String filePath) {
        Matcher matcher = yyyyMMPattern.matcher(fileName);
        if (matcher.matches()) {
            String yyyyMM = matcher.group(2);
            return String.format("%04d%02d%02d",
                    Integer.parseInt(yyyyMM.substring(0, 4)),
                    Integer.parseInt(yyyyMM.substring(yyyyMM.length() - 2, yyyyMM.length())),
                    0);
        }
        return null;
    }
}
