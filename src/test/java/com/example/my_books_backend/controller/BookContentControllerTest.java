package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.BookService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookContentController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class BookContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void testGetBookChapterPageContent_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String bookId = "test-book-123";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber))
            .andExpect(status().isUnauthorized());

        // サービスは呼ばれない
        verify(bookService, never()).getBookChapterPageContent(anyString(), anyLong(), anyLong());
    }

    @Test
    void testGetBookChapterPageContent_正常系_JWT認証あり() throws Exception {
        // Given
        String bookId = "test-book-123";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("This is the page content.");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value(bookId))
            .andExpect(jsonPath("$.chapterNumber").value(chapterNumber))
            .andExpect(jsonPath("$.pageNumber").value(pageNumber))
            .andExpect(jsonPath("$.content").value("This is the page content."));

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_正常系_WithMockUser() throws Exception {
        // Given
        String bookId = "test-book-123";
        Long chapterNumber = 2L;
        Long pageNumber = 5L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("Chapter 2, Page 5 content.");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt().jwt(jwt -> jwt.subject("550e8400-e29b-41d4-a716-446655440000"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value(bookId))
            .andExpect(jsonPath("$.chapterNumber").value(chapterNumber))
            .andExpect(jsonPath("$.pageNumber").value(pageNumber))
            .andExpect(jsonPath("$.content").value("Chapter 2, Page 5 content."));

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_存在しないページ_404NotFound() throws Exception {
        // Given
        String bookId = "test-book-123";
        Long chapterNumber = 99L;
        Long pageNumber = 99L;

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenThrow(new NotFoundException("指定されたページが見つかりません"));

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_存在しない書籍_404NotFound() throws Exception {
        // Given
        String nonExistentBookId = "non-existent-book";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        when(bookService.getBookChapterPageContent(nonExistentBookId, chapterNumber, pageNumber))
            .thenThrow(new NotFoundException("指定された書籍が見つかりません"));

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                nonExistentBookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(bookService).getBookChapterPageContent(nonExistentBookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_パスパラメータの検証() throws Exception {
        // Given
        String bookId = "book-with-special-chars-123";
        Long chapterNumber = 10L;
        Long pageNumber = 25L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("Content");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value(bookId))
            .andExpect(jsonPath("$.chapterNumber").value(chapterNumber))
            .andExpect(jsonPath("$.pageNumber").value(pageNumber));

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_JWT認証_異なるユーザー() throws Exception {
        // Given
        String bookId = "test-book-123";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("Content");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then - User1でアクセス
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt().jwt(jwt -> jwt.subject("user-id-1"))))
            .andExpect(status().isOk());

        // When & Then - User2でも同じコンテンツにアクセス可能
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt().jwt(jwt -> jwt.subject("user-id-2"))))
            .andExpect(status().isOk());

        // 2回呼ばれる
        verify(bookService, times(2)).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_章番号1ページ番号1() throws Exception {
        // Given - 最初のページ
        String bookId = "test-book-123";
        Long chapterNumber = 1L;
        Long pageNumber = 1L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("First page of first chapter");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("First page of first chapter"));

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_大きな章番号とページ番号() throws Exception {
        // Given - 大きな番号
        String bookId = "test-book-123";
        Long chapterNumber = 100L;
        Long pageNumber = 500L;

        BookChapterPageContentResponse response = new BookChapterPageContentResponse();
        response.setBookId(bookId);
        response.setChapterNumber(chapterNumber);
        response.setPageNumber(pageNumber);
        response.setContent("Large chapter and page number content");

        when(bookService.getBookChapterPageContent(bookId, chapterNumber, pageNumber))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/book-content/books/{id}/chapters/{chapter}/pages/{page}",
                bookId, chapterNumber, pageNumber)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chapterNumber").value(chapterNumber))
            .andExpect(jsonPath("$.pageNumber").value(pageNumber));

        verify(bookService).getBookChapterPageContent(bookId, chapterNumber, pageNumber);
    }
}
