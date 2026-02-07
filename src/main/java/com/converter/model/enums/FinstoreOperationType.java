package com.converter.model.enums;

import java.util.Arrays;
import java.util.Set;

public enum FinstoreOperationType {
    CASH_IN("Пополнение кошелька"),
    CASH_OUT("Вывод денежных средств"),
    INCOME("Получение дохода"),
    INCOME_REFERRAL("Получение дохода по реферальной программе"),
    BUY_TOKENS("Покупка токенов"),
    BUY_TOKENS_ON_SECONDARY_MARKET("Покупка ICO токенов на Вторичном рынке"),
    SELL_TOKENS_ON_SECONDARY_MARKET("Продажа ICO токенов на Вторичном рынке");

    private String value;
    FinstoreOperationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FinstoreOperationType getValueOf(String operationType) {
        return Arrays.stream(FinstoreOperationType.values()).filter(opType -> operationType.equals(opType.getValue())).findFirst().get();
    }

    public boolean isTokensPurchase() {
        return Set.of(BUY_TOKENS, BUY_TOKENS_ON_SECONDARY_MARKET).contains(this);
    }

    public boolean isOperationWithoutTokens() {
        return Set.of(CASH_IN, CASH_OUT, INCOME_REFERRAL).contains(this);
    }

    public boolean isCashInOut() {
        return Set.of(CASH_IN, CASH_OUT).contains(this);
    }
}
