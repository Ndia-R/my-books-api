package com.example.my_books_backend.dto.book;

import java.sql.Date;
import java.util.List;
import com.example.my_books_backend.dto.genre.GenreResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailsResponse {
    private String id;
    private String title;
    private String description;
    private List<GenreResponse> genres;
    private List<String> authors;
    private String publisher;
    private Date publicationDate;
    private Long price;
    private Long pageCount;
    private String isbn;
    private String imagePath;
    private Long reviewCount;
    private Double averageRating;
    private Double popularity;
}
