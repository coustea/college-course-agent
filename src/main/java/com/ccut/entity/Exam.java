package com.ccut.entity;

import lombok.Data;
import java.util.List;

@Data
public class Exam {
    private Long id;
    private List<Question> questions;
}


