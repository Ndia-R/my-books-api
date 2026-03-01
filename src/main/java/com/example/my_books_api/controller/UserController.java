package com.example.my_books_api.controller;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.bookmark.BookmarkResponse;
import com.example.my_books_api.dto.favorite.FavoriteResponse;
import com.example.my_books_api.dto.review.ReviewResponse;
import com.example.my_books_api.dto.user.UserProfileCountsResponse;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.dto.user.UserResponse;
import com.example.my_books_api.dto.user.UpdateSubscriptionPlanRequest;
import com.example.my_books_api.dto.user.UpdateUserProfileRequest;
import com.example.my_books_api.service.BookmarkService;
import com.example.my_books_api.service.FavoriteService;
import com.example.my_books_api.service.ReviewService;
import com.example.my_books_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "User", description = "ユーザー")
public class UserController {
    private final UserService userService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final BookmarkService bookmarkService;

    private static final String DEFAULT_USER_START_PAGE = "1";
    private static final String DEFAULT_USER_PAGE_SIZE = "5";
    private static final String DEFAULT_USER_SORT = "updatedAt.desc";

    @Operation(description = "ユーザー情報を取得")
    @GetMapping("")
    public ResponseEntity<UserResponse> getUser() {
        UserResponse response = userService.getUser();
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーのプロフィール情報（存在しない場合は自動作成）")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        UserProfileResponse response = userService.getUserProfile();
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーのレビュー、お気に入り、ブックマークの数")
    @GetMapping("/profile-counts")
    public ResponseEntity<UserProfileCountsResponse> getUserProfileCounts() {
        UserProfileCountsResponse response = userService.getUserProfileCounts();
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
        PageResponse<ReviewResponse> response = reviewService.getUserReviews(page, size, sort, bookId);
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
        PageResponse<FavoriteResponse> response = favoriteService.getUserFavorites(page, size, sort, bookId);
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
        PageResponse<BookmarkResponse> responses = bookmarkService.getUserBookmarks(page, size, sort, bookId);
        return ResponseEntity.ok(responses);
    }

    @Operation(description = "ユーザーのプロフィール情報を更新")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userService.updateUserProfile(request);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーのサブスクリプションプランを更新")
    @PutMapping("/subscription")
    public ResponseEntity<UserProfileResponse> updateSubscriptionPlan(
        @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        UserProfileResponse response = userService.updateSubscriptionPlan(request);
        return ResponseEntity.ok(response);
    }
}