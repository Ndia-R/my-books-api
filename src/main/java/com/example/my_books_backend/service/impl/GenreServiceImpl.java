package com.example.my_books_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.genre.GenreRequest;
import com.example.my_books_backend.dto.genre.GenreResponse;
import com.example.my_books_backend.entity.Genre;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.GenreMapper;
import com.example.my_books_backend.repository.GenreRepository;
import com.example.my_books_backend.service.GenreService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAuthority('genre:manage')")
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    @PreAuthorize("permitAll()")
    public List<GenreResponse> getAllGenres() {
        List<Genre> genres = genreRepository.findByIsDeletedFalse();
        return genreMapper.toGenreResponseList(genres);
    }

    @Override
    @PreAuthorize("permitAll()")
    public GenreResponse getGenreById(@NonNull Long id) {
        Genre genre = genreRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Genre not found"));
        return genreMapper.toGenreResponse(genre);
    }

    @Override
    @PreAuthorize("permitAll()")
    public List<GenreResponse> getGenresByIds(@NonNull List<Long> ids) {
        List<Genre> genres = genreRepository.findByIdInAndIsDeletedFalse(ids);
        return genreMapper.toGenreResponseList(genres);
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        // 同名のジャンルが既に存在するか確認（削除済みも含めて検索）
        Optional<Genre> existingGenre = genreRepository.findByName(request.getName());

        Genre genre;
        if (existingGenre.isPresent()) {
            genre = existingGenre.get();
            if (genre.getIsDeleted()) {
                genre.setIsDeleted(false);
                genre.setCreatedAt(LocalDateTime.now());
                genre.setDescription(request.getDescription());
            } else {
                throw new ConflictException("すでに同じ名前のジャンルが存在します。");
            }
        } else {
            genre = new Genre();
            genre.setName(request.getName());
            genre.setDescription(request.getDescription());
        }

        Genre savedGenre = genreRepository.save(genre);
        return genreMapper.toGenreResponse(savedGenre);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(@NonNull Long id, GenreRequest request) {
        Genre genre = genreRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Genre not found"));

        if (request.getName() != null) {
            genre.setName(request.getName());
        }
        if (request.getDescription() != null) {
            genre.setDescription(request.getDescription());
        }

        Genre savedGenre = genreRepository.save(genre);
        return genreMapper.toGenreResponse(savedGenre);
    }

    @Override
    @Transactional
    public void deleteGenre(@NonNull Long id) {
        Genre genre = genreRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Genre not found"));

        genre.setIsDeleted(true);
        genreRepository.save(genre);
    }
}
