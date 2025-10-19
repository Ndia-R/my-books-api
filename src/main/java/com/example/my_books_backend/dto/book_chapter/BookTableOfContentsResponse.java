package com.example.my_books_backend.dto.book_chapter;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookTableOfContentsResponse {
    private String bookId;
    private String title;
    private List<BookChapterResponse> chapters;
}
