package com.converter.model.enums;

import java.util.Arrays;
import java.util.Set;

public enum EventType {
    CASH_IN("Cash_In"),
    CASH_OUT("Cash_Out"),
    CASH_GAIN("Cash_Gain"),
    DIVIDEND("Dividend"),
    BUY("Buy");

    private String value;
    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EventType getValueOf(String eventType) {
        return Arrays.stream(EventType.values()).filter(evType -> eventType.equals(evType.getValue())).findFirst().get();
    }

    public boolean isCashInOut() {
        return Set.of(CASH_IN, CASH_OUT).contains(this);
    }

    public boolean isIncomeEvent() {
        return Set.of(CASH_IN, CASH_OUT, CASH_GAIN, DIVIDEND).contains(this);
    }
}
