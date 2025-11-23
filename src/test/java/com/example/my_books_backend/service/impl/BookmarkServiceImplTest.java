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
import com.example.my_books_backend.dto.book.BookResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.BookChapterId;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookmarkMapper;
import com.example.my_books_backend.repository.BookChapterPageContentRepository;
import com.example.my_books_backend.repository.BookChapterRepository;
import com.example.my_books_backend.repository.BookmarkRepository;
import com.example.my_books_backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookmarkServiceImplTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkMapper bookmarkMapper;

    @Mock
    private BookChapterPageContentRepository bookChapterPageContentRepository;

    @Mock
    private BookChapterRepository bookChapterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookmarkServiceImpl bookmarkService;

    private User testUser;
    private Book testBook;
    private BookChapterPageContent testPageContent;
    private Bookmark testBookmark;
    private BookChapter testChapter;
    private String testUserId;
    private String testBookId;
    private Long testChapterNumber;
    private Long testPageNumber;

    @BeforeEach
    void setUp() {
        testUserId = "550e8400-e29b-41d4-a716-446655440000";
        testBookId = "test-book-123";
        testChapterNumber = 1L;
        testPageNumber = 5L;

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setDisplayName("Test User");

        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Test Book");

        testPageContent = new BookChapterPageContent();
        testPageContent.setBookId(testBookId);
        testPageContent.setChapterNumber(testChapterNumber);
        testPageContent.setPageNumber(testPageNumber);

        testBookmark = new Bookmark();
        testBookmark.setId(1L);
        testBookmark.setUser(testUser);
        testBookmark.setPageContent(testPageContent);
        testBookmark.setNote("Test note");
        testBookmark.setIsDeleted(false);

        testChapter = new BookChapter();
        BookChapterId chapterId = new BookChapterId(testBookId, testChapterNumber);
        testChapter.setId(chapterId);
        testChapter.setTitle("Test Chapter");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserBookmarks_正常系_bookIdなし() {
        // Given
        List<Bookmark> bookmarks = Arrays.asList(testBookmark);
        Page<Bookmark> page = new PageImpl<>(bookmarks);

        BookResponse bookResponse = new BookResponse();
        bookResponse.setId(testBookId);

        BookmarkResponse bookmarkResponse = new BookmarkResponse();
        bookmarkResponse.setBook(bookResponse);
        bookmarkResponse.setChapterNumber(testChapterNumber);

        List<BookmarkResponse> bookmarkResponses = Arrays.asList(bookmarkResponse);
        PageResponse<BookmarkResponse> expectedResponse = new PageResponse<>();
        expectedResponse.setData(bookmarkResponses);

        when(bookmarkRepository.findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class)))
            .thenReturn(page);
        when(bookmarkRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(bookmarks);
        when(bookmarkMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);
        when(bookChapterRepository.findByIdInAndIsDeletedFalse(any()))
            .thenReturn(Arrays.asList(testChapter));

        // When
        PageResponse<BookmarkResponse> result = bookmarkService.getUserBookmarks(
            testUserId, 1L, 20L, "updatedAt.desc", null
        );

        // Then
        assertNotNull(result);
        verify(bookmarkRepository).findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class));
        verify(bookmarkRepository).findAllByIdInWithRelations(anyList());
        verify(bookmarkMapper).toPageResponse(any(Page.class));
        verify(bookChapterRepository).findByIdInAndIsDeletedFalse(any());
        assertEquals("Test Chapter", result.getData().get(0).getChapterTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserBookmarks_正常系_bookIdあり() {
        // Given
        List<Bookmark> bookmarks = Arrays.asList(testBookmark);
        Page<Bookmark> page = new PageImpl<>(bookmarks);

        List<BookmarkResponse> bookmarkResponses = new ArrayList<>();
        PageResponse<BookmarkResponse> expectedResponse = new PageResponse<>();
        expectedResponse.setData(bookmarkResponses);

        when(bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class)))
            .thenReturn(page);
        when(bookmarkRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(bookmarks);
        when(bookmarkMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<BookmarkResponse> result = bookmarkService.getUserBookmarks(
            testUserId, 1L, 20L, "updatedAt.desc", testBookId
        );

        // Then
        assertNotNull(result);
        verify(bookmarkRepository).findByUserIdAndIsDeletedFalseAndPageContentBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class));
        verify(bookmarkRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    void testCreateBookmarkByUserId_正常系_新規作成() {
        // Given
        BookmarkRequest request = new BookmarkRequest();
        request.setBookId(testBookId);
        request.setChapterNumber(testChapterNumber);
        request.setPageNumber(testPageNumber);
        request.setNote("New note");

        BookmarkResponse expectedResponse = new BookmarkResponse();
        expectedResponse.setId(1L);

        when(bookChapterPageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(testPageContent));
        when(bookmarkRepository.findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
            testUserId, testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(bookmarkRepository.save(any(Bookmark.class)))
            .thenReturn(testBookmark);
        when(bookmarkMapper.toBookmarkResponse(any(Bookmark.class)))
            .thenReturn(expectedResponse);

        // When
        BookmarkResponse result = bookmarkService.createBookmarkByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        verify(bookChapterPageContentRepository).findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber);
        verify(userRepository).findById(testUserId);
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void testCreateBookmarkByUserId_正常系_論理削除済みブックマークの復活() {
        // Given
        BookmarkRequest request = new BookmarkRequest();
        request.setBookId(testBookId);
        request.setChapterNumber(testChapterNumber);
        request.setPageNumber(testPageNumber);
        request.setNote("Restored note");

        Bookmark deletedBookmark = new Bookmark();
        deletedBookmark.setId(1L);
        deletedBookmark.setUser(testUser);
        deletedBookmark.setPageContent(testPageContent);
        deletedBookmark.setIsDeleted(true);

        BookmarkResponse expectedResponse = new BookmarkResponse();

        when(bookChapterPageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(testPageContent));
        when(bookmarkRepository.findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
            testUserId, testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(deletedBookmark));
        when(bookmarkRepository.save(any(Bookmark.class)))
            .thenReturn(deletedBookmark);
        when(bookmarkMapper.toBookmarkResponse(any(Bookmark.class)))
            .thenReturn(expectedResponse);

        // When
        BookmarkResponse result = bookmarkService.createBookmarkByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        assertFalse(deletedBookmark.getIsDeleted());
        verify(bookmarkRepository).save(deletedBookmark);
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void testCreateBookmarkByUserId_ページコンテンツが存在しない場合() {
        // Given
        BookmarkRequest request = new BookmarkRequest();
        request.setBookId(testBookId);
        request.setChapterNumber(testChapterNumber);
        request.setPageNumber(testPageNumber);

        when(bookChapterPageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookmarkService.createBookmarkByUserId(request, testUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testCreateBookmarkByUserId_既存ブックマークが存在する場合() {
        // Given
        BookmarkRequest request = new BookmarkRequest();
        request.setBookId(testBookId);
        request.setChapterNumber(testChapterNumber);
        request.setPageNumber(testPageNumber);

        when(bookChapterPageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(testPageContent));
        when(bookmarkRepository.findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
            testUserId, testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(testBookmark));

        // When & Then
        assertThrows(ConflictException.class, () -> {
            bookmarkService.createBookmarkByUserId(request, testUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testCreateBookmarkByUserId_ユーザーが存在しない場合() {
        // Given
        BookmarkRequest request = new BookmarkRequest();
        request.setBookId(testBookId);
        request.setChapterNumber(testChapterNumber);
        request.setPageNumber(testPageNumber);

        when(bookChapterPageContentRepository.findByBookIdAndChapterNumberAndPageNumber(
            testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.of(testPageContent));
        when(bookmarkRepository.findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
            testUserId, testBookId, testChapterNumber, testPageNumber))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookmarkService.createBookmarkByUserId(request, testUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testUpdateBookmarkByUserId_正常系() {
        // Given
        Long bookmarkId = 1L;
        BookmarkRequest request = new BookmarkRequest();
        request.setNote("Updated note");

        BookmarkResponse expectedResponse = new BookmarkResponse();

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.of(testBookmark));
        when(bookmarkRepository.save(any(Bookmark.class)))
            .thenReturn(testBookmark);
        when(bookmarkMapper.toBookmarkResponse(any(Bookmark.class)))
            .thenReturn(expectedResponse);

        // When
        BookmarkResponse result = bookmarkService.updateBookmarkByUserId(bookmarkId, request, testUserId);

        // Then
        assertNotNull(result);
        assertEquals("Updated note", testBookmark.getNote());
        verify(bookmarkRepository).save(testBookmark);
    }

    @Test
    void testUpdateBookmarkByUserId_ブックマークが存在しない場合() {
        // Given
        Long bookmarkId = 1L;
        BookmarkRequest request = new BookmarkRequest();
        request.setNote("Updated note");

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookmarkService.updateBookmarkByUserId(bookmarkId, request, testUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testUpdateBookmarkByUserId_権限がない場合() {
        // Given
        Long bookmarkId = 1L;
        String otherUserId = "other-user-id";
        BookmarkRequest request = new BookmarkRequest();
        request.setNote("Updated note");

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.of(testBookmark));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            bookmarkService.updateBookmarkByUserId(bookmarkId, request, otherUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testDeleteBookmarkByUserId_正常系() {
        // Given
        Long bookmarkId = 1L;

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.of(testBookmark));
        when(bookmarkRepository.save(any(Bookmark.class)))
            .thenReturn(testBookmark);

        // When
        bookmarkService.deleteBookmarkByUserId(bookmarkId, testUserId);

        // Then
        assertTrue(testBookmark.getIsDeleted());
        verify(bookmarkRepository).save(testBookmark);
    }

    @Test
    void testDeleteBookmarkByUserId_ブックマークが存在しない場合() {
        // Given
        Long bookmarkId = 1L;

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            bookmarkService.deleteBookmarkByUserId(bookmarkId, testUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void testDeleteBookmarkByUserId_権限がない場合() {
        // Given
        Long bookmarkId = 1L;
        String otherUserId = "other-user-id";

        when(bookmarkRepository.findById(bookmarkId))
            .thenReturn(Optional.of(testBookmark));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            bookmarkService.deleteBookmarkByUserId(bookmarkId, otherUserId);
        });
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }
}
