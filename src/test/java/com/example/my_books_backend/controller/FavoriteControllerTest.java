package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.util.JwtClaimExtractor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private JwtClaimExtractor jwtClaimExtractor;

    @Test
    void testCreateFavorite_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\"}";

        // When & Then
        mockMvc.perform(post("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(favoriteService, never()).createFavoriteByUserId(any(FavoriteRequest.class), anyString());
    }

    @Test
    void testCreateFavorite_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\"}";

        FavoriteResponse response = new FavoriteResponse();
        response.setId(1L);
        response.setUserId(userId);

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(favoriteService.createFavoriteByUserId(any(FavoriteRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/favorites/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(userId));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).createFavoriteByUserId(any(FavoriteRequest.class), eq(userId));
    }

    @Test
    void testCreateFavorite_バリデーションエラー_bookIdなし() throws Exception {
        // Given
        String requestBody = "{}";

        // When & Then
        mockMvc.perform(post("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(favoriteService, never()).createFavoriteByUserId(any(FavoriteRequest.class), anyString());
    }

    @Test
    void testCreateFavorite_存在しない書籍_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"non-existent-book\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(favoriteService.createFavoriteByUserId(any(FavoriteRequest.class), eq(userId)))
            .thenThrow(new NotFoundException("書籍が見つかりません"));

        // When & Then
        mockMvc.perform(post("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).createFavoriteByUserId(any(FavoriteRequest.class), eq(userId));
    }

    @Test
    void testCreateFavorite_重複するお気に入り_409Conflict() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(favoriteService.createFavoriteByUserId(any(FavoriteRequest.class), eq(userId)))
            .thenThrow(new ConflictException("既にお気に入りに追加されています"));

        // When & Then
        mockMvc.perform(post("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isConflict());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).createFavoriteByUserId(any(FavoriteRequest.class), eq(userId));
    }

    @Test
    void testDeleteFavoriteById_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long favoriteId = 1L;

        // When & Then
        mockMvc.perform(delete("/favorites/{id}", favoriteId))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(favoriteService, never()).deleteFavoriteByUserId(anyLong(), anyString());
    }

    @Test
    void testDeleteFavoriteById_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long favoriteId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doNothing().when(favoriteService).deleteFavoriteByUserId(favoriteId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/{id}", favoriteId)
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByUserId(favoriteId, userId);
    }

    @Test
    void testDeleteFavoriteById_存在しないお気に入り_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long favoriteId = 999L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new NotFoundException("お気に入りが見つかりません"))
            .when(favoriteService).deleteFavoriteByUserId(favoriteId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/{id}", favoriteId)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByUserId(favoriteId, userId);
    }

    @Test
    void testDeleteFavoriteById_他人のお気に入り_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long favoriteId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new ForbiddenException("他人のお気に入りは削除できません"))
            .when(favoriteService).deleteFavoriteByUserId(favoriteId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/{id}", favoriteId)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByUserId(favoriteId, userId);
    }

    @Test
    void testDeleteFavoriteByBookId_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String bookId = "test-book-123";

        // When & Then
        mockMvc.perform(delete("/favorites/books/{bookId}", bookId))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(favoriteService, never()).deleteFavoriteByBookIdAndUserId(anyString(), anyString());
    }

    @Test
    void testDeleteFavoriteByBookId_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "test-book-123";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doNothing().when(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/books/{bookId}", bookId)
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);
    }

    @Test
    void testDeleteFavoriteByBookId_存在しないお気に入り_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "non-existent-book";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new NotFoundException("お気に入りが見つかりません"))
            .when(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/books/{bookId}", bookId)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);
    }

    @Test
    void testDeleteFavoriteByBookId_他人のお気に入り_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String bookId = "test-book-123";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new ForbiddenException("他人のお気に入りは削除できません"))
            .when(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);

        // When & Then
        mockMvc.perform(delete("/favorites/books/{bookId}", bookId)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(favoriteService).deleteFavoriteByBookIdAndUserId(bookId, userId);
    }
}
