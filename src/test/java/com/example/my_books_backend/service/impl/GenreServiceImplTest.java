package com.example.my_books_backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.my_books_backend.dto.genre.GenreRequest;
import com.example.my_books_backend.dto.genre.GenreResponse;
import com.example.my_books_backend.entity.Genre;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.GenreMapper;
import com.example.my_books_backend.repository.GenreRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private GenreMapper genreMapper;

    @InjectMocks
    private GenreServiceImpl genreService;

    private Genre testGenre;
    private Long testGenreId;

    @BeforeEach
    void setUp() {
        testGenreId = 1L;

        testGenre = new Genre();
        testGenre.setId(testGenreId);
        testGenre.setName("Test Genre");
        testGenre.setDescription("Test Description");
    }

    @Test
    void testGetAllGenres_正常系() {
        // Given
        List<Genre> genres = Arrays.asList(testGenre);
        List<GenreResponse> expectedResponses = Arrays.asList(new GenreResponse());

        when(genreRepository.findAll())
            .thenReturn(genres);
        when(genreMapper.toGenreResponseList(genres))
            .thenReturn(expectedResponses);

        // When
        List<GenreResponse> result = genreService.getAllGenres();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(genreRepository).findAll();
        verify(genreMapper).toGenreResponseList(genres);
    }

    @Test
    void testGetGenreById_正常系() {
        // Given
        GenreResponse expectedResponse = new GenreResponse();
        expectedResponse.setId(testGenreId);

        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.of(testGenre));
        when(genreMapper.toGenreResponse(testGenre))
            .thenReturn(expectedResponse);

        // When
        GenreResponse result = genreService.getGenreById(testGenreId);

        // Then
        assertNotNull(result);
        assertEquals(testGenreId, result.getId());
        verify(genreRepository).findById(testGenreId);
        verify(genreMapper).toGenreResponse(testGenre);
    }

    @Test
    void testGetGenreById_ジャンルが存在しない場合() {
        // Given
        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            genreService.getGenreById(testGenreId);
        });
        verify(genreRepository).findById(testGenreId);
    }

    @Test
    void testGetGenresByIds_正常系() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Genre> genres = Arrays.asList(testGenre);
        List<GenreResponse> expectedResponses = Arrays.asList(new GenreResponse());

        when(genreRepository.findAllById(ids))
            .thenReturn(genres);
        when(genreMapper.toGenreResponseList(genres))
            .thenReturn(expectedResponses);

        // When
        List<GenreResponse> result = genreService.getGenresByIds(ids);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(genreRepository).findAllById(ids);
        verify(genreMapper).toGenreResponseList(genres);
    }

    @Test
    void testCreateGenre_正常系() {
        // Given
        GenreRequest request = new GenreRequest();
        request.setName("New Genre");
        request.setDescription("New Description");

        GenreResponse expectedResponse = new GenreResponse();
        expectedResponse.setId(testGenreId);

        when(genreRepository.save(any(Genre.class)))
            .thenReturn(testGenre);
        when(genreMapper.toGenreResponse(testGenre))
            .thenReturn(expectedResponse);

        // When
        GenreResponse result = genreService.createGenre(request);

        // Then
        assertNotNull(result);
        verify(genreRepository).save(argThat(genre ->
            "New Genre".equals(genre.getName()) &&
            "New Description".equals(genre.getDescription())
        ));
        verify(genreMapper).toGenreResponse(testGenre);
    }

    @Test
    void testUpdateGenre_正常系_名前と説明の両方を更新() {
        // Given
        GenreRequest request = new GenreRequest();
        request.setName("Updated Genre");
        request.setDescription("Updated Description");

        GenreResponse expectedResponse = new GenreResponse();

        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.of(testGenre));
        when(genreRepository.save(any(Genre.class)))
            .thenReturn(testGenre);
        when(genreMapper.toGenreResponse(testGenre))
            .thenReturn(expectedResponse);

        // When
        GenreResponse result = genreService.updateGenre(testGenreId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Genre", testGenre.getName());
        assertEquals("Updated Description", testGenre.getDescription());
        verify(genreRepository).save(testGenre);
    }

    @Test
    void testUpdateGenre_正常系_名前のみ更新() {
        // Given
        GenreRequest request = new GenreRequest();
        request.setName("Updated Genre");

        GenreResponse expectedResponse = new GenreResponse();

        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.of(testGenre));
        when(genreRepository.save(any(Genre.class)))
            .thenReturn(testGenre);
        when(genreMapper.toGenreResponse(testGenre))
            .thenReturn(expectedResponse);

        // When
        GenreResponse result = genreService.updateGenre(testGenreId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Genre", testGenre.getName());
        verify(genreRepository).save(testGenre);
    }

    @Test
    void testUpdateGenre_正常系_説明のみ更新() {
        // Given
        GenreRequest request = new GenreRequest();
        request.setDescription("Updated Description");

        GenreResponse expectedResponse = new GenreResponse();

        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.of(testGenre));
        when(genreRepository.save(any(Genre.class)))
            .thenReturn(testGenre);
        when(genreMapper.toGenreResponse(testGenre))
            .thenReturn(expectedResponse);

        // When
        GenreResponse result = genreService.updateGenre(testGenreId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Description", testGenre.getDescription());
        verify(genreRepository).save(testGenre);
    }

    @Test
    void testUpdateGenre_ジャンルが存在しない場合() {
        // Given
        GenreRequest request = new GenreRequest();
        request.setName("Updated Genre");

        when(genreRepository.findById(testGenreId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            genreService.updateGenre(testGenreId, request);
        });
        verify(genreRepository).findById(testGenreId);
        verify(genreRepository, never()).save(any(Genre.class));
    }

    @Test
    void testDeleteGenre_正常系() {
        // Given
        when(genreRepository.existsById(testGenreId))
            .thenReturn(true);

        // When
        genreService.deleteGenre(testGenreId);

        // Then
        verify(genreRepository).existsById(testGenreId);
        verify(genreRepository).deleteById(testGenreId);
    }

    @Test
    void testDeleteGenre_ジャンルが存在しない場合() {
        // Given
        when(genreRepository.existsById(testGenreId))
            .thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            genreService.deleteGenre(testGenreId);
        });
        verify(genreRepository).existsById(testGenreId);
        verify(genreRepository, never()).deleteById(anyLong());
    }
}
