package parser;

import java.util.ArrayList;
import java.util.List;

public class CompoundFileDateParser implements FileDateParser {

    private final List<FileDateParser> fileDateParserList = new ArrayList<>();

    @Override
    public String parse(String fileName, String filePath) {
        for (FileDateParser fileDateParser : fileDateParserList) {
            String result = fileDateParser.parse(fileName, filePath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public CompoundFileDateParser addFileDateParser(FileDateParser fileDateParser) {
        if (fileDateParserList.contains(fileDateParser)) {
            return this;
        }
        fileDateParserList.add(fileDateParser);
        return this;
    }
}
