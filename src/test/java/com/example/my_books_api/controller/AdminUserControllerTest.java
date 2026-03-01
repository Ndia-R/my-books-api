package com.example.my_books_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_api.config.RoleConfig;
import com.example.my_books_api.config.SecurityConfig;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.service.UserService;

@WebMvcTest(AdminUserController.class)
@Import({ SecurityConfig.class, RoleConfig.class })
@SuppressWarnings("null")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private static final String TARGET_USER_ID = "user-uuid-001";

    private static final String VALID_REQUEST_BODY = """
        {"subscriptionPlan": "PREMIUM"}
        """;

    @Test
    @DisplayName("PUT /admin/users/{id}/subscription - 正常系: 200 OK")
    void updateUserSubscriptionPlan_Success_Returns200() throws Exception {
        // Given
        UserProfileResponse response = new UserProfileResponse();
        response.setId(TARGET_USER_ID);
        response.setSubscriptionPlan("PREMIUM");
        when(userService.updateUserSubscriptionPlan(eq(TARGET_USER_ID), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(
            put("/admin/users/{id}/subscription", TARGET_USER_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("user:manage:all")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/subscription - 未認証: 401 Unauthorized")
    void updateUserSubscriptionPlan_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(
            put("/admin/users/{id}/subscription", TARGET_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/subscription - 権限不足: 403 Forbidden")
    void updateUserSubscriptionPlan_InsufficientAuthority_Returns403() throws Exception {
        mockMvc.perform(
            put("/admin/users/{id}/subscription", TARGET_USER_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("user:read:own")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/subscription - ユーザーが存在しない: 404 Not Found")
    void updateUserSubscriptionPlan_UserNotFound_Returns404() throws Exception {
        // Given
        when(userService.updateUserSubscriptionPlan(eq(TARGET_USER_ID), any()))
            .thenThrow(new NotFoundException("User not found"));

        // When / Then
        mockMvc.perform(
            put("/admin/users/{id}/subscription", TARGET_USER_ID)
                .with(jwt().authorities(new SimpleGrantedAuthority("user:manage:all")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isNotFound());
    }
}
