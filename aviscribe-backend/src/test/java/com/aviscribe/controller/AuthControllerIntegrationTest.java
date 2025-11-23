package com.aviscribe.controller;

import com.aviscribe.dto.AuthResponse;
import com.aviscribe.dto.UserProfileDTO;
import com.aviscribe.entity.Task;
import com.aviscribe.entity.User;
import com.aviscribe.security.RateLimitProperties;
import com.aviscribe.security.RateLimitService;
import com.aviscribe.service.TaskService;
import com.aviscribe.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "aviscribe.security.rate-limit.auth.max-requests=50",
        "aviscribe.security.rate-limit.auth.window-seconds=60"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private RateLimitProperties rateLimitProperties;

    @BeforeEach
    void setup() {
        rateLimitService.reset();
        rateLimitProperties.getAuth().setMaxRequests(50);
        rateLimitProperties.getAuth().setWindowSeconds(60);
    }

    @AfterEach
    void cleanup() {
        rateLimitService.reset();
    }

    @Test
    void shouldRegisterLoginAndFetchProfile() throws Exception {
        String username = randomUsername();
        AuthResponse registerResponse = register(username, "secret123", "Tester");
        assertThat(registerResponse.getAccessToken()).isNotBlank();

        AuthResponse loginResponse = login(username, "secret123");
        assertThat(loginResponse.getAccessToken()).isNotBlank();

        UserProfileDTO profile = fetchProfile(loginResponse.getAccessToken());
        assertThat(profile.getUsername()).isEqualTo(username);
    }

    @Test
    void userCannotAccessOthersTask() throws Exception {
        String usernameA = randomUsername();
        String usernameB = randomUsername();
        AuthResponse userA = register(usernameA, "passwordA", "UserA");
        register(usernameB, "passwordB", "UserB");

        User dbUserA = userService.findByUsername(usernameA);
        User dbUserB = userService.findByUsername(usernameB);
        Task taskForB = createTaskForUser(dbUserB.getId(), "UserB Task");
        Task taskForA = createTaskForUser(dbUserA.getId(), "UserA Task");

        mockMvc.perform(get("/v1/task/" + taskForB.getId())
                        .header("Authorization", "Bearer " + userA.getAccessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/task/" + taskForA.getId())
                        .header("Authorization", "Bearer " + userA.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void rateLimiterBlocksRapidRegistrations() throws Exception {
        rateLimitProperties.getAuth().setMaxRequests(2);
        rateLimitProperties.getAuth().setWindowSeconds(60);
        rateLimitService.reset();

        register(randomUsername(), "secret1", "User1");
        register(randomUsername(), "secret2", "User2");

        mockMvc.perform(post("/v1/auth/register")
                        .with(sameClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", randomUsername(),
                                "password", "secret3",
                                "displayName", "User3"
                        ))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void userCanUpdateProfile() throws Exception {
        String username = randomUsername();
        AuthResponse auth = register(username, "secret123", "OldName");

        mockMvc.perform(post("/v1/auth/profile")
                        .with(bearer(auth.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "NewName",
                                "phone", "18800001111"
                        ))))
                .andExpect(status().isOk());

        UserProfileDTO profile = fetchProfile(auth.getAccessToken());
        assertThat(profile.getDisplayName()).isEqualTo("NewName");
        assertThat(profile.getPhone()).isEqualTo("18800001111");
    }

    @Test
    void userCanChangePassword() throws Exception {
        String username = randomUsername();
        register(username, "oldPass1", "UserCP");

        AuthResponse login = login(username, "oldPass1");

        mockMvc.perform(post("/v1/auth/change-password")
                        .with(bearer(login.getAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", "oldPass1",
                                "newPassword", "newPass1"
                        ))))
                .andExpect(status().isOk());

        login(username, "newPass1");
    }

    private AuthResponse register(String username, String password, String displayName) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("displayName", displayName);
        String response = mockMvc.perform(post("/v1/auth/register")
                        .with(sameClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class);
    }

    private AuthResponse login(String username, String password) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        String response = mockMvc.perform(post("/v1/auth/login")
                        .with(sameClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class);
    }

    private UserProfileDTO fetchProfile(String accessToken) throws Exception {
        String response = mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, UserProfileDTO.class);
    }

    private Task createTaskForUser(Long userId, String name) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTaskName(name);
        task.setSourceType(1);
        task.setTaskStatus(1);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskService.save(task);
        return task;
    }

    private String randomUsername() {
        return ("user" + UUID.randomUUID()).replace("-", "").substring(0, 16);
    }

    private RequestPostProcessor sameClient() {
        return request -> {
            request.setRemoteAddr("127.0.0.1");
            return request;
        };
    }

    private RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
