package com.ccut.entity;

import com.ccut.dto.Question;
import lombok.Data;
import java.util.List;

@Data
public class Exam {
    private Long id;
    private List<Question> questions;
}


