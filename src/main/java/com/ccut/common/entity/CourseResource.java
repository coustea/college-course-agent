package com.ccut.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseResource {
    private Integer id;
    private Integer courseId;
    private String pptPath;
    private String imagePath;
    private String videoPath;

    private Course course;

}
