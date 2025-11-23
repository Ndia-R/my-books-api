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
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.FavoriteMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.FavoriteRepository;
import com.example.my_books_backend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private User testUser;
    private Book testBook;
    private Favorite testFavorite;
    private String testUserId;
    private String testBookId;

    @BeforeEach
    void setUp() {
        testUserId = "550e8400-e29b-41d4-a716-446655440000";
        testBookId = "test-book-123";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setDisplayName("Test User");

        testBook = new Book();
        testBook.setId(testBookId);
        testBook.setTitle("Test Book");

        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setUser(testUser);
        testFavorite.setBook(testBook);
        testFavorite.setIsDeleted(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserFavorites_正常系_bookIdなし() {
        // Given
        List<Favorite> favorites = Arrays.asList(testFavorite);
        Page<Favorite> page = new PageImpl<>(favorites);
        PageResponse<FavoriteResponse> expectedResponse = new PageResponse<>();

        when(favoriteRepository.findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class)))
            .thenReturn(page);
        when(favoriteRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(favorites);
        when(favoriteMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<FavoriteResponse> result = favoriteService.getUserFavorites(
            testUserId, 1L, 20L, "updatedAt.desc", null
        );

        // Then
        assertNotNull(result);
        verify(favoriteRepository).findByUserIdAndIsDeletedFalse(eq(testUserId), any(Pageable.class));
        verify(favoriteRepository).findAllByIdInWithRelations(anyList());
        verify(favoriteMapper).toPageResponse(any(Page.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUserFavorites_正常系_bookIdあり() {
        // Given
        List<Favorite> favorites = Arrays.asList(testFavorite);
        Page<Favorite> page = new PageImpl<>(favorites);
        PageResponse<FavoriteResponse> expectedResponse = new PageResponse<>();

        when(favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class)))
            .thenReturn(page);
        when(favoriteRepository.findAllByIdInWithRelations(anyList()))
            .thenReturn(favorites);
        when(favoriteMapper.toPageResponse(any(Page.class)))
            .thenReturn(expectedResponse);

        // When
        PageResponse<FavoriteResponse> result = favoriteService.getUserFavorites(
            testUserId, 1L, 20L, "updatedAt.desc", testBookId
        );

        // Then
        assertNotNull(result);
        verify(favoriteRepository).findByUserIdAndIsDeletedFalseAndBookId(
            eq(testUserId), eq(testBookId), any(Pageable.class));
        verify(favoriteRepository).findAllByIdInWithRelations(anyList());
    }

    @Test
    void testGetBookFavoriteStats_正常系() {
        // Given
        FavoriteStatsResponse expectedStats = new FavoriteStatsResponse();
        expectedStats.setFavoriteCount(100L);

        when(favoriteRepository.getFavoriteStatsResponse(testBookId))
            .thenReturn(expectedStats);

        // When
        FavoriteStatsResponse result = favoriteService.getBookFavoriteStats(testBookId);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getFavoriteCount());
        verify(favoriteRepository).getFavoriteStatsResponse(testBookId);
    }

    @Test
    void testCreateFavoriteByUserId_正常系_新規作成() {
        // Given
        FavoriteRequest request = new FavoriteRequest();
        request.setBookId(testBookId);

        FavoriteResponse expectedResponse = new FavoriteResponse();
        expectedResponse.setId(1L);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(favoriteRepository.save(any(Favorite.class)))
            .thenReturn(testFavorite);
        when(favoriteMapper.toFavoriteResponse(any(Favorite.class)))
            .thenReturn(expectedResponse);

        // When
        FavoriteResponse result = favoriteService.createFavoriteByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        verify(bookRepository).findById(testBookId);
        verify(favoriteRepository).findByUserIdAndBookId(testUserId, testBookId);
        verify(userRepository).findById(testUserId);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void testCreateFavoriteByUserId_正常系_論理削除済みお気に入りの復活() {
        // Given
        FavoriteRequest request = new FavoriteRequest();
        request.setBookId(testBookId);

        Favorite deletedFavorite = new Favorite();
        deletedFavorite.setId(1L);
        deletedFavorite.setUser(testUser);
        deletedFavorite.setBook(testBook);
        deletedFavorite.setIsDeleted(true);

        FavoriteResponse expectedResponse = new FavoriteResponse();

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.of(deletedFavorite));
        when(favoriteRepository.save(any(Favorite.class)))
            .thenReturn(deletedFavorite);
        when(favoriteMapper.toFavoriteResponse(any(Favorite.class)))
            .thenReturn(expectedResponse);

        // When
        FavoriteResponse result = favoriteService.createFavoriteByUserId(request, testUserId);

        // Then
        assertNotNull(result);
        assertFalse(deletedFavorite.getIsDeleted());
        verify(favoriteRepository).save(deletedFavorite);
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void testCreateFavoriteByUserId_書籍が存在しない場合() {
        // Given
        FavoriteRequest request = new FavoriteRequest();
        request.setBookId(testBookId);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            favoriteService.createFavoriteByUserId(request, testUserId);
        });
        verify(bookRepository).findById(testBookId);
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testCreateFavoriteByUserId_既存お気に入りが存在する場合() {
        // Given
        FavoriteRequest request = new FavoriteRequest();
        request.setBookId(testBookId);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.of(testFavorite));

        // When & Then
        assertThrows(ConflictException.class, () -> {
            favoriteService.createFavoriteByUserId(request, testUserId);
        });
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testCreateFavoriteByUserId_ユーザーが存在しない場合() {
        // Given
        FavoriteRequest request = new FavoriteRequest();
        request.setBookId(testBookId);

        when(bookRepository.findById(testBookId))
            .thenReturn(Optional.of(testBook));
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            favoriteService.createFavoriteByUserId(request, testUserId);
        });
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testDeleteFavoriteByUserId_正常系() {
        // Given
        Long favoriteId = 1L;

        when(favoriteRepository.findById(favoriteId))
            .thenReturn(Optional.of(testFavorite));
        when(favoriteRepository.save(any(Favorite.class)))
            .thenReturn(testFavorite);

        // When
        favoriteService.deleteFavoriteByUserId(favoriteId, testUserId);

        // Then
        assertTrue(testFavorite.getIsDeleted());
        verify(favoriteRepository).save(testFavorite);
    }

    @Test
    void testDeleteFavoriteByUserId_お気に入りが存在しない場合() {
        // Given
        Long favoriteId = 1L;

        when(favoriteRepository.findById(favoriteId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            favoriteService.deleteFavoriteByUserId(favoriteId, testUserId);
        });
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testDeleteFavoriteByUserId_権限がない場合() {
        // Given
        Long favoriteId = 1L;
        String otherUserId = "other-user-id";

        when(favoriteRepository.findById(favoriteId))
            .thenReturn(Optional.of(testFavorite));

        // When & Then
        assertThrows(ForbiddenException.class, () -> {
            favoriteService.deleteFavoriteByUserId(favoriteId, otherUserId);
        });
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void testDeleteFavoriteByBookIdAndUserId_正常系() {
        // Given
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.of(testFavorite));
        when(favoriteRepository.save(any(Favorite.class)))
            .thenReturn(testFavorite);

        // When
        favoriteService.deleteFavoriteByBookIdAndUserId(testBookId, testUserId);

        // Then
        assertTrue(testFavorite.getIsDeleted());
        verify(favoriteRepository).save(testFavorite);
    }

    @Test
    void testDeleteFavoriteByBookIdAndUserId_お気に入りが存在しない場合() {
        // Given
        when(favoriteRepository.findByUserIdAndBookId(testUserId, testBookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            favoriteService.deleteFavoriteByBookIdAndUserId(testBookId, testUserId);
        });
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }
}
