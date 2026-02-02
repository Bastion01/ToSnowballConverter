package com.converter;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import com.converter.parsers.TableParser;
import com.converter.readers.DocumentReader;
import com.converter.writers.ExcelWriter;
import org.jsoup.nodes.Document;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Document doc = DocumentReader.getInstance().readDocumentFromMhtml("page.mhtml", "text=Вид операции");
            TableParser tableParser = TableParser.getInstance();
            TheadRow headers = tableParser.parseThead(doc);
            List<TableRow> data = tableParser.parseTbody(doc);

            ExcelWriter.getInstance().writeReport(headers, data);
//            SnowballExcelWriter.getInstance().writeSnowballReport(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
