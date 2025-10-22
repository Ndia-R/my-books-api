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
import com.example.my_books_backend.dto.user.UpdateUserEmailRequest;
import com.example.my_books_backend.dto.user.UpdateUserPasswordRequest;
import com.example.my_books_backend.dto.user.UpdateUserProfileRequest;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.service.ReviewService;
import com.example.my_books_backend.service.UserService;
import com.example.my_books_backend.util.SecurityUtils;
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
    private final SecurityUtils securityUtils;

    private static final String DEFAULT_USER_START_PAGE = "1";
    private static final String DEFAULT_USER_PAGE_SIZE = "5";
    private static final String DEFAULT_USER_SORT = "updatedAt.desc";

    @Operation(description = "ユーザーのプロフィール情報")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        User user = securityUtils.getCurrentUser();
        UserProfileResponse response = userService.getUserProfile(user);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ユーザーのレビュー、お気に入り、ブックマークの数")
    @GetMapping("/profile-counts")
    public ResponseEntity<UserProfileCountsResponse> getUserProfileCounts() {
        User user = securityUtils.getCurrentUser();
        UserProfileCountsResponse response = userService.getUserProfileCounts(user);
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
        User user = securityUtils.getCurrentUser();
        PageResponse<ReviewResponse> response = reviewService.getUserReviews(user, page, size, sort, bookId);
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
        User user = securityUtils.getCurrentUser();
        PageResponse<FavoriteResponse> response = favoriteService.getUserFavorites(user, page, size, sort, bookId);
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
        User user = securityUtils.getCurrentUser();
        PageResponse<BookmarkResponse> responses = bookmarkService.getUserBookmarks(user, page, size, sort, bookId);
        return ResponseEntity.ok(responses);
    }

    @Operation(description = "ユーザーのプロフィール情報を更新")
    @PutMapping("/profile")
    public ResponseEntity<Void> updateUserProfile(
        @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        userService.updateUserProfile(request, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "ユーザーのメールアドレスを更新")
    @PutMapping("/email")
    public ResponseEntity<Void> updateUserEmail(
        @Valid @RequestBody UpdateUserEmailRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        userService.updateUserEmail(request, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "ユーザーのパスワードを更新")
    @PutMapping("/password")
    public ResponseEntity<Void> updateUserPassword(
        @Valid @RequestBody UpdateUserPasswordRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        userService.updateUserPassword(request, user);
        return ResponseEntity.noContent().build();
    }
}
