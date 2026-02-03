package com.converter;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import com.converter.parsers.TableParser;
import com.converter.readers.DocumentReader;
import com.converter.writers.ExcelWriter;
import com.converter.writers.SnowballCategoriesCsvWriter;
import com.converter.writers.SnowballExcelWriter;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            Document doc = DocumentReader.getInstance().readDocumentFromMhtml("page.mhtml", "text=Вид операции");
            TableParser tableParser = TableParser.getInstance();
            TheadRow headers = tableParser.parseThead(doc);
            List<TableRow> data = tableParser.parseTbody(doc);

            ExcelWriter.getInstance().writeReport(headers, data);
            Set<String> tokenNames = data.stream().map(TableRow::tokenName).collect(Collectors.toSet());
            SnowballCategoriesCsvWriter.getInstance().writeCategories(tokenNames);
            SnowballExcelWriter.getInstance().writeSnowballReport(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
