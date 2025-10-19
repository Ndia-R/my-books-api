package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import com.example.my_books_backend.dto.genre.GenreResponse;
import com.example.my_books_backend.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreResponse toGenreResponse(Genre genre);

    List<GenreResponse> toGenreResponseList(List<Genre> genres);
}
