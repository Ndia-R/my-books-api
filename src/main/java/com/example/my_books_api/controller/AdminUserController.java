package com.example.my_books_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.user.UpdateSubscriptionPlanRequest;
import com.example.my_books_api.dto.user.UserProfileResponse;
import com.example.my_books_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "AdminUser", description = "管理者権限ユーザー用")
public class AdminUserController {
    private final UserService userService;

    private static final String DEFAULT_USER_START_PAGE = "1";
    private static final String DEFAULT_USER_PAGE_SIZE = "5";
    private static final String DEFAULT_USER_SORT = "createdAt.desc";

    @Operation(description = "ユーザー一覧取得")
    @GetMapping("")
    public ResponseEntity<PageResponse<UserProfileResponse>> getUsers(
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_USER_START_PAGE) @RequestParam(defaultValue = DEFAULT_USER_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_USER_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_USER_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_USER_SORT, schema = @Schema(allowableValues = {
            "updatedAt.asc",
            "updatedAt.desc",
            "createdAt.asc",
            "createdAt.desc",
            "displayName.asc",
            "displayName.desc" })) @RequestParam(defaultValue = DEFAULT_USER_SORT) String sort
    ) {
        PageResponse<UserProfileResponse> response = userService.getUsers(page, size, sort);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "特定のユーザー取得")
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable @NonNull String id) {
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(description = "ユーザー削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @NonNull String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "ユーザーのサブスクリプションプランを更新（管理者）")
    @PutMapping("/{id}/subscription")
    public ResponseEntity<UserProfileResponse> updateUserSubscriptionPlan(
        @PathVariable @NonNull String id,
        @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        UserProfileResponse response = userService.updateUserSubscriptionPlan(id, request);
        return ResponseEntity.ok(response);
    }
}
