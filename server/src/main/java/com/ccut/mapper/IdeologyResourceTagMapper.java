package com.ccut.mapper;

import com.ccut.entity.IdeologyResourceTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IdeologyResourceTagMapper {

    int insertIgnoreDuplicate(IdeologyResourceTag tag);

    IdeologyResourceTag selectByName(@Param("tagName") String tagName);

    int attachTag(@Param("resourceId") Long resourceId, @Param("tagId") Long tagId);

    int deleteResourceTags(@Param("resourceId") Long resourceId);

    List<IdeologyResourceTag> listByResourceId(@Param("resourceId") Long resourceId);
}
