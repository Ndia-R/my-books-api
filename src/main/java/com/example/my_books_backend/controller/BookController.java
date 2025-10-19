package com.example.my_books_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book.BookDetailsResponse;
import com.example.my_books_backend.dto.book.BookResponse;
import com.example.my_books_backend.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.service.BookService;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Book", description = "書籍")
public class BookController {
    private final BookService bookService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    private static final String DEFAULT_BOOKS_START_PAGE = "1";
    private static final String DEFAULT_BOOKS_PAGE_SIZE = "20";
    private static final String DEFAULT_BOOKS_SORT = "popularity.desc";

    private static final String DEFAULT_REVIEWS_START_PAGE = "1";
    private static final String DEFAULT_REVIEWS_PAGE_SIZE = "3";
    private static final String DEFAULT_REVIEWS_SORT = "updatedAt.desc";

    @Operation(description = "最新の書籍リスト（１０冊）")
    @GetMapping("/new-releases")
    public ResponseEntity<PageResponse<BookResponse>> getLatestBooks() {
        PageResponse<BookResponse> response = bookService.getBooks(1L, 10L, "publicationDate.desc");
        return ResponseEntity.ok(response);
    }

    @Operation(description = "タイトル検索: 指定されたタイトルから書籍を検索")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<BookResponse>> getBooksByTitleKeyword(
        @Parameter(description = "タイトルに指定された文字列を含む書籍を検索", example = "魔法", required = true) @RequestParam String q,
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_BOOKS_START_PAGE) @RequestParam(defaultValue = DEFAULT_BOOKS_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_BOOKS_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_BOOKS_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_BOOKS_SORT, schema = @Schema(allowableValues = {
            "title.asc",
            "title.desc",
            "publicationDate.asc",
            "publicationDate.desc",
            "reviewCount.asc",
            "reviewCount.desc",
            "averageRating.asc",
            "averageRating.desc",
            "popularity.asc",
            "popularity.desc" })) @RequestParam(defaultValue = DEFAULT_BOOKS_SORT) String sort
    ) {
        PageResponse<BookResponse> response = bookService.getBooksByTitleKeyword(q, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ジャンル検索: 指定されたジャンルIDと条件に基づいて書籍を検索")
    @GetMapping("/discover")
    public ResponseEntity<PageResponse<BookResponse>> getBooksByGenre(
        @Parameter(description = """
            検索対象のジャンルIDをカンマ区切りで指定
            - 単一ジャンル: 1
            - 複数ジャンル: 1,2,3
            """, example = "1,2", required = true) @RequestParam String genreIds,
        @Parameter(description = """
            ジャンル検索の条件を指定
            - SINGLE: 指定したジャンルのみ（複数指定の場合、最初のジャンルのみ）
            - AND: 指定したすべてのジャンル
            - OR: 指定したいずれかのジャンル
            """, example = "AND", required = true, schema = @Schema(allowableValues = { "SINGLE",
            "AND",
            "OR" })) @RequestParam String condition,
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_BOOKS_START_PAGE) @RequestParam(defaultValue = DEFAULT_BOOKS_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_BOOKS_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_BOOKS_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_BOOKS_SORT, schema = @Schema(allowableValues = {
            "title.asc",
            "title.desc",
            "publicationDate.asc",
            "publicationDate.desc",
            "reviewCount.asc",
            "reviewCount.desc",
            "averageRating.asc",
            "averageRating.desc",
            "popularity.asc",
            "popularity.desc" })) @RequestParam(defaultValue = DEFAULT_BOOKS_SORT) String sort
    ) {
        PageResponse<BookResponse> response = bookService.getBooksByGenre(genreIds, condition, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍の詳細")
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailsResponse> getBookDetails(@PathVariable String id) {
        BookDetailsResponse response = bookService.getBookDetails(id);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍の目次")
    @GetMapping("/{id}/toc")
    public ResponseEntity<BookTableOfContentsResponse> getBookTableOfContents(
        @PathVariable String id
    ) {
        BookTableOfContentsResponse response = bookService.getBookTableOfContents(id);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍のレビューリスト")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getBookReviews(
        @PathVariable String id,
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_REVIEWS_START_PAGE) @RequestParam(defaultValue = DEFAULT_REVIEWS_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_REVIEWS_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_REVIEWS_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_REVIEWS_SORT, schema = @Schema(allowableValues = {
            "updatedAt.asc",
            "updatedAt.desc",
            "createdAt.asc",
            "createdAt.desc",
            "rating.asc",
            "rating.desc" })) @RequestParam(defaultValue = DEFAULT_REVIEWS_SORT) String sort
    ) {
        PageResponse<ReviewResponse> response = reviewService.getBookReviews(id, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍のレビュー統計")
    @GetMapping("/{id}/stats/reviews")
    public ResponseEntity<ReviewStatsResponse> getBookReviewStats(@PathVariable String id) {
        ReviewStatsResponse response = reviewService.getBookReviewStats(id);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定の書籍のお気に入り統計")
    @GetMapping("/{id}/stats/favorites")
    public ResponseEntity<FavoriteStatsResponse> getBookFavoriteStats(@PathVariable String id) {
        FavoriteStatsResponse response = favoriteService.getBookFavoriteStats(id);
        return ResponseEntity.ok(response);
    }
}
