package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.util.JwtClaimExtractor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookmarkController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookmarkService bookmarkService;

    @MockBean
    private JwtClaimExtractor jwtClaimExtractor;

    @Test
    void testCreateBookmark_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":1,\"pageNumber\":1,\"note\":\"メモ\"}";

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).createBookmarkByUserId(any(BookmarkRequest.class), anyString());
    }

    @Test
    void testCreateBookmark_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":1,\"pageNumber\":1,\"note\":\"メモ\"}";

        BookmarkResponse response = new BookmarkResponse();
        response.setId(1L);
        response.setUserId(userId);
        response.setChapterNumber(1L);
        response.setPageNumber(1L);
        response.setNote("メモ");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.createBookmarkByUserId(any(BookmarkRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/bookmarks/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.chapterNumber").value(1))
            .andExpect(jsonPath("$.pageNumber").value(1))
            .andExpect(jsonPath("$.note").value("メモ"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).createBookmarkByUserId(any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testCreateBookmark_バリデーションエラー_bookIdなし() throws Exception {
        // Given
        String requestBody = "{\"chapterNumber\":1,\"pageNumber\":1,\"note\":\"メモ\"}";

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).createBookmarkByUserId(any(BookmarkRequest.class), anyString());
    }

    @Test
    void testCreateBookmark_バリデーションエラー_chapterNumberなし() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"pageNumber\":1,\"note\":\"メモ\"}";

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).createBookmarkByUserId(any(BookmarkRequest.class), anyString());
    }

    @Test
    void testCreateBookmark_バリデーションエラー_pageNumberなし() throws Exception {
        // Given
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":1,\"note\":\"メモ\"}";

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).createBookmarkByUserId(any(BookmarkRequest.class), anyString());
    }

    @Test
    void testCreateBookmark_存在しない書籍_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"non-existent-book\",\"chapterNumber\":1,\"pageNumber\":1,\"note\":\"メモ\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.createBookmarkByUserId(any(BookmarkRequest.class), eq(userId)))
            .thenThrow(new NotFoundException("書籍が見つかりません"));

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).createBookmarkByUserId(any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testCreateBookmark_重複するブックマーク_409Conflict() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":1,\"pageNumber\":1,\"note\":\"メモ\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.createBookmarkByUserId(any(BookmarkRequest.class), eq(userId)))
            .thenThrow(new ConflictException("既にブックマークが存在します"));

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isConflict());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).createBookmarkByUserId(any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testUpdateBookmark_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long bookmarkId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":2,\"pageNumber\":5,\"note\":\"更新メモ\"}";

        // When & Then
        mockMvc.perform(put("/bookmarks/{id}", bookmarkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).updateBookmarkByUserId(anyLong(), any(BookmarkRequest.class), anyString());
    }

    @Test
    void testUpdateBookmark_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":2,\"pageNumber\":5,\"note\":\"更新メモ\"}";

        BookmarkResponse response = new BookmarkResponse();
        response.setId(bookmarkId);
        response.setUserId(userId);
        response.setChapterNumber(2L);
        response.setPageNumber(5L);
        response.setNote("更新メモ");

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(put("/bookmarks/{id}", bookmarkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookmarkId))
            .andExpect(jsonPath("$.chapterNumber").value(2))
            .andExpect(jsonPath("$.pageNumber").value(5))
            .andExpect(jsonPath("$.note").value("更新メモ"));

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testUpdateBookmark_存在しないブックマーク_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 999L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":2,\"pageNumber\":5,\"note\":\"更新メモ\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId)))
            .thenThrow(new NotFoundException("ブックマークが見つかりません"));

        // When & Then
        mockMvc.perform(put("/bookmarks/{id}", bookmarkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testUpdateBookmark_他人のブックマーク_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 1L;
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":2,\"pageNumber\":5,\"note\":\"更新メモ\"}";

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        when(bookmarkService.updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId)))
            .thenThrow(new ForbiddenException("他人のブックマークは編集できません"));

        // When & Then
        mockMvc.perform(put("/bookmarks/{id}", bookmarkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).updateBookmarkByUserId(eq(bookmarkId), any(BookmarkRequest.class), eq(userId));
    }

    @Test
    void testDeleteBookmark_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long bookmarkId = 1L;

        // When & Then
        mockMvc.perform(delete("/bookmarks/{id}", bookmarkId))
            .andExpect(status().isUnauthorized());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).deleteBookmarkByUserId(anyLong(), anyString());
    }

    @Test
    void testDeleteBookmark_正常系() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doNothing().when(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);

        // When & Then
        mockMvc.perform(delete("/bookmarks/{id}", bookmarkId)
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);
    }

    @Test
    void testDeleteBookmark_存在しないブックマーク_404NotFound() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 999L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new NotFoundException("ブックマークが見つかりません"))
            .when(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);

        // When & Then
        mockMvc.perform(delete("/bookmarks/{id}", bookmarkId)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);
    }

    @Test
    void testDeleteBookmark_他人のブックマーク_403Forbidden() throws Exception {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440001";
        Long bookmarkId = 1L;

        when(jwtClaimExtractor.getCurrentUserId()).thenReturn(userId);
        doThrow(new ForbiddenException("他人のブックマークは削除できません"))
            .when(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);

        // When & Then
        mockMvc.perform(delete("/bookmarks/{id}", bookmarkId)
                .with(jwt()))
            .andExpect(status().isForbidden());

        verify(jwtClaimExtractor).getCurrentUserId();
        verify(bookmarkService).deleteBookmarkByUserId(bookmarkId, userId);
    }

    @Test
    void testCreateBookmark_バリデーションエラー_noteなし() throws Exception {
        // Given - noteは必須フィールドのためバリデーションエラーになる
        String requestBody = "{\"bookId\":\"test-book-123\",\"chapterNumber\":1,\"pageNumber\":1}";

        // When & Then
        mockMvc.perform(post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(jwtClaimExtractor, never()).getCurrentUserId();
        verify(bookmarkService, never()).createBookmarkByUserId(any(BookmarkRequest.class), anyString());
    }
}
