package com.example.my_books_api.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStatsResponse {
    private String bookId;
    private Long favoriteCount;
}
