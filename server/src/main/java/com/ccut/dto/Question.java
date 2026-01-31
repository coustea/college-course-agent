package com.ccut.dto;

import java.util.List;

public record Question(
    Long id,
    String question,
    Type type,
    List<String> options,
    String answer,
    String analysis
) {
    public enum Type { CHOICE, JUDGE }
}