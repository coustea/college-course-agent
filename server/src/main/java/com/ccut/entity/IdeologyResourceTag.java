package com.ccut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdeologyResourceTag {
    private Long tagId;// 标签ID
    private String tagName;// 标签名称
    private String tagType;// 标签类型：theme/keyword/scene/value
    private String description;// 标签说明
    private LocalDateTime createdAt;// 创建时间
}
