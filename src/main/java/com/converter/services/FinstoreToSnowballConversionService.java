package com.converter.services;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import com.converter.parsers.finstore.FinstoreTableParser;
import com.converter.readers.DocumentReader;
import com.converter.utils.DataUtils;
import com.converter.writers.finstore.FinstoreCategoriesCsvWriter;
import com.converter.writers.finstore.FinstoreExcelWriter;
import com.converter.writers.snowball.SnowballExcelWriter;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class FinstoreToSnowballConversionService {
    private static final FinstoreToSnowballConversionService INSTANCE =
            new FinstoreToSnowballConversionService();

    private FinstoreToSnowballConversionService() {}

    public static FinstoreToSnowballConversionService getInstance() {
        return INSTANCE;
    }

    public List<File> convert(Set<File> dataFiles, File categoriesFile, List<ResultKey> resultKeys) {
        List<Document> documents = readDocuments(dataFiles);
        TheadRow headers = readHeaders(documents);
        List<TableRow> data = readData(documents);

        return generateReports(categoriesFile, headers, data, resultKeys);
    }

    public List<Document> readDocuments(Set<File> dataFiles) {
        List<Document> documents = DocumentReader.getInstance().readDocumentsFromMhtml(
                dataFiles, "text=Вид операции"
        );
        return documents;
    }

    public TheadRow readHeaders(List<Document> documents) {
        FinstoreTableParser tableParser = FinstoreTableParser.getInstance();
        return tableParser.parseThead(documents.getFirst());
    }

    public List<TableRow> readData(List<Document> documents) {
        FinstoreTableParser tableParser = FinstoreTableParser.getInstance();
        List<TableRow> data = new ArrayList<>();
        for (Document doc : documents) {
            List<TableRow> newData = tableParser.parseTbody(doc);
            data = DataUtils.getInstance().mergeAndSortByDateDesc(data, newData);
        }
        return data;
    }

    public List<File> generateReports(File categoriesFile, TheadRow headers, List<TableRow> data, List<ResultKey> resultKeys) {
        List<File> resultList = new ArrayList<>();
        if (resultKeys.contains(ResultKey.FINSTORE_REPORT)) {
            File finstoreReport = FinstoreExcelWriter.getInstance().writeReport(headers, data);
            resultList.add(finstoreReport);
        }
        if (resultKeys.contains(ResultKey.SNOWBALL_REPORT)) {
            File snowballReport = SnowballExcelWriter.getInstance().writeSnowballReport(data);
            resultList.add(snowballReport);
        }
        if (resultKeys.contains(ResultKey.SNOWBALL_CATEGORIES)) {
            Set<String> tokenNames = data.stream().map(TableRow::tokenName).collect(Collectors.toSet());
            File snowballCategories = FinstoreCategoriesCsvWriter.getInstance().writeCategories(tokenNames, categoriesFile);
            resultList.add(snowballCategories);
        }

        return resultList;
    }
}
