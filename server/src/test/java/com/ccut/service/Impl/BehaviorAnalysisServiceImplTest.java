package com.ccut.service.Impl;

import com.ccut.dto.BehaviorAnalysisResult;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BehaviorAnalysisServiceImplTest {

    private BehaviorAnalysisServiceImpl service;
    private StudentBehaviorProfileMapper profileMapper;
    private LearningPathRecordMapper pathRecordMapper;
    private LearningProgressMapper learningProgressMapper;
    private StudentMapper studentMapper;
    private ConversationMapper conversationMapper;
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;
    private EnrollmentMapper enrollmentMapper;

    @BeforeEach
    void setUp() {
        service = new BehaviorAnalysisServiceImpl();
        profileMapper = mock(StudentBehaviorProfileMapper.class);
        pathRecordMapper = mock(LearningPathRecordMapper.class);
        learningProgressMapper = mock(LearningProgressMapper.class);
        studentMapper = mock(StudentMapper.class);
        conversationMapper = mock(ConversationMapper.class);
        ideologyRecommendationMapper = mock(IdeologyResourceRecommendationMapper.class);
        enrollmentMapper = mock(EnrollmentMapper.class);

        ReflectionTestUtils.setField(service, "profileMapper", profileMapper);
        ReflectionTestUtils.setField(service, "pathRecordMapper", pathRecordMapper);
        ReflectionTestUtils.setField(service, "learningProgressMapper", learningProgressMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "ideologyRecommendationMapper", ideologyRecommendationMapper);
        ReflectionTestUtils.setField(service, "enrollmentMapper", enrollmentMapper);
        ReflectionTestUtils.setField(service, "videoProgressMapper", mock(VideoProgressMapper.class));
        ReflectionTestUtils.setField(service, "documentProgressMapper", mock(DocumentProgressMapper.class));
    }

    @Test
    void analyzeCourseBehaviorReturnsActualResultsForEnrolledStudents() {
        Long courseId = 100L;
        Student student1 = student(1L, "student-one", "学生一", "Class A");
        Student student2 = student(2L, "student-two", "学生二", "Class A");

        when(enrollmentMapper.findStudentsByCourseId(courseId)).thenReturn(List.of(student1, student2));
        when(profileMapper.selectByStudentId(anyLong())).thenReturn(null);
        when(studentMapper.selectById(1L)).thenReturn(student1);
        when(studentMapper.selectById(2L)).thenReturn(student2);
        when(ideologyRecommendationMapper.findByStudentId(anyLong(), anyInt())).thenReturn(List.of());
        when(learningProgressMapper.getWeeklyStudyTime(anyLong())).thenReturn(28800);
        when(learningProgressMapper.getConsecutiveDays(anyLong())).thenReturn(3);

        List<LearningPathRecord> records1 = List.of(
                record(1L, courseId, "video", "complete", 100.0, LocalDateTime.now().minusDays(2)),
                record(1L, courseId, "document", "view", 70.0, LocalDateTime.now().minusDays(1))
        );
        List<LearningPathRecord> records2 = List.of(
                record(2L, courseId, "interactive", "complete", 100.0, LocalDateTime.now().minusDays(3)),
                record(2L, courseId, "video", "view", 40.0, LocalDateTime.now().minusDays(2))
        );

        when(pathRecordMapper.selectByStudentId(eq(1L), isNull(), eq(100))).thenReturn(records1);
        when(pathRecordMapper.selectByStudentId(eq(2L), isNull(), eq(100))).thenReturn(records2);
        when(pathRecordMapper.selectRecentByStudentId(1L, 100)).thenReturn(records1);
        when(pathRecordMapper.selectRecentByStudentId(2L, 100)).thenReturn(records2);
        when(pathRecordMapper.countByStudentAndResource(eq(1L), eq("video"), any(LocalDateTime.class))).thenReturn(1);
        when(pathRecordMapper.countByStudentAndResource(eq(1L), eq("document"), any(LocalDateTime.class))).thenReturn(1);
        when(pathRecordMapper.countByStudentAndResource(eq(1L), eq("ideology"), any(LocalDateTime.class))).thenReturn(0);
        when(pathRecordMapper.countByStudentAndResource(eq(2L), eq("video"), any(LocalDateTime.class))).thenReturn(1);
        when(pathRecordMapper.countByStudentAndResource(eq(2L), eq("document"), any(LocalDateTime.class))).thenReturn(0);
        when(pathRecordMapper.countByStudentAndResource(eq(2L), eq("ideology"), any(LocalDateTime.class))).thenReturn(1);
        when(pathRecordMapper.sumDurationByStudent(eq(1L), isNull(), any(LocalDateTime.class))).thenReturn(3600);
        when(pathRecordMapper.sumDurationByStudent(eq(2L), isNull(), any(LocalDateTime.class))).thenReturn(5400);
        when(pathRecordMapper.countByStudentAndResource(eq(1L), isNull(), any(LocalDateTime.class))).thenReturn(2);
        when(pathRecordMapper.countByStudentAndResource(eq(2L), isNull(), any(LocalDateTime.class))).thenReturn(2);
        when(conversationMapper.findByStudentId(1L)).thenReturn(List.of(new Conversation()));
        when(conversationMapper.findByStudentId(2L)).thenReturn(List.of(new Conversation(), new Conversation()));

        List<BehaviorAnalysisResult> results = service.analyzeCourseBehavior(courseId);

        assertEquals(2, results.size());
        assertEquals(List.of(1L, 2L), results.stream().map(BehaviorAnalysisResult::getStudentId).toList());
        assertTrue(results.stream().allMatch(result -> result.getCompletionRate() != null && result.getCompletionRate() > 0.6));
        assertTrue(results.stream().allMatch(result -> result.getConsistencyScore() != null && !result.getConsistencyScore().equals(60.0)));
    }

    @Test
    void batchUpdateCourseProfilesRefreshesEveryEnrolledStudent() {
        Long courseId = 200L;
        Student student1 = student(11L, "student-eleven", "学生十一", "Class B");
        Student student2 = student(12L, "student-twelve", "学生十二", "Class B");

        when(enrollmentMapper.findStudentsByCourseId(courseId)).thenReturn(List.of(student1, student2));
        when(profileMapper.selectByStudentId(anyLong())).thenReturn(null);
        when(studentMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long studentId = invocation.getArgument(0);
            return studentId.equals(11L) ? student1 : student2;
        });
        when(ideologyRecommendationMapper.findByStudentId(anyLong(), anyInt())).thenReturn(List.of());
        when(learningProgressMapper.getWeeklyStudyTime(anyLong())).thenReturn(0);
        when(learningProgressMapper.getConsecutiveDays(anyLong())).thenReturn(0);
        when(pathRecordMapper.selectByStudentId(anyLong(), isNull(), anyInt())).thenReturn(List.of());
        when(pathRecordMapper.selectRecentByStudentId(anyLong(), anyInt())).thenReturn(List.of());
        when(pathRecordMapper.sumDurationByStudent(anyLong(), isNull(), any(LocalDateTime.class))).thenReturn(0);
        when(pathRecordMapper.countByStudentAndResource(anyLong(), any(), any(LocalDateTime.class))).thenReturn(0);
        when(conversationMapper.findByStudentId(anyLong())).thenReturn(List.of());

        service.batchUpdateCourseProfiles(courseId);

        verify(profileMapper, times(2)).upsert(any(StudentBehaviorProfile.class));
    }

    private Student student(Long id, String username, String name, String className) {
        Student student = new Student();
        student.setId(id);
        student.setUsername(username);
        student.setName(name);
        student.setClassName(className);
        return student;
    }

    private LearningPathRecord record(Long studentId, Long courseId, String resourceType,
                                      String actionType, Double progressPercent, LocalDateTime createdAt) {
        LearningPathRecord record = new LearningPathRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setResourceType(resourceType);
        record.setActionType(actionType);
        record.setProgressPercent(progressPercent);
        record.setCreatedAt(createdAt);
        return record;
    }
}
