package com.ccut.controller;

import com.ccut.entity.Result;
import com.ccut.entity.StudentSubmission;
import com.ccut.mapper.StudentSubmissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studentSubmissions")
@Slf4j
public class StudentSubmissionController {

	@Autowired
	private StudentSubmissionMapper studentSubmissionMapper;

	@GetMapping("/by-assignment/{assignmentId}")
	public Result<List<StudentSubmission>> listByAssignment(@PathVariable Long assignmentId) {
		try {
			if (assignmentId == null) return Result.error(400, "assignmentId 不能为空");
			return Result.success(studentSubmissionMapper.selectByAssignmentId(assignmentId));
		} catch (Exception e) {
			log.error("查询小组提交失败: assignmentId={}", assignmentId, e);
			return Result.error(500, e.getMessage());
		}
	}

	@GetMapping("/by-group/{groupId}")
	public Result<List<StudentSubmission>> listByGroup(@PathVariable Long groupId) {
		try {
			if (groupId == null) return Result.error(400, "groupId 不能为空");
			return Result.success(studentSubmissionMapper.selectByGroupId(groupId));
		} catch (Exception e) {
			log.error("查询小组提交失败: groupId={}", groupId, e);
			return Result.error(500, e.getMessage());
		}
	}
}


