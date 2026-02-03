package com.converter.model;

import java.util.Arrays;
import java.util.Set;

public enum OperationType {
    CASH_IN("Пополнение кошелька"),
    CASH_OUT("Вывод денежных средств"),
    INCOME("Получение дохода"),
    INCOME_REFERRAL("Получение дохода по реферальной программе"),
    BUY_TOKENS("Покупка токенов");

    private String value;
    OperationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OperationType getValueOf(String operationType) {
        return Arrays.stream(OperationType.values()).filter(opType -> operationType.equals(opType.getValue())).findFirst().get();
    }

    public boolean isOperationWithoutTokens() {
        return Set.of(CASH_IN, CASH_OUT, INCOME_REFERRAL).contains(this);
    }

    public boolean isCashInOut() {
        return Set.of(CASH_IN, CASH_OUT).contains(this);
    }
}
