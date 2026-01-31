package com.ccut.controller;

import com.ccut.dto.Result;
import com.ccut.dto.StudentMemberScore;
import com.ccut.entity.StudentSubmission;
import com.ccut.mapper.StudentMemberScoreMapper;
import com.ccut.mapper.StudentSubmissionMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/grading")
@Slf4j
public class TeacherGradingController {

	@Autowired
	private StudentMemberScoreMapper studentMemberScoreMapper;

	@Autowired
	private StudentSubmissionMapper studentSubmissionMapper;

	@Data
	public static class MemberGradeInput {
		private Long studentId;
		private Integer score;
		private String level;
		private String feedback;
	}

	@Data
	public static class GradeGroupRequest {
		private Long submissionId; // 小组提交ID
		private String teacherName; // 评分教师姓名
		private List<MemberGradeInput> members; // 每个成员评分
	}

	@Data
	public static class GroupGradesResponse {
		private List<StudentMemberScore> memberScores; // 成员评分列表
		private String groupComment; // 小组整体评语
	}

	@PostMapping("/group")
	public Result<String> gradeGroup(@RequestBody GradeGroupRequest body) {
		try {
			if (body == null || body.getSubmissionId() == null || body.getMembers() == null || body.getMembers().isEmpty()) {
				return Result.error(400, "参数不完整");
			}
			LocalDateTime now = LocalDateTime.now();
			for (MemberGradeInput m : body.getMembers()) {
				if (m.getStudentId() == null) continue;
				// 先尝试更新；若未命中（返回0），再插入
				int updated = (body.getTeacherName() == null || body.getTeacherName().isEmpty())
					? studentMemberScoreMapper.updateBySubmissionAndStudent(
							body.getSubmissionId(), m.getStudentId(), m.getScore(), m.getLevel(), m.getFeedback(), now)
					: studentMemberScoreMapper.updateBySubmissionAndStudentWithTeacherName(
							body.getSubmissionId(), m.getStudentId(), body.getTeacherName(), m.getScore(), m.getLevel(), m.getFeedback(), now);
				if (updated <= 0) {
					int inserted = (body.getTeacherName() == null || body.getTeacherName().isEmpty())
						? studentMemberScoreMapper.insertWithTeacherName(body.getSubmissionId(), m.getStudentId(), null, m.getScore(), m.getLevel(), m.getFeedback(), now)
						: studentMemberScoreMapper.insertWithTeacherName(body.getSubmissionId(), m.getStudentId(), body.getTeacherName(), m.getScore(), m.getLevel(), m.getFeedback(), now);
					if (inserted <= 0) {
						log.error("插入成员评分失败: submissionId={}, studentId={}", body.getSubmissionId(), m.getStudentId());
						return Result.error(500, "保存评分失败");
					}
				}
			}
			return Result.success("评分已保存");
		} catch (Exception e) {
			log.error("批改小组作品失败", e);
			return Result.error(500, e.getMessage());
		}
	}

	@GetMapping("/group/{submissionId}")
	public Result<GroupGradesResponse> getGroupGrades(@PathVariable Long submissionId) {
		try {
			if (submissionId == null) return Result.error(400, "submissionId 不能为空");
			
			// 查询成员评分
			List<StudentMemberScore> memberScores = studentMemberScoreMapper.selectBySubmissionId(submissionId);
			
			// 查询小组评语
			String groupComment = "";
			try {
				StudentSubmission submission = studentSubmissionMapper.selectById(submissionId);
				if (submission != null && submission.getGroupComment() != null) {
					groupComment = submission.getGroupComment();
				}
			} catch (Exception e) {
				log.warn("查询小组评语失败: submissionId={}", submissionId, e);
			}
			
			// 组装响应
			GroupGradesResponse response = new GroupGradesResponse();
			response.setMemberScores(memberScores);
			response.setGroupComment(groupComment);
			
			return Result.success(response);
		} catch (Exception e) {
			log.error("查询小组评分失败", e);
			return Result.error(500, e.getMessage());
		}
	}
}


