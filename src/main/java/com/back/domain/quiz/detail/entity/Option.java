package com.back.domain.quiz.detail.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Option {
    OPTION1("option1"),
    OPTION2("option2"),
    OPTION3("option3");

    private final String value;

    Option(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Option fromValue(String value) {
        for (Option o : Option.values()) {
            if (o.value.equals(value)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
