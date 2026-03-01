package com.example.my_books_api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.favorite.FavoriteResponse;
import com.example.my_books_api.entity.Favorite;
import com.example.my_books_api.util.PageableUtils;

@Mapper(componentModel = "spring", uses = { BookMapper.class })
public interface FavoriteMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "book", source = "book")
    FavoriteResponse toFavoriteResponse(Favorite favorite);

    List<FavoriteResponse> toFavoriteResponseList(List<Favorite> favorites);

    default PageResponse<FavoriteResponse> toPageResponse(Page<Favorite> favorites) {
        List<FavoriteResponse> responses = toFavoriteResponseList(favorites.getContent());
        return PageableUtils.toPageResponse(favorites, responses);
    }
}
