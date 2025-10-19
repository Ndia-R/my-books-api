package com.example.my_books_backend.dto.book;

import java.sql.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private String id;
    private String title;
    private String description;
    private List<Long> genreIds;
    private List<String> authors;
    private Date publicationDate;
    private String imagePath;
    private Long reviewCount;
    private Double averageRating;
    private Double popularity;
}
