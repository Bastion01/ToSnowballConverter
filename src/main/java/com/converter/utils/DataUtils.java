package com.converter.utils;

import com.converter.model.TableRow;

import java.util.*;

public class DataUtils {
    private static final DataUtils INSTANCE = new DataUtils();

    private DataUtils() {}

    public static DataUtils getInstance() {
        return INSTANCE;
    }

    @SafeVarargs
    public final List<TableRow> mergeAndSortByDateDesc(Collection<TableRow>... collections) {
        Set<TableRow> mergedList = new LinkedHashSet<>();

        for (Collection<TableRow> collection : collections) {
            if (collection != null) {
                mergedList.addAll(collection);
            }
        }

        return mergedList.stream()
                .filter(row -> row.date() != null)
                .sorted(Comparator.comparing(TableRow::date).reversed())
                .toList();
    }
}

