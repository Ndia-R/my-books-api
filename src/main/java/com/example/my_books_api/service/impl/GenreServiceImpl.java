package com.example.my_books_api.service.impl;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_api.dto.genre.GenreRequest;
import com.example.my_books_api.dto.genre.GenreResponse;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.GenreMapper;
import com.example.my_books_api.repository.GenreRepository;
import com.example.my_books_api.service.GenreService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<GenreResponse> getAllGenres() {
        List<Genre> genres = genreRepository.findAll();
        return genreMapper.toGenreResponseList(genres);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public GenreResponse getGenreById(@NonNull Long id) {
        Genre genre = genreRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Genre not found"));
        return genreMapper.toGenreResponse(genre);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public List<GenreResponse> getGenresByIds(@NonNull List<Long> ids) {
        List<Genre> genres = genreRepository.findAllById(ids);
        return genreMapper.toGenreResponseList(genres);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('genre:manage:all')")
    public GenreResponse createGenre(GenreRequest request) {
        // 同名のジャンルが既に存在するか確認
        if (genreRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("すでに同じ名前のジャンルが存在します。");
        }

        Genre genre = new Genre();
        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        Genre savedGenre = genreRepository.save(genre);
        return genreMapper.toGenreResponse(savedGenre);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('genre:manage:all')")
    public GenreResponse updateGenre(@NonNull Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Genre not found"));

        if (request.getName() != null && !request.getName().equals(genre.getName())) {
            if (genreRepository.findByName(request.getName()).isPresent()) {
                throw new ConflictException("すでに同じ名前のジャンルが存在します。");
            }
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
    @PreAuthorize("hasAuthority('genre:manage:all')")
    public void deleteGenre(@NonNull Long id) {
        genreRepository.findById(id).orElseThrow(() -> new NotFoundException("Genre not found"));
        genreRepository.deleteById(id);
    }
}
