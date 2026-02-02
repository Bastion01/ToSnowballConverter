package com.converter.model;

import java.util.List;

public record TheadRow(List<String> columnNames) {
    // Record автоматически создаст геттеры, toString и конструктор
}
