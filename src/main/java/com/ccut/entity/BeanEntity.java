package com.ccut.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"title","data","author","content"})
public class BeanEntity {
    private String title;
    private String author;
    private String date;
    private String content;
}


