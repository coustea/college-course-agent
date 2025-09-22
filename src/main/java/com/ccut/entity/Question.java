package com.ccut.entity;

import lombok.Data;
import java.util.List;

@Data
public class Question {
    public enum Type { CHOICE, JUDGE }

    private Long id;
    private String question;
    private Type type;
    private List<String> options;
    private String answer;
    private String analysis;
}


