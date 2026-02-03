package com.converter.model;

import com.converter.model.enums.Currency;

import java.util.Date;

public record TableRow(
        String operationType,
        String tokenName,
        Integer tokenAmount,
        Double price,
        Date date,
        Currency currency
) {}