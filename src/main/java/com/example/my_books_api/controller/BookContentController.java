package com.example.my_books_api.controller;

import com.example.my_books_api.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_api.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book-content")
@RequiredArgsConstructor
@Tag(name = "BookContent", description = "書籍コンテンツ")
public class BookContentController {

    private final BookService bookService;

    @Operation(description = "特定の書籍の閲覧ページ（試し読み）")
    @GetMapping("/preview/books/{id}/chapters/{chapter}/pages/{page}")
    public ResponseEntity<BookChapterPageContentResponse> getBookChapterPagePreview(
        @PathVariable String id,
        @PathVariable Long chapter,
        @PathVariable Long page
    ) {
        BookChapterPageContentResponse response = bookService.getBookChapterPagePreview(id, chapter, page);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍の閲覧ページ")
    @GetMapping("books/{id}/chapters/{chapter}/pages/{page}")
    public ResponseEntity<BookChapterPageContentResponse> getBookChapterPageContent(
        @PathVariable String id,
        @PathVariable Long chapter,
        @PathVariable Long page
    ) {
        BookChapterPageContentResponse response = bookService.getBookChapterPageContent(id, chapter, page);
        return ResponseEntity.ok(response);
    }
}
