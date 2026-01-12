package com.example.my_books_backend.controller;

import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book-content/books")
@RequiredArgsConstructor
@Tag(name = "BookContent", description = "書籍コンテンツ")
public class BookContentController {

    private final BookService bookService;

    @Operation(description = "特定の書籍の試し読みページ")
    @GetMapping("/{id}/preview/pages/{page}")
    public ResponseEntity<BookChapterPageContentResponse> getBookChapterPageContentPreview(
        @PathVariable String id,
        @PathVariable Long page
    ) {
        // 試し読みなので常に第1章を返す
        BookChapterPageContentResponse response = bookService.getBookChapterPageContent(id, 1L, page);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍の閲覧ページ")
    @GetMapping("/{id}/chapters/{chapter}/pages/{page}")
    public ResponseEntity<BookChapterPageContentResponse> getBookChapterPageContent(
        @PathVariable String id,
        @PathVariable Long chapter,
        @PathVariable Long page
    ) {
        BookChapterPageContentResponse response = bookService.getBookChapterPageContent(id, chapter, page);
        return ResponseEntity.ok(response);
    }
}
