package com.example.my_books_backend.controller;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.my_books_backend.config.SecurityConfig;
import com.example.my_books_backend.dto.genre.GenreRequest;
import com.example.my_books_backend.dto.genre.GenreResponse;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.service.GenreService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenreController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null") // IDE null safety warnings for Spring Test framework methods
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenreService genreService;

    @Test
    void testGetAllGenres_認証なしでアクセス可能_パブリックエンドポイント() throws Exception {
        // Given
        GenreResponse genre1 = new GenreResponse();
        genre1.setId(1L);
        genre1.setName("ロマンス");

        GenreResponse genre2 = new GenreResponse();
        genre2.setId(2L);
        genre2.setName("ミステリー");

        List<GenreResponse> genres = Arrays.asList(genre1, genre2);

        when(genreService.getAllGenres()).thenReturn(genres);

        // When & Then
        mockMvc.perform(get("/genres"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("ロマンス"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("ミステリー"));

        verify(genreService).getAllGenres();
    }

    @Test
    void testGetAllGenres_空のリスト() throws Exception {
        // Given
        when(genreService.getAllGenres()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/genres"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(genreService).getAllGenres();
    }

    @Test
    void testGetGenreById_認証なしでアクセス可能_パブリックエンドポイント() throws Exception {
        // Given
        Long genreId = 1L;
        GenreResponse genre = new GenreResponse();
        genre.setId(genreId);
        genre.setName("ロマンス");

        when(genreService.getGenreById(genreId)).thenReturn(genre);

        // When & Then
        mockMvc.perform(get("/genres/{id}", genreId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(genreId))
            .andExpect(jsonPath("$.name").value("ロマンス"));

        verify(genreService).getGenreById(genreId);
    }

    @Test
    void testGetGenreById_存在しないジャンル_404NotFound() throws Exception {
        // Given
        Long genreId = 999L;

        when(genreService.getGenreById(genreId))
            .thenThrow(new NotFoundException("ジャンルが見つかりません"));

        // When & Then
        mockMvc.perform(get("/genres/{id}", genreId))
            .andExpect(status().isNotFound());

        verify(genreService).getGenreById(genreId);
    }

    @Test
    void testCreateGenre_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        String requestBody = "{\"name\":\"ホラー\"}";

        // When & Then
        mockMvc.perform(post("/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(genreService, never()).createGenre(any(GenreRequest.class));
    }

    @Test
    void testCreateGenre_正常系() throws Exception {
        // Given
        String requestBody = "{\"name\":\"ホラー\",\"description\":\"怖い物語のジャンル\"}";

        GenreResponse response = new GenreResponse();
        response.setId(1L);
        response.setName("ホラー");

        when(genreService.createGenre(any(GenreRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/genres/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("ホラー"));

        verify(genreService).createGenre(any(GenreRequest.class));
    }

    @Test
    void testCreateGenre_バリデーションエラー_nameなし() throws Exception {
        // Given
        String requestBody = "{}";

        // When & Then
        mockMvc.perform(post("/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isBadRequest());

        verify(genreService, never()).createGenre(any(GenreRequest.class));
    }

    @Test
    void testCreateGenre_重複するジャンル名_409Conflict() throws Exception {
        // Given
        String requestBody = "{\"name\":\"ロマンス\",\"description\":\"恋愛物語のジャンル\"}";

        when(genreService.createGenre(any(GenreRequest.class)))
            .thenThrow(new ConflictException("既に存在するジャンル名です"));

        // When & Then
        mockMvc.perform(post("/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isConflict());

        verify(genreService).createGenre(any(GenreRequest.class));
    }

    @Test
    void testUpdateGenre_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long genreId = 1L;
        String requestBody = "{\"name\":\"更新されたジャンル\"}";

        // When & Then
        mockMvc.perform(put("/genres/{id}", genreId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());

        verify(genreService, never()).updateGenre(anyLong(), any(GenreRequest.class));
    }

    @Test
    void testUpdateGenre_正常系() throws Exception {
        // Given
        Long genreId = 1L;
        String requestBody = "{\"name\":\"更新されたジャンル\",\"description\":\"更新された説明\"}";

        GenreResponse response = new GenreResponse();
        response.setId(genreId);
        response.setName("更新されたジャンル");

        when(genreService.updateGenre(eq(genreId), any(GenreRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/genres/{id}", genreId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(genreId))
            .andExpect(jsonPath("$.name").value("更新されたジャンル"));

        verify(genreService).updateGenre(eq(genreId), any(GenreRequest.class));
    }

    @Test
    void testUpdateGenre_存在しないジャンル_404NotFound() throws Exception {
        // Given
        Long genreId = 999L;
        String requestBody = "{\"name\":\"更新されたジャンル\",\"description\":\"更新された説明\"}";

        when(genreService.updateGenre(eq(genreId), any(GenreRequest.class)))
            .thenThrow(new NotFoundException("ジャンルが見つかりません"));

        // When & Then
        mockMvc.perform(put("/genres/{id}", genreId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(genreService).updateGenre(eq(genreId), any(GenreRequest.class));
    }

    @Test
    void testUpdateGenre_重複するジャンル名_409Conflict() throws Exception {
        // Given
        Long genreId = 1L;
        String requestBody = "{\"name\":\"既存のジャンル\",\"description\":\"既存の説明\"}";

        when(genreService.updateGenre(eq(genreId), any(GenreRequest.class)))
            .thenThrow(new ConflictException("既に存在するジャンル名です"));

        // When & Then
        mockMvc.perform(put("/genres/{id}", genreId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(jwt()))
            .andExpect(status().isConflict());

        verify(genreService).updateGenre(eq(genreId), any(GenreRequest.class));
    }

    @Test
    void testDeleteGenre_認証なしでアクセス_401Unauthorized() throws Exception {
        // Given
        Long genreId = 1L;

        // When & Then
        mockMvc.perform(delete("/genres/{id}", genreId))
            .andExpect(status().isUnauthorized());

        verify(genreService, never()).deleteGenre(anyLong());
    }

    @Test
    void testDeleteGenre_正常系() throws Exception {
        // Given
        Long genreId = 1L;

        doNothing().when(genreService).deleteGenre(genreId);

        // When & Then
        mockMvc.perform(delete("/genres/{id}", genreId)
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(genreService).deleteGenre(genreId);
    }

    @Test
    void testDeleteGenre_存在しないジャンル_404NotFound() throws Exception {
        // Given
        Long genreId = 999L;

        doThrow(new NotFoundException("ジャンルが見つかりません"))
            .when(genreService).deleteGenre(genreId);

        // When & Then
        mockMvc.perform(delete("/genres/{id}", genreId)
                .with(jwt()))
            .andExpect(status().isNotFound());

        verify(genreService).deleteGenre(genreId);
    }
}
