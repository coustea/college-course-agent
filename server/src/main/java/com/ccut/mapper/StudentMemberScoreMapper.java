package com.ccut.mapper;

import com.ccut.dto.StudentMemberScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentMemberScoreMapper {

	int insert(StudentMemberScore score);

	int updateBySubmissionAndStudent(@Param("submissionId") Long submissionId,
	                                 @Param("studentId") Long studentId,
	                                 @Param("score") Integer score,
	                                 @Param("level") String level,
	                                 @Param("feedback") String feedback,
	                                 @Param("gradedAt") java.time.LocalDateTime gradedAt);

	int insertWithTeacherName(@Param("submissionId") Long submissionId,
	                          @Param("studentId") Long studentId,
	                          @Param("teacherName") String teacherName,
	                          @Param("score") Integer score,
	                          @Param("level") String level,
	                          @Param("feedback") String feedback,
	                          @Param("gradedAt") java.time.LocalDateTime gradedAt);

	int updateBySubmissionAndStudentWithTeacherName(@Param("submissionId") Long submissionId,
	                                               @Param("studentId") Long studentId,
	                                               @Param("teacherName") String teacherName,
	                                               @Param("score") Integer score,
	                                               @Param("level") String level,
	                                               @Param("feedback") String feedback,
	                                               @Param("gradedAt") java.time.LocalDateTime gradedAt);

	java.util.List<StudentMemberScore> selectBySubmissionId(@Param("submissionId") Long submissionId);

}


