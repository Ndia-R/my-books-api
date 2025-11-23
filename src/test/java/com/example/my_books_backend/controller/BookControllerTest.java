package com.example.my_books_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book.BookDetailsResponse;
import com.example.my_books_backend.dto.book.BookResponse;
import com.example.my_books_backend.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.exception.BadRequestException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.BookService;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.service.ReviewService;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private FavoriteService favoriteService;

    @Test
    void testGetLatestBooks_認証なしでアクセス可能() throws Exception {
        // Given
        PageResponse<BookResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new BookResponse(), new BookResponse()));
        response.setCurrentPage(1L);
        response.setPageSize(10L);
        response.setTotalPages(1L);
        response.setTotalItems(2L);

        when(bookService.getBooks(1L, 10L, "publicationDate.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/new-releases"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.currentPage").value(1))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalItems").value(2));

        verify(bookService).getBooks(1L, 10L, "publicationDate.desc");
    }

    @Test
    void testGetLatestBooks_常に10冊を取得() throws Exception {
        // Given
        PageResponse<BookResponse> response = new PageResponse<>();
        response.setData(Arrays.asList());

        when(bookService.getBooks(1L, 10L, "publicationDate.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/new-releases"))
            .andExpect(status().isOk());

        verify(bookService).getBooks(1L, 10L, "publicationDate.desc");
    }

    @Test
    void testGetBooksByTitleKeyword_認証なしでアクセス可能() throws Exception {
        // Given
        String keyword = "魔法";
        PageResponse<BookResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new BookResponse()));

        when(bookService.getBooksByTitleKeyword(keyword, 1L, 20L, "popularity.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", keyword))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(bookService).getBooksByTitleKeyword(keyword, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByTitleKeyword_デフォルトパラメータ() throws Exception {
        // Given
        String keyword = "test";
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByTitleKeyword(keyword, 1L, 20L, "popularity.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", keyword))
            .andExpect(status().isOk());

        verify(bookService).getBooksByTitleKeyword(keyword, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByTitleKeyword_カスタムパラメータ() throws Exception {
        // Given
        String keyword = "test";
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByTitleKeyword(keyword, 2L, 10L, "title.asc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/search")
                .param("q", keyword)
                .param("page", "2")
                .param("size", "10")
                .param("sort", "title.asc"))
            .andExpect(status().isOk());

        verify(bookService).getBooksByTitleKeyword(keyword, 2L, 10L, "title.asc");
    }

    @Test
    void testGetBooksByTitleKeyword_クエリパラメータなし_400BadRequest() throws Exception {
        // When & Then - qパラメータが必須
        mockMvc.perform(get("/books/search"))
            .andExpect(status().isBadRequest());

        verify(bookService, never()).getBooksByTitleKeyword(anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    void testGetBooksByGenre_認証なしでアクセス可能_OR条件() throws Exception {
        // Given
        String genreIds = "1,2";
        String condition = "OR";
        PageResponse<BookResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new BookResponse()));

        when(bookService.getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", genreIds)
                .param("condition", condition))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(bookService).getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByGenre_AND条件() throws Exception {
        // Given
        String genreIds = "1,2,3";
        String condition = "AND";
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", genreIds)
                .param("condition", condition))
            .andExpect(status().isOk());

        verify(bookService).getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByGenre_SINGLE条件() throws Exception {
        // Given
        String genreIds = "1";
        String condition = "SINGLE";
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", genreIds)
                .param("condition", condition))
            .andExpect(status().isOk());

        verify(bookService).getBooksByGenre(genreIds, condition, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByGenre_不正な条件_400BadRequest() throws Exception {
        // Given
        String genreIds = "1,2";
        String invalidCondition = "INVALID";

        when(bookService.getBooksByGenre(genreIds, invalidCondition, 1L, 20L, "popularity.desc"))
            .thenThrow(new BadRequestException("不正な条件です"));

        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", genreIds)
                .param("condition", invalidCondition))
            .andExpect(status().isBadRequest());

        verify(bookService).getBooksByGenre(genreIds, invalidCondition, 1L, 20L, "popularity.desc");
    }

    @Test
    void testGetBooksByGenre_genreIdsパラメータなし_400BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("condition", "OR"))
            .andExpect(status().isBadRequest());

        verify(bookService, never()).getBooksByGenre(anyString(), anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    void testGetBooksByGenre_conditionパラメータなし_400BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", "1,2"))
            .andExpect(status().isBadRequest());

        verify(bookService, never()).getBooksByGenre(anyString(), anyString(), anyLong(), anyLong(), anyString());
    }

    @Test
    void testGetBookDetails_認証なしでアクセス可能() throws Exception {
        // Given
        String bookId = "test-book-123";
        BookDetailsResponse response = new BookDetailsResponse();
        response.setId(bookId);
        response.setTitle("Test Book");

        when(bookService.getBookDetails(bookId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(bookId))
            .andExpect(jsonPath("$.title").value("Test Book"));

        verify(bookService).getBookDetails(bookId);
    }

    @Test
    void testGetBookDetails_存在しない書籍_404NotFound() throws Exception {
        // Given
        String nonExistentBookId = "non-existent-book";

        when(bookService.getBookDetails(nonExistentBookId))
            .thenThrow(new NotFoundException("書籍が見つかりません"));

        // When & Then
        mockMvc.perform(get("/books/{id}", nonExistentBookId))
            .andExpect(status().isNotFound());

        verify(bookService).getBookDetails(nonExistentBookId);
    }

    @Test
    void testGetBookTableOfContents_認証なしでアクセス可能() throws Exception {
        // Given
        String bookId = "test-book-123";
        BookTableOfContentsResponse response = new BookTableOfContentsResponse();
        response.setBookId(bookId);
        response.setTitle("Test Book");

        when(bookService.getBookTableOfContents(bookId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}/toc", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value(bookId))
            .andExpect(jsonPath("$.title").value("Test Book"));

        verify(bookService).getBookTableOfContents(bookId);
    }

    @Test
    void testGetBookReviews_認証なしでアクセス可能() throws Exception {
        // Given
        String bookId = "test-book-123";
        PageResponse<ReviewResponse> response = new PageResponse<>();
        response.setData(Arrays.asList(new ReviewResponse()));

        when(reviewService.getBookReviews(bookId, 1L, 3L, "updatedAt.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}/reviews", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        verify(reviewService).getBookReviews(bookId, 1L, 3L, "updatedAt.desc");
    }

    @Test
    void testGetBookReviews_カスタムページネーション() throws Exception {
        // Given
        String bookId = "test-book-123";
        PageResponse<ReviewResponse> response = new PageResponse<>();

        when(reviewService.getBookReviews(bookId, 2L, 5L, "rating.desc"))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}/reviews", bookId)
                .param("page", "2")
                .param("size", "5")
                .param("sort", "rating.desc"))
            .andExpect(status().isOk());

        verify(reviewService).getBookReviews(bookId, 2L, 5L, "rating.desc");
    }

    @Test
    void testGetBookReviewStats_認証なしでアクセス可能() throws Exception {
        // Given
        String bookId = "test-book-123";
        ReviewStatsResponse response = new ReviewStatsResponse();
        response.setReviewCount(10L);
        response.setAverageRating(4.5);

        when(reviewService.getBookReviewStats(bookId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}/stats/reviews", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewCount").value(10))
            .andExpect(jsonPath("$.averageRating").value(4.5));

        verify(reviewService).getBookReviewStats(bookId);
    }

    @Test
    void testGetBookFavoriteStats_認証なしでアクセス可能() throws Exception {
        // Given
        String bookId = "test-book-123";
        FavoriteStatsResponse response = new FavoriteStatsResponse();
        response.setFavoriteCount(50L);

        when(favoriteService.getBookFavoriteStats(bookId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/{id}/stats/favorites", bookId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.favoriteCount").value(50));

        verify(favoriteService).getBookFavoriteStats(bookId);
    }

    @Test
    void testGetBooksByTitleKeyword_ソート条件の検証() throws Exception {
        // Given
        String keyword = "test";
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByTitleKeyword(eq(keyword), anyLong(), anyLong(), anyString()))
            .thenReturn(response);

        // When & Then - 各種ソート条件
        String[] sortOptions = {
            "title.asc", "title.desc",
            "publicationDate.asc", "publicationDate.desc",
            "reviewCount.asc", "reviewCount.desc",
            "averageRating.asc", "averageRating.desc",
            "popularity.asc", "popularity.desc"
        };

        for (String sort : sortOptions) {
            mockMvc.perform(get("/books/search")
                    .param("q", keyword)
                    .param("sort", sort))
                .andExpect(status().isOk());

            verify(bookService).getBooksByTitleKeyword(keyword, 1L, 20L, sort);
        }
    }

    @Test
    void testGetBooksByGenre_複数パラメータの組み合わせ() throws Exception {
        // Given
        PageResponse<BookResponse> response = new PageResponse<>();

        when(bookService.getBooksByGenre(anyString(), anyString(), anyLong(), anyLong(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/books/discover")
                .param("genreIds", "1,2,3")
                .param("condition", "AND")
                .param("page", "3")
                .param("size", "15")
                .param("sort", "title.asc"))
            .andExpect(status().isOk());

        verify(bookService).getBooksByGenre("1,2,3", "AND", 3L, 15L, "title.asc");
    }
}
