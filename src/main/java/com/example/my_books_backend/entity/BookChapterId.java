package com.example.my_books_backend.entity;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class BookChapterId implements Serializable {
    @Column(name = "book_id")
    private String bookId;

    @Column(name = "chapter_number")
    private Long chapterNumber;
}
