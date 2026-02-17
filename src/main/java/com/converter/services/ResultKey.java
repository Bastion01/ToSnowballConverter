package com.converter.services;

public enum ResultKey {
    FINSTORE_REPORT("FinstoreReport"),
    SNOWBALL_REPORT("SnowballReport"),
    SNOWBALL_CATEGORIES("SnowballCategories");

    private String value;
    ResultKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
