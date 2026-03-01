package com.example.my_books_api.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.example.my_books_api.service.BookmarkService;
import com.example.my_books_api.service.FavoriteService;
import com.example.my_books_api.service.ReviewService;
import com.example.my_books_api.service.UserService;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, RoleConfig.class })
@SuppressWarnings("null")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private BookmarkService bookmarkService;

    private static final String VALID_REQUEST_BODY = """
        {"subscriptionPlan": "PREMIUM"}
        """;

    private static final String INVALID_REQUEST_BODY = """
        {"subscriptionPlan": "INVALID"}
        """;

    @Test
    @DisplayName("PUT /me/subscription - 正常系: 200 OK")
    void updateSubscriptionPlan_Success_Returns200() throws Exception {
        // Given
        UserProfileResponse response = new UserProfileResponse();
        response.setSubscriptionPlan("PREMIUM");
        when(userService.updateSubscriptionPlan(any())).thenReturn(response);

        // When / Then
        mockMvc.perform(
            put("/me/subscription")
                .with(jwt().authorities(new SimpleGrantedAuthority("user:update:own")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /me/subscription - 未認証: 401 Unauthorized")
    void updateSubscriptionPlan_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(
            put("/me/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /me/subscription - 権限不足: 403 Forbidden")
    void updateSubscriptionPlan_InsufficientAuthority_Returns403() throws Exception {
        mockMvc.perform(
            put("/me/subscription")
                .with(jwt().authorities(new SimpleGrantedAuthority("user:read:own")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST_BODY)
        )
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /me/subscription - 無効なプラン値: 400 Bad Request")
    void updateSubscriptionPlan_InvalidPlan_Returns400() throws Exception {
        mockMvc.perform(
            put("/me/subscription")
                .with(jwt().authorities(new SimpleGrantedAuthority("user:update:own")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_REQUEST_BODY)
        )
            .andExpect(status().isBadRequest());
    }
}
