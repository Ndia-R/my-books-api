package com.example.my_books_backend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private Long currentPage; // ページ番号は1ベース
    private Long pageSize;
    private Long totalPages;
    private Long totalItems;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private List<T> data;
}
