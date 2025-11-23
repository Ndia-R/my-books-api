package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.user.UserResponse;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.UserService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void testGetAllUsers_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void testGetAllUsers_一般ユーザーでアクセス_403Forbidden() throws Exception {
        // When & Then - ADMINロールなし
        mockMvc.perform(get("/admin/users")
                .with(jwt().jwt(jwt -> jwt.subject("user-id-1"))))
            .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void testGetAllUsers_ADMINロールでアクセス_正常系() throws Exception {
        // Given
        UserResponse user1 = new UserResponse();
        user1.setId("user-id-1");
        user1.setDisplayName("User One");

        UserResponse user2 = new UserResponse();
        user2.setId("user-id-2");
        user2.setDisplayName("User Two");

        List<UserResponse> users = Arrays.asList(user1, user2);

        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("user-id-1"))
            .andExpect(jsonPath("$[0].displayName").value("User One"))
            .andExpect(jsonPath("$[1].id").value("user-id-2"))
            .andExpect(jsonPath("$[1].displayName").value("User Two"));

        verify(userService).getAllUsers();
    }

    @Test
    void testGetAllUsers_ADMINロールでアクセス_空リスト() throws Exception {
        // Given
        when(userService.getAllUsers()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(userService).getAllUsers();
    }

    @Test
    void testGetUserById_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String userId = "user-id-1";

        // When & Then
        mockMvc.perform(get("/admin/users/{id}", userId))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserById(anyString());
    }

    @Test
    void testGetUserById_一般ユーザーでアクセス_403Forbidden() throws Exception {
        // Given
        String userId = "user-id-1";

        // When & Then
        mockMvc.perform(get("/admin/users/{id}", userId)
                .with(jwt().jwt(jwt -> jwt.subject("user-id-1"))))
            .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyString());
    }

    @Test
    void testGetUserById_ADMINロールでアクセス_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        UserResponse user = new UserResponse();
        user.setId(userId);
        user.setDisplayName("Test User");
        user.setAvatarPath("/avatars/test.png");

        when(userService.getUserById(userId)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/admin/users/{id}", userId)
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.displayName").value("Test User"))
            .andExpect(jsonPath("$.avatarPath").value("/avatars/test.png"));

        verify(userService).getUserById(userId);
    }

    @Test
    void testGetUserById_存在しないユーザー_404NotFound() throws Exception {
        // Given
        String nonExistentUserId = "non-existent-user-id";

        when(userService.getUserById(nonExistentUserId))
            .thenThrow(new NotFoundException("ユーザーが見つかりません"));

        // When & Then
        mockMvc.perform(get("/admin/users/{id}", nonExistentUserId)
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isNotFound());

        verify(userService).getUserById(nonExistentUserId);
    }

    @Test
    void testDeleteUser_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String userId = "user-id-1";

        // When & Then
        mockMvc.perform(delete("/admin/users/{id}", userId))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    void testDeleteUser_一般ユーザーでアクセス_403Forbidden() throws Exception {
        // Given
        String userId = "user-id-1";

        // When & Then
        mockMvc.perform(delete("/admin/users/{id}", userId)
                .with(jwt().jwt(jwt -> jwt.subject("user-id-1"))))
            .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    void testDeleteUser_ADMINロールでアクセス_正常系() throws Exception {
        // Given
        String userId = "user-id-to-delete";

        doNothing().when(userService).deleteUser(userId);

        // When & Then
        mockMvc.perform(delete("/admin/users/{id}", userId)
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    void testDeleteUser_存在しないユーザー_削除成功() throws Exception {
        // Given
        String nonExistentUserId = "non-existent-user-id";

        // UserServiceがNotFoundExceptionをスローしない場合（deleteByIdの動作）
        doNothing().when(userService).deleteUser(nonExistentUserId);

        // When & Then
        mockMvc.perform(delete("/admin/users/{id}", nonExistentUserId)
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isNoContent());

        verify(userService).deleteUser(nonExistentUserId);
    }

    @Test
    void testGetAllUsers_複数のADMINロール() throws Exception {
        // Given
        UserResponse user = new UserResponse();
        user.setId("user-id-1");
        user.setDisplayName("User");

        when(userService.getAllUsers()).thenReturn(Arrays.asList(user));

        // When & Then - ROLE_ADMINとROLE_USERの両方を持つ
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ROLE_ADMIN", () -> "ROLE_USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(userService).getAllUsers();
    }

    @Test
    void testGetUserById_特殊文字を含むUUID() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        UserResponse user = new UserResponse();
        user.setId(userId);
        user.setDisplayName("User");

        when(userService.getUserById(userId)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/admin/users/{id}", userId)
                .with(jwt().authorities(() -> "ROLE_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId));

        verify(userService).getUserById(userId);
    }

    @Test
    void testAdminEndpoints_ROLE_ADMINが必要() throws Exception {
        // 異なるロール名でアクセスを試みる

        // ROLE_MANAGER（存在しないロール）
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ROLE_MANAGER")))
            .andExpect(status().isForbidden());

        // ROLE_USER
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ROLE_USER")))
            .andExpect(status().isForbidden());

        // ADMIN（プレフィックスなし）
        mockMvc.perform(get("/admin/users")
                .with(jwt().authorities(() -> "ADMIN")))
            .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }
}
