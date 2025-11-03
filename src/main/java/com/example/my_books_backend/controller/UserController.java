package com.example.my_books_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.dto.user.UserProfileResponse;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.service.UserService;
import com.example.my_books_backend.util.JwtClaimExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "User", description = "ユーザー")
public class UserController {
    private final UserService userService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final BookmarkService bookmarkService;
    private final JwtClaimExtractor jwtClaimExtractor;

    private static final String DEFAULT_USER_START_PAGE = "1";
    private static final String DEFAULT_USER_PAGE_SIZE = "5";
    private static final String DEFAULT_USER_SORT = "updatedAt.desc";

    @Operation(description = "ユーザーのプロフィール情報（存在しない場合は自動作成）")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        String userId = jwtClaimExtractor.getCurrentUserId();
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーのレビュー、お気に入り、ブックマークの数")
    @GetMapping("/profile-counts")
    public ResponseEntity<UserProfileCountsResponse> getUserProfileCounts() {
        String userId = jwtClaimExtractor.getCurrentUserId();
        UserProfileCountsResponse response = userService.getUserProfileCounts(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーが投稿したレビューリスト")
    @GetMapping("/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getUserReviews(
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_USER_START_PAGE) @RequestParam(defaultValue = DEFAULT_USER_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_USER_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_USER_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_USER_SORT, schema = @Schema(allowableValues = {
            "updatedAt.asc",
            "updatedAt.desc",
            "createdAt.asc",
            "createdAt.desc",
            "rating.asc",
            "rating.desc" })) @RequestParam(defaultValue = DEFAULT_USER_SORT) String sort,
        @RequestParam(required = false) String bookId
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        PageResponse<ReviewResponse> response = reviewService.getUserReviews(userId, page, size, sort, bookId);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーが追加したお気に入りリスト")
    @GetMapping("/favorites")
    public ResponseEntity<PageResponse<FavoriteResponse>> getUserFavorites(
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_USER_START_PAGE) @RequestParam(defaultValue = DEFAULT_USER_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_USER_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_USER_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_USER_SORT, schema = @Schema(allowableValues = {
            "updatedAt.asc",
            "updatedAt.desc",
            "createdAt.asc",
            "createdAt.desc" })) @RequestParam(defaultValue = DEFAULT_USER_SORT) String sort,
        @RequestParam(required = false) String bookId
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        PageResponse<FavoriteResponse> response = favoriteService.getUserFavorites(userId, page, size, sort, bookId);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーが追加したブックマークリスト")
    @GetMapping("/bookmarks")
    public ResponseEntity<PageResponse<BookmarkResponse>> getUserBookmarks(
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_USER_START_PAGE) @RequestParam(defaultValue = DEFAULT_USER_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_USER_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_USER_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_USER_SORT, schema = @Schema(allowableValues = {
            "updatedAt.asc",
            "updatedAt.desc",
            "createdAt.asc",
            "createdAt.desc" })) @RequestParam(defaultValue = DEFAULT_USER_SORT) String sort,
        @RequestParam(required = false) String bookId
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        PageResponse<BookmarkResponse> responses = bookmarkService.getUserBookmarks(userId, page, size, sort, bookId);
        return ResponseEntity.ok(responses);
    }

    @Operation(description = "ユーザーのプロフィール情報を更新")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        String userId = jwtClaimExtractor.getCurrentUserId();
        UserProfileResponse response = userService.updateUserProfile(request, userId);
        return ResponseEntity.ok(response);
    }
}