package parser;

// 文件名称中的日期解析器
public interface FileDateParser {

    /**
     * 解析文件名或路径中的日期，返回 yyyyMMdd 的格式，如：20220101；若月份和日期解析失败，可以写成如 20220000 的形式
     *
     * @param fileName 文件名称
     * @param filePath 文件路径
     * @return 返回 null 标识未识别
     */
    String parse(String fileName, String filePath);

    class Provider {

        private static FileDateParser fileDateParser;

        public static FileDateParser getParser() {
            if (fileDateParser != null) {
                return fileDateParser;
            }
            fileDateParser = new CompoundFileDateParser()
                    .addFileDateParser(new YYMMDDFileDateParser())
                    .addFileDateParser(new TimestampFileDateParser())
                    .addFileDateParser(new YYMMFileDateParser())
                    .addFileDateParser(new YYFileDateParser())
                    .addFileDateParser(new PathYYFileDateParser());
            return fileDateParser;
        }
    }
}
