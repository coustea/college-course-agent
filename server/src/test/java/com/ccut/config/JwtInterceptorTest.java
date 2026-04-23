package com.ccut.config;

import com.ccut.context.UserContext;
import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.utils.JWTUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtInterceptorTest {

    private JwtInterceptor interceptor;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        JWTUtils jwtUtils = new JWTUtils();
        ReflectionTestUtils.setField(jwtUtils, "injectedSignKey",
                "TEST_JWT_SECRET_KEY_2026_HS512_ALGORITHM_MUST_BE_AT_LEAST_64_BYTES_LONG");
        jwtUtils.init();

        userMapper = mock(UserMapper.class);
        interceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(interceptor, "userMapper", userMapper);
        ReflectionTestUtils.setField(interceptor, "jwtEnabled", true);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void preHandleSetsCurrentUserContextForValidToken() throws Exception {
        String token = JWTUtils.generateToken("student1");
        User user = new User(42L, "student1", "secret", User.Role.student, token);
        when(userMapper.getUserByUsername("student1")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/progress/course");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));

        UserContext.Context context = UserContext.get();
        assertNotNull(context);
        assertEquals(42L, context.userId());
        assertEquals("student1", context.username());
        assertEquals(User.Role.student, context.role());
    }

    @Test
    void afterCompletionClearsCurrentUserContext() throws Exception {
        UserContext.set(42L, "student1", User.Role.student);

        interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), null);

        assertNull(UserContext.get());
    }
}
