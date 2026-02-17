package com.converter;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import com.converter.parsers.finstore.FinstoreTableParser;
import com.converter.readers.DocumentReader;
import com.converter.utils.DataUtils;
import com.converter.writers.finstore.FinstoreExcelWriter;
import com.converter.writers.finstore.FinstoreCategoriesCsvWriter;
import com.converter.writers.snowball.SnowballExcelWriter;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
//            List<Document> documents = DocumentReader.getInstance().readDocumentsFromMhtml(
//                    Set.of("page.mhtml"), "text=Вид операции"
//            );
            Set<File> mhtmlFiles = new HashSet<>();
            for (String fileName : Set.of("page1.mhtml", "page2.mhtml", "page3.mhtml", "page4.mhtml", "page5.mhtml")) {
                File file = new File(fileName);
                mhtmlFiles.add(file);
            }

            List<Document> documents = DocumentReader.getInstance().readDocumentsFromMhtml(
                    mhtmlFiles, "text=Вид операции"
            );
            FinstoreTableParser tableParser = FinstoreTableParser.getInstance();
            TheadRow headers = tableParser.parseThead(documents.getFirst());
            List<TableRow> data = new ArrayList<>();
            for (Document doc : documents) {
                List<TableRow> newData = tableParser.parseTbody(doc);
                data = DataUtils.getInstance().mergeAndSortByDateDesc(data, newData);
            }

            FinstoreExcelWriter.getInstance().writeReport(headers, data);
            Set<String> tokenNames = data.stream().map(TableRow::tokenName).collect(Collectors.toSet());
            FinstoreCategoriesCsvWriter.getInstance().writeCategories(tokenNames, null);
            SnowballExcelWriter.getInstance().writeSnowballReport(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
