package com.ccut.service.Impl;

import com.ccut.dto.EvaluationResult;
import com.ccut.dto.StudentMemberScore;
import com.ccut.entity.*;
import com.ccut.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluationServiceImplTest {

    private EvaluationServiceImpl service;
    private TeachingEvaluationMapper evaluationMapper;
    private LearningProgressMapper learningProgressMapper;
    private ConversationMapper conversationMapper;
    private CourseMapper courseMapper;
    private StudentMapper studentMapper;
    private EnrollmentMapper enrollmentMapper;
    private IdeologyResourceMapper ideologyResourceMapper;
    private IdeologyResourceRecommendationMapper ideologyRecommendationMapper;
    private LearningPathRecordMapper pathRecordMapper;
    private TeacherAssignmentMapper teacherAssignmentMapper;
    private StudentSubmissionMapper studentSubmissionMapper;
    private StudentMemberScoreMapper studentMemberScoreMapper;
    private WrongQuestionMapper wrongQuestionMapper;

    @BeforeEach
    void setUp() {
        service = new EvaluationServiceImpl();
        evaluationMapper = mock(TeachingEvaluationMapper.class);
        learningProgressMapper = mock(LearningProgressMapper.class);
        conversationMapper = mock(ConversationMapper.class);
        courseMapper = mock(CourseMapper.class);
        studentMapper = mock(StudentMapper.class);
        enrollmentMapper = mock(EnrollmentMapper.class);
        ideologyResourceMapper = mock(IdeologyResourceMapper.class);
        ideologyRecommendationMapper = mock(IdeologyResourceRecommendationMapper.class);
        pathRecordMapper = mock(LearningPathRecordMapper.class);
        teacherAssignmentMapper = mock(TeacherAssignmentMapper.class);
        studentSubmissionMapper = mock(StudentSubmissionMapper.class);
        studentMemberScoreMapper = mock(StudentMemberScoreMapper.class);
        wrongQuestionMapper = mock(WrongQuestionMapper.class);

        ReflectionTestUtils.setField(service, "evaluationMapper", evaluationMapper);
        ReflectionTestUtils.setField(service, "learningProgressMapper", learningProgressMapper);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "enrollmentMapper", enrollmentMapper);
        ReflectionTestUtils.setField(service, "ideologyResourceMapper", ideologyResourceMapper);
        ReflectionTestUtils.setField(service, "ideologyRecommendationMapper", ideologyRecommendationMapper);
        ReflectionTestUtils.setField(service, "pathRecordMapper", pathRecordMapper);
        ReflectionTestUtils.setField(service, "teacherAssignmentMapper", teacherAssignmentMapper);
        ReflectionTestUtils.setField(service, "studentSubmissionMapper", studentSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMemberScoreMapper", studentMemberScoreMapper);
        ReflectionTestUtils.setField(service, "wrongQuestionMapper", wrongQuestionMapper);
    }

    @Test
    void evaluateStudentUsesLearningPathConversationsAssignmentsAndWrongQuestions() {
        Long studentId = 7L;
        Long courseId = 9L;

        Course course = new Course();
        course.setCourseId(courseId);
        course.setCourseName("课程思政");
        course.setTeacherId(88L);
        Student student = new Student();
        student.setId(studentId);
        student.setName("学生七");

        when(courseMapper.selectById(courseId)).thenReturn(course);
        when(studentMapper.selectById(studentId)).thenReturn(student);
        when(ideologyResourceMapper.search(courseId, null, "published", 100)).thenReturn(List.of(resource(courseId, "家国情怀")));
        when(ideologyRecommendationMapper.findByStudentId(studentId, 50)).thenReturn(List.of());
        when(learningProgressMapper.findOne(studentId, courseId)).thenReturn(progress(80.0, 7200));
        when(pathRecordMapper.selectByStudentId(studentId, courseId, 100)).thenReturn(List.of(
                pathRecord(studentId, courseId, "video", "interact", 100.0),
                pathRecord(studentId, courseId, "document", "review", 100.0)
        ));
        when(conversationMapper.findByStudentId(studentId)).thenReturn(List.of(new Conversation(), new Conversation()));
        when(teacherAssignmentMapper.selectByTeacherId(88L)).thenReturn(List.of(assignment(301L)));
        when(studentSubmissionMapper.selectByAssignmentId(301L)).thenReturn(List.of(submission(901L, studentId)));
        when(studentMemberScoreMapper.selectBySubmissionId(901L)).thenReturn(List.of(
                new StudentMemberScore(1L, 901L, studentId, "学生七", 92, "优秀", "完成度高，反思充分", LocalDateTime.now())
        ));
        when(wrongQuestionMapper.selectByStudentId(studentId, courseId)).thenReturn(List.of(
                wrongQuestion(false, false),
                wrongQuestion(true, true)
        ));

        EvaluationResult result = service.evaluateStudent(studentId, courseId, "weekly");

        assertEquals(studentId, result.getStudentId());
        assertEquals(courseId, result.getCourseId());
        assertTrue(result.getInteractionScore() > 50.0);
        assertTrue(result.getValueRecognitionScore() > 50.0);
        assertNotNull(result.getInteractionAnalysis());
        assertNotNull(result.getValueAnalysis());
    }

    private LearningProgress progress(Double completion, Integer timeSpent) {
        LearningProgress progress = new LearningProgress();
        progress.setCompletionPercentage(completion);
        progress.setTimeSpent(timeSpent);
        return progress;
    }

    private IdeologyResource resource(Long courseId, String theme) {
        IdeologyResource resource = new IdeologyResource();
        resource.setCourseId(courseId);
        resource.setStatus("published");
        resource.setValueTheme(theme);
        return resource;
    }

    private LearningPathRecord pathRecord(Long studentId, Long courseId, String resourceType, String actionType, Double progressPercent) {
        LearningPathRecord record = new LearningPathRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setResourceType(resourceType);
        record.setActionType(actionType);
        record.setProgressPercent(progressPercent);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private TeacherAssignment assignment(Long assignmentId) {
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setAssignmentId(assignmentId);
        return assignment;
    }

    private StudentSubmission submission(Long submissionId, Long submittedBy) {
        StudentSubmission submission = new StudentSubmission();
        submission.setSubmissionId(submissionId);
        submission.setSubmittedBy(submittedBy);
        return submission;
    }

    private WrongQuestion wrongQuestion(boolean mastered, boolean correctCountHigh) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setIsMastered(mastered);
        wrongQuestion.setCorrectCount(correctCountHigh ? 3 : 0);
        wrongQuestion.setWrongCount(correctCountHigh ? 1 : 2);
        return wrongQuestion;
    }
}
