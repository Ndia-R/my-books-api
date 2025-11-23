package com.example.my_books_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookStatsServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private BookStatsServiceImpl bookStatsService;

    private Book testBook;
    private String testBookId;

    @BeforeEach
    void setUp() {
        testBookId = "test-book-123";
        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Test Book");
        testBook.setReviewCount(0L);
        testBook.setAverageRating(0.0);
        testBook.setPopularity(0.0);
    }

    @Test
    void testUpdateBookStats_正常系_レビューあり() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(10L);
        stats.setAverageRating(4.5);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(reviewRepository).getReviewStatsResponse(testBookId);
        verify(bookRepository).findById(testBookId);
        verify(bookRepository).save(argThat(book -> {
            assertEquals(10L, book.getReviewCount());
            assertEquals(4.5, book.getAverageRating());
            assertTrue(book.getPopularity() > 0);
            return true;
        }));
    }

    @Test
    void testUpdateBookStats_正常系_レビューなし() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(0L);
        stats.setAverageRating(0.0);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(reviewRepository).getReviewStatsResponse(testBookId);
        verify(bookRepository).findById(testBookId);
        verify(bookRepository).save(argThat(book -> {
            assertEquals(0L, book.getReviewCount());
            assertEquals(0.0, book.getAverageRating());
            assertEquals(0.0, book.getPopularity());
            return true;
        }));
    }

    @Test
    void testUpdateBookStats_人気度の計算() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(100L);
        stats.setAverageRating(5.0);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(bookRepository).save(argThat(book -> {
            assertEquals(100L, book.getReviewCount());
            assertEquals(5.0, book.getAverageRating());
            // popularity = 5.0 * log(101) * 20 ≈ 460以上
            assertTrue(book.getPopularity() > 450);
            assertTrue(book.getPopularity() < 470);
            return true;
        }));
    }

    @Test
    void testUpdateBookStats_小数点以下2桁に丸め() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(10L);
        stats.setAverageRating(3.567);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(bookRepository).save(argThat(book -> {
            // averageRatingは小数点以下2桁に丸められる
            assertEquals(3.57, book.getAverageRating());
            // popularityも小数点以下2桁に丸められる
            String popularityStr = String.format("%.2f", book.getPopularity());
            assertEquals(book.getPopularity(), Double.parseDouble(popularityStr));
            return true;
        }));
    }

    @Test
    void testUpdateBookStats_書籍が存在しない場合() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(10L);
        stats.setAverageRating(4.5);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookStatsService.updateBookStats(testBookId);
        });

        verify(reviewRepository).getReviewStatsResponse(testBookId);
        verify(bookRepository).findById(testBookId);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testUpdateBookStats_レビュー数が多い場合の人気度() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(1000L);
        stats.setAverageRating(4.8);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(bookRepository).save(argThat(book -> {
            assertEquals(1000L, book.getReviewCount());
            assertEquals(4.8, book.getAverageRating());
            // popularity = 4.8 * log(1001) * 20 ≈ 662以上
            assertTrue(book.getPopularity() > 660);
            assertTrue(book.getPopularity() < 670);
            return true;
        }));
    }

    @Test
    void testUpdateBookStats_低評価の場合の人気度() {
        // Given
        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setReviewCount(50L);
        stats.setAverageRating(2.0);

        when(reviewRepository.getReviewStatsResponse(testBookId)).thenReturn(stats);
        when(bookRepository.findById(testBookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        bookStatsService.updateBookStats(testBookId);

        // Then
        verify(bookRepository).save(argThat(book -> {
            assertEquals(50L, book.getReviewCount());
            assertEquals(2.0, book.getAverageRating());
            // 低評価なら人気度も低い
            assertTrue(book.getPopularity() > 0);
            assertTrue(book.getPopularity() < 200);
            return true;
        }));
    }
}
