package com.example.my_books_api.dto.book_preview_setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPreviewSettingPublicResponse {
    private String bookId;
    private Long maxChapter;
    private Long maxPage;
}
