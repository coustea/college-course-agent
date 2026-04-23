package com.ccut.service.Impl;

import com.ccut.entity.*;
import com.ccut.mapper.*;
import com.ccut.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValueAssessmentServiceImplTest {

    private ValueAssessmentServiceImpl service;
    private ValueAssessmentMapper valueAssessmentMapper;
    private TeachingStyleProfileMapper teachingStyleMapper;
    private StudentMapper studentMapper;
    private EnrollmentMapper enrollmentMapper;
    private CourseMapper courseMapper;
    private IdeologyResourceMapper ideologyResourceMapper;
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;
    private LearningProgressMapper learningProgressMapper;
    private EvaluationService evaluationService;
    private ConversationMapper conversationMapper;
    private LearningPathRecordMapper pathRecordMapper;
    private WrongQuestionMapper wrongQuestionMapper;

    @BeforeEach
    void setUp() {
        service = new ValueAssessmentServiceImpl();
        valueAssessmentMapper = mock(ValueAssessmentMapper.class);
        teachingStyleMapper = mock(TeachingStyleProfileMapper.class);
        studentMapper = mock(StudentMapper.class);
        enrollmentMapper = mock(EnrollmentMapper.class);
        courseMapper = mock(CourseMapper.class);
        ideologyResourceMapper = mock(IdeologyResourceMapper.class);
        ideologyRecommendationMapper = mock(IdeologyResourceRecommendationMapper.class);
        learningProgressMapper = mock(LearningProgressMapper.class);
        evaluationService = mock(EvaluationService.class);
        conversationMapper = mock(ConversationMapper.class);
        pathRecordMapper = mock(LearningPathRecordMapper.class);
        wrongQuestionMapper = mock(WrongQuestionMapper.class);

        ReflectionTestUtils.setField(service, "valueAssessmentMapper", valueAssessmentMapper);
        ReflectionTestUtils.setField(service, "teachingStyleMapper", teachingStyleMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "enrollmentMapper", enrollmentMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "ideologyResourceMapper", ideologyResourceMapper);
        ReflectionTestUtils.setField(service, "ideologyRecommendationMapper", ideologyRecommendationMapper);
        ReflectionTestUtils.setField(service, "learningProgressMapper", learningProgressMapper);
        ReflectionTestUtils.setField(service, "evaluationService", evaluationService);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "pathRecordMapper", pathRecordMapper);
        ReflectionTestUtils.setField(service, "wrongQuestionMapper", wrongQuestionMapper);
    }

    @Test
    void assessStudentValueUsesBehaviorSignalsForInnovation() {
        Long studentId = 4L;
        Long courseId = 8L;

        when(ideologyRecommendationMapper.findByStudentId(studentId, 100)).thenReturn(List.of(
                recommendation(true), recommendation(false)
        ));
        when(valueAssessmentMapper.findLatestByStudentAndCourse(studentId, courseId)).thenReturn(null);
        when(learningProgressMapper.findOne(studentId, courseId)).thenReturn(progress(90.0, 5400));
        when(conversationMapper.findByStudentId(studentId)).thenReturn(List.of(new Conversation(), new Conversation()));
        when(pathRecordMapper.selectByStudentId(studentId, courseId, 100)).thenReturn(List.of(
                pathRecord(studentId, courseId, "video", "interact"),
                pathRecord(studentId, courseId, "document", "review")
        ));
        when(pathRecordMapper.selectRecentByStudentId(studentId, 100)).thenReturn(List.of());
        when(pathRecordMapper.countByStudentAndResource(eq(studentId), any(), any(LocalDateTime.class))).thenReturn(0);
        when(pathRecordMapper.sumDurationByStudent(eq(studentId), eq(courseId), any(LocalDateTime.class))).thenReturn(5400);
        when(wrongQuestionMapper.selectByStudentId(studentId, courseId)).thenReturn(List.of(
                wrongQuestion(false, true),
                wrongQuestion(true, true)
        ));

        ValueAssessment assessment = service.assessStudentValue(studentId, courseId, "monthly");

        assertEquals(studentId, assessment.getStudentId());
        assertEquals(courseId, assessment.getCourseId());
        assertNotEquals(60.0, assessment.getInnovationScore());
        assertTrue(assessment.getInnovationScore() > 60.0);
    }

    @Test
    void analyzeTeachingStyleDerivesScoresFromCourseAndProgressData() {
        Long teacherId = 19L;

        Course course1 = new Course();
        course1.setCourseId(101L);
        course1.setCourseName("课程一");
        course1.setTeacherId(teacherId);
        Course course2 = new Course();
        course2.setCourseId(102L);
        course2.setCourseName("课程二");
        course2.setTeacherId(teacherId);

        Student student1 = new Student();
        student1.setId(301L);
        Student student2 = new Student();
        student2.setId(302L);

        when(teachingStyleMapper.findByTeacherId(teacherId)).thenReturn(null);
        when(courseMapper.selectByTeacherId(teacherId)).thenReturn(List.of(course1, course2));
        when(ideologyResourceMapper.findByCreatorId(teacherId, 1000)).thenReturn(List.of(
                ideologyResource(101L, "家国情怀", "video"),
                ideologyResource(102L, "科学精神", "document")
        ));
        when(ideologyResourceMapper.search(eq(101L), any(), eq("published"), anyInt())).thenReturn(List.of(
                ideologyResource(101L, "家国情怀", "video")
        ));
        when(ideologyResourceMapper.search(eq(102L), any(), eq("published"), anyInt())).thenReturn(List.of(
                ideologyResource(102L, "科学精神", "document")
        ));
        when(enrollmentMapper.findStudentsByCourseId(101L)).thenReturn(List.of(student1));
        when(enrollmentMapper.findStudentsByCourseId(102L)).thenReturn(List.of(student2));
        when(learningProgressMapper.findOne(301L, 101L)).thenReturn(progress(90.0, 7200));
        when(learningProgressMapper.findOne(302L, 102L)).thenReturn(progress(60.0, 3600));
        when(conversationMapper.findByStudentId(301L)).thenReturn(List.of(new Conversation(), new Conversation()));
        when(conversationMapper.findByStudentId(302L)).thenReturn(List.of(new Conversation()));
        when(pathRecordMapper.selectByStudentId(anyLong(), anyLong(), anyInt())).thenReturn(List.of());
        when(pathRecordMapper.selectRecentByStudentId(anyLong(), anyInt())).thenReturn(List.of());
        when(pathRecordMapper.countByStudentAndResource(anyLong(), any(), any(LocalDateTime.class))).thenReturn(0);
        when(pathRecordMapper.sumDurationByStudent(anyLong(), any(), any(LocalDateTime.class))).thenReturn(0);

        TeachingStyleProfile profile = service.analyzeTeachingStyle(teacherId);

        assertEquals(teacherId, profile.getTeacherId());
        assertNotEquals(55.0, profile.getInnovation());
        assertNotEquals(70.0, profile.getInteractionLevel());
        assertNotNull(profile.getStyleTag());
    }

    private IdeologyResourceRecommendation recommendation(boolean clicked) {
        IdeologyResourceRecommendation recommendation = new IdeologyResourceRecommendation();
        recommendation.setHasClicked(clicked);
        return recommendation;
    }

    private LearningProgress progress(Double completion, Integer timeSpent) {
        LearningProgress progress = new LearningProgress();
        progress.setCompletionPercentage(completion);
        progress.setTimeSpent(timeSpent);
        return progress;
    }

    private LearningPathRecord pathRecord(Long studentId, Long courseId, String resourceType, String actionType) {
        LearningPathRecord record = new LearningPathRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setResourceType(resourceType);
        record.setActionType(actionType);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private WrongQuestion wrongQuestion(boolean mastered, boolean reviewed) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setIsMastered(mastered);
        wrongQuestion.setCorrectCount(reviewed ? 2 : 0);
        wrongQuestion.setWrongCount(reviewed ? 1 : 2);
        return wrongQuestion;
    }

    private IdeologyResource ideologyResource(Long courseId, String theme, String type) {
        IdeologyResource resource = new IdeologyResource();
        resource.setCourseId(courseId);
        resource.setValueTheme(theme);
        resource.setResourceType(type);
        resource.setStatus("published");
        return resource;
    }
}
