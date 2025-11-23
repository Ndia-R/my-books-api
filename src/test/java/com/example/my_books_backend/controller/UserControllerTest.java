package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.service.UserService;
import com.example.my_books_backend.util.JwtClaimExtractor;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
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

    @MockBean
    private JwtClaimExtractor jwtClaimExtractor;

    @Test
    void testGetUserProfile_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/me/profile"))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(userService, never()).getUserProfile(anyString());
    }

    @Test
    void testGetUserProfile_正常系_既存ユーザー() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        UserProfileResponse response = new UserProfileResponse();
        response.setId(userId);
        response.setEmail("test@example.com");
        response.setName("Test User");
        response.setDisplayName("Test Display");
        response.setAvatarPath("/avatars/test.png");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(userService.getUserProfile(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/profile")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.displayName").value("Test Display"))
            .andExpect(jsonPath("$.avatarPath").value("/avatars/test.png"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(userService).getUserProfile(userId);
    }

    @Test
    void testGetUserProfile_正常系_新規ユーザー自動作成() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        UserProfileResponse response = new UserProfileResponse();
        response.setId(userId);
        response.setDisplayName("User");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(userService.getUserProfile(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/profile")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(userService).getUserProfile(userId);
    }

    @Test
    void testGetUserProfileCounts_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/me/profile-counts"))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(userService, never()).getUserProfileCounts(anyString());
    }

    @Test
    void testGetUserProfileCounts_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        UserProfileCountsResponse response = new UserProfileCountsResponse(5L, 3L, 10L);

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(userService.getUserProfileCounts(userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/profile-counts")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.favoriteCount").value(5))
            .andExpect(jsonPath("$.bookmarkCount").value(3))
            .andExpect(jsonPath("$.reviewCount").value(10));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(userService).getUserProfileCounts(userId);
    }

    @Test
    void testGetUserReviews_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/me/reviews"))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(reviewService, never()).getUserReviews(anyString(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void testGetUserReviews_正常系_デフォルトパラメータ() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        PageResponse<ReviewResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new ReviewResponse()));

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.getUserReviews(userId, 1L, 5L, "updatedAt.desc", null))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/reviews")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).getUserReviews(userId, 1L, 5L, "updatedAt.desc", null);
    }

    @Test
    void testGetUserReviews_正常系_カスタムパラメータ() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        PageResponse<ReviewResponse> response = new PageResponse<>();

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.getUserReviews(userId, 2L, 10L, "rating.desc", null))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/reviews")
                .param("page", "2")
                .param("size", "10")
                .param("sort", "rating.desc")
                .with(jwt()))
            .andExpect(status().isOk());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).getUserReviews(userId, 2L, 10L, "rating.desc", null);
    }

    @Test
    void testGetUserReviews_正常系_bookIdフィルタあり() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "test-book-123";

        PageResponse<ReviewResponse> response = new PageResponse<>();

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.getUserReviews(userId, 1L, 5L, "updatedAt.desc", bookId))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/reviews")
                .param("bookId", bookId)
                .with(jwt()))
            .andExpect(status().isOk());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(reviewService).getUserReviews(userId, 1L, 5L, "updatedAt.desc", bookId);
    }

    @Test
    void testGetUserFavorites_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/me/favorites"))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(favoriteService, never()).getUserFavorites(anyString(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void testGetUserFavorites_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        PageResponse<FavoriteResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new FavoriteResponse()));

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(favoriteService.getUserFavorites(userId, 1L, 5L, "updatedAt.desc", null))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/favorites")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).getUserFavorites(userId, 1L, 5L, "updatedAt.desc", null);
    }

    @Test
    void testGetUserBookmarks_認証なしでアクセス_401Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/me/bookmarks"))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).getUserBookmarks(anyString(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void testGetUserBookmarks_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";

        PageResponse<BookmarkResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new BookmarkResponse()));

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.getUserBookmarks(userId, 1L, 5L, "updatedAt.desc", null))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/bookmarks")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).getUserBookmarks(userId, 1L, 5L, "updatedAt.desc", null);
    }

    @Test
    void testUpdateUserProfile_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String requestBody = "{\"displayName\":\"New Name\",\"avatarPath\":\"/avatars/new.png\"}";

        // When & Then
        mockMvc.perform(put("/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(userService, never()).updateUserProfile(any(UpdateUserProfileRequest.class), anyString());
    }

    @Test
    void testUpdateUserProfile_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"displayName\":\"Updated Name\",\"avatarPath\":\"/avatars/updated.png\"}";

        UserProfileResponse response = new UserProfileResponse();
        response.setId(userId);
        response.setDisplayName("Updated Name");
        response.setAvatarPath("/avatars/updated.png");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(userService.updateUserProfile(any(UpdateUserProfileRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.avatarPath").value("/avatars/updated.png"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(userService).updateUserProfile(any(UpdateUserProfileRequest.class), eq(userId));
    }

    @Test
    void testUpdateUserProfile_バリデーションエラー_空のdisplayName() throws Exception {
        // Given
        String requestBody = "{\"avatarPath\":\"/avatars/test.png\"}";

        // When & Then - displayNameがnullの場合バリデーションエラー
        mockMvc.perform(put("/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(userService, never()).updateUserProfile(any(UpdateUserProfileRequest.class), anyString());
    }

    @Test
    void testGetUserReviews_ソート条件の検証() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        PageResponse<ReviewResponse> response = new PageResponse<>();

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(reviewService.getUserReviews(eq(userId), anyLong(), anyLong(), anyString(), any()))
            .thenReturn(response);

        // When & Then - 各種ソート条件
        String[] sortOptions = {
            "updatedAt.asc", "updatedAt.desc",
            "createdAt.asc", "createdAt.desc",
            "rating.asc", "rating.desc"
        };

        for (String sort : sortOptions) {
            mockMvc.perform(get("/me/reviews")
                    .param("sort", sort)
                    .with(jwt()))
                .andExpect(status().isOk());

            verify(reviewService).getUserReviews(userId, 1L, 5L, sort, null);
        }
    }

    @Test
    void testGetUserFavorites_bookIdフィルタあり() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "test-book-123";

        PageResponse<FavoriteResponse> response = new PageResponse<>();

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(favoriteService.getUserFavorites(userId, 1L, 5L, "updatedAt.desc", bookId))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/favorites")
                .param("bookId", bookId)
                .with(jwt()))
            .andExpect(status().isOk());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).getUserFavorites(userId, 1L, 5L, "updatedAt.desc", bookId);
    }

    @Test
    void testGetUserBookmarks_bookIdフィルタあり() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "test-book-123";

        PageResponse<BookmarkResponse> response = new PageResponse<>();

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.getUserBookmarks(userId, 1L, 5L, "updatedAt.desc", bookId))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/me/bookmarks")
                .param("bookId", bookId)
                .with(jwt()))
            .andExpect(status().isOk());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).getUserBookmarks(userId, 1L, 5L, "updatedAt.desc", bookId);
    }
}
