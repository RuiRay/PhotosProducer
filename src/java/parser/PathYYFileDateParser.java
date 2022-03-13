package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析目录中包含的日期（主要由用户自定义？）
 */
public class PathYYFileDateParser implements FileDateParser {

    private final Pattern pathYearPattern = Pattern.compile(".*?/photos([0-9]{4})/.*");

    @Override
    public String parse(String fileName, String filePath) {
        Matcher matcher = pathYearPattern.matcher(filePath);
        if (matcher.matches()) {
            return String.format("%04d%02d%02d", Integer.parseInt(matcher.group(1)), 0, 0);
        }
        return null;
    }

}
