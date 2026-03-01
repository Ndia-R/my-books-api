package com.example.my_books_api.dto.bookmark;

import org.springframework.lang.NonNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkRequest {
    @NonNull
    @NotNull
    private String bookId;

    @NonNull
    @NotNull
    @Min(1)
    private Long chapterNumber;

    @NonNull
    @NotNull
    @Min(1)
    private Long pageNumber;

    @NonNull
    @NotNull
    private String note;
}
