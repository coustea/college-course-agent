package com.ccut.mapper;

import com.ccut.entity.IdeologyResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IdeologyResourceMapper {

    int insert(IdeologyResource resource);

    int updateById(IdeologyResource resource);

    int deleteById(@Param("resourceId") Long resourceId);

    IdeologyResource selectById(@Param("resourceId") Long resourceId);

    List<IdeologyResource> search(@Param("courseId") Long courseId,
                                  @Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("limit") int limit);

    List<IdeologyResource> findPublishedByKeywords(@Param("courseId") Long courseId,
                                                   @Param("keywords") List<String> keywords,
                                                   @Param("limit") int limit);

    List<IdeologyResource> findByCreatorId(@Param("creatorId") Long creatorId, @Param("limit") int limit);
}
