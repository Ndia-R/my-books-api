package com.example.my_books_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book.BookDetailsResponse;
import com.example.my_books_backend.dto.book.BookResponse;
import com.example.my_books_backend.dto.book_chapter.BookChapterResponse;
import com.example.my_books_backend.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.exception.BadRequestException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookMapper;
import com.example.my_books_backend.repository.BookChapterPageContentRepository;
import com.example.my_books_backend.repository.BookRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BookChapterPageContentRepository bookChapterPageContentRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private String testBookId;

    @BeforeEach
    void setUp() {
        testBookId = "test-book-123";

        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Test Book");
        testBook.setIsDeleted(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBooks_正常系() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        Page<Book> page = new PageImpl<>(books);
        PageResponse<BookResponse> expectedResponse = new PageResponse<>();

        when(bookRepository.findByIsDeletedFalse(any(Pageable.class)))
            .thenReturn(page);
        when(bookRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(books);
        when(bookMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookResponse> result = bookService.getBooks(1L, 20L, "popularity.desc");

        // Then
        assertNotNull(result);
        verify(bookRepository).findByIsDeletedFalse(any(Pageable.class));
        verify(bookRepository).findAllByIdInWithRelations(anyList());
        verify(bookMapper).toPageResponse(any(Page.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBooksByTitleKeyword_正常系() {
        // Given
        String keyword = "Test";
        List<Book> books = Arrays.asList(testBook);
        Page<Book> page = new PageImpl<>(books);
        PageResponse<BookResponse> expectedResponse = new PageResponse<>();

        when(bookRepository.findByTitleContainingAndIsDeletedFalse(eq(keyword), any(Pageable.class)))
            .thenReturn(page);
        when(bookRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(books);
        when(bookMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookResponse> result = bookService.getBooksByTitleKeyword(
            keyword, 1L, 20L, "title.asc"
        );

        // Then
        assertNotNull(result);
        verify(bookRepository).findByTitleContainingAndIsDeletedFalse(eq(keyword), any(Pageable.class));
        verify(bookRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBooksByGenre_正常系_OR条件() {
        // Given
        String genreIds = "1,2,3";
        String condition = "OR";
        List<Book> books = Arrays.asList(testBook);
        Page<Book> page = new PageImpl<>(books);
        PageResponse<BookResponse> expectedResponse = new PageResponse<>();

        when(bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(anyList(), any(Pageable.class)))
            .thenReturn(page);
        when(bookRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(books);
        when(bookMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookResponse> result = bookService.getBooksByGenre(
            genreIds, condition, 1L, 20L, "popularity.desc"
        );

        // Then
        assertNotNull(result);
        verify(bookRepository).findDistinctByGenres_IdInAndIsDeletedFalse(
            eq(Arrays.asList(1L, 2L, 3L)), any(Pageable.class));
        verify(bookRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBooksByGenre_正常系_AND条件() {
        // Given
        String genreIds = "1,2";
        String condition = "AND";
        List<Book> books = Arrays.asList(testBook);
        Page<Book> page = new PageImpl<>(books);
        PageResponse<BookResponse> expectedResponse = new PageResponse<>();

        when(bookRepository.findBooksHavingAllGenres(anyList(), anyLong(), any(Pageable.class)))
            .thenReturn(page);
        when(bookRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(books);
        when(bookMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookResponse> result = bookService.getBooksByGenre(
            genreIds, condition, 1L, 20L, "popularity.desc"
        );

        // Then
        assertNotNull(result);
        verify(bookRepository).findBooksHavingAllGenres(
            eq(Arrays.asList(1L, 2L)), eq(2L), any(Pageable.class));
        verify(bookRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBooksByGenre_正常系_SINGLE条件() {
        // Given
        String genreIds = "1";
        String condition = "SINGLE";
        List<Book> books = Arrays.asList(testBook);
        Page<Book> page = new PageImpl<>(books);
        PageResponse<BookResponse> expectedResponse = new PageResponse<>();

        when(bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(anyList(), any(Pageable.class)))
            .thenReturn(page);
        when(bookRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(books);
        when(bookMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookResponse> result = bookService.getBooksByGenre(
            genreIds, condition, 1L, 20L, "popularity.desc"
        );

        // Then
        assertNotNull(result);
        verify(bookRepository).findDistinctByGenres_IdInAndIsDeletedFalse(
            eq(Arrays.asList(1L)), any(Pageable.class));
    }

    @Test
    void testGetBooksByGenre_不正な条件() {
        // Given
        String genreIds = "1,2";
        String invalidCondition = "INVALID";

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            bookService.getBooksByGenre(genreIds, invalidCondition, 1L, 20L, "popularity.desc");
        });
        verify(bookRepository, never()).findDistinctByGenres_IdInAndIsDeletedFalse(anyList(), any(Pageable.class));
        verify(bookRepository, never()).findBooksHavingAllGenres(anyList(), anyLong(), any(Pageable.class));
    }

    @Test
    void testGetBooksByGenre_不正なジャンルID() {
        // Given
        String invalidGenreIds = "1,abc,3";
        String condition = "OR";

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            bookService.getBooksByGenre(invalidGenreIds, condition, 1L, 20L, "popularity.desc");
        });
    }

    @Test
    void testGetBookDetails_正常系() {
        // Given
        BookDetailsResponse expectedResponse = new BookDetailsResponse();
        expectedResponse.setId(testBookId);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(bookMapper.toBookDetailsResponse(testBook))
            .thenReturn(expectedResponse);

        // When
        BookDetailsResponse result = bookService.getBookDetails(testBookId);

        // Then
        assertNotNull(result);
        assertEquals(testBookId, result.getId());
        verify(bookRepository).findById(testBookId);
        verify(bookMapper).toBookDetailsResponse(testBook);
    }

    @Test
    void testGetBookDetails_書籍が存在しない場合() {
        // Given
        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookService.getBookDetails(testBookId);
        });
        verify(bookRepository).findById(testBookId);
    }

    @Test
    void testGetBookTableOfContents_正常系() {
        // Given
        BookChapterResponse chapter1 = new BookChapterResponse();
        chapter1.setChapterNumber(1L);
        chapter1.setChapterTitle("Chapter 1");

        BookChapterResponse chapter2 = new BookChapterResponse();
        chapter2.setChapterNumber(2L);
        chapter2.setChapterTitle("Chapter 2");

        List<BookChapterResponse> chapters = Arrays.asList(chapter1, chapter2);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(bookChapterPageContentRepository.findChapterResponsesByBookId(testBookId))
            .thenReturn(chapters);

        // When
        BookTableOfContentsResponse result = bookService.getBookTableOfContents(testBookId);

        // Then
        assertNotNull(result);
        assertEquals(testBookId, result.getBookId());
        assertEquals("Test Book", result.getTitle());
        assertEquals(2, result.getChapters().size());
        verify(bookRepository).findById(testBookId);
        verify(bookChapterPageContentRepository).findChapterResponsesByBookId(testBookId);
    }

    @Test
    void testGetBookTableOfContents_書籍が存在しない場合() {
        // Given
        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookService.getBookTableOfContents(testBookId);
        });
        verify(bookRepository).findById(testBookId);
        verify(bookChapterPageContentRepository, never()).findChapterResponsesByBookId(anyString());
    }

    @Test
    void testGetBookChapterPageContent_正常系() {
        // Given
        Long chapterNumber = 1L;
        Long pageNumber = 5L;
        BookChapterPageContentResponse expectedResponse = new BookChapterPageContentResponse();
        expectedResponse.setBookId(testBookId);
        expectedResponse.setChapterNumber(chapterNumber);
        expectedResponse.setPageNumber(pageNumber);

        when(bookChapterPageContentRepository.findChapterPageContentResponse(
            testBookId, chapterNumber, pageNumber))
            .thenReturn(Optional.of(expectedResponse));

        // When
        BookChapterPageContentResponse result = bookService.getBookChapterPageContent(
            testBookId, chapterNumber, pageNumber
        );

        // Then
        assertNotNull(result);
        assertEquals(testBookId, result.getBookId());
        assertEquals(chapterNumber, result.getChapterNumber());
        assertEquals(pageNumber, result.getPageNumber());
        verify(bookChapterPageContentRepository).findChapterPageContentResponse(
            testBookId, chapterNumber, pageNumber);
    }

    @Test
    void testGetBookChapterPageContent_ページコンテンツが存在しない場合() {
        // Given
        Long chapterNumber = 1L;
        Long pageNumber = 5L;

        when(bookChapterPageContentRepository.findChapterPageContentResponse(
            testBookId, chapterNumber, pageNumber))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookService.getBookChapterPageContent(testBookId, chapterNumber, pageNumber);
        });
        verify(bookChapterPageContentRepository).findChapterPageContentResponse(
            testBookId, chapterNumber, pageNumber);
    }
}
