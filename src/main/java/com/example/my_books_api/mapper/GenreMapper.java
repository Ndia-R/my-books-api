package com.example.my_books_api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import com.example.my_books_api.dto.genre.GenreResponse;
import com.example.my_books_api.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreResponse toGenreResponse(Genre genre);

    List<GenreResponse> toGenreResponseList(List<Genre> genres);
}
