package com.example.my_books_api.controller;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingRequest;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingResponse;
import com.example.my_books_api.service.BookPreviewSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/preview-settings")
@RequiredArgsConstructor
@Tag(name = "BookPreviewSetting", description = "試し読み設定")
public class BookPreviewSettingController {

    private final BookPreviewSettingService bookPreviewSettingService;

    private static final String DEFAULT_START_PAGE = "1";
    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final String DEFAULT_SORT = "createdAt.desc";

    @Operation(description = "試し読み設定一覧取得")
    @GetMapping("")
    public ResponseEntity<PageResponse<BookPreviewSettingResponse>> getPreviewSettings(
        @Parameter(description = "ページ番号（1ベース）", example = DEFAULT_START_PAGE) @RequestParam(defaultValue = DEFAULT_START_PAGE) Long page,
        @Parameter(description = "1ページあたりの件数", example = DEFAULT_PAGE_SIZE) @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Long size,
        @Parameter(description = "ソート条件", example = DEFAULT_SORT, schema = @Schema(allowableValues = {
            "createdAt.asc",
            "createdAt.desc",
            "updatedAt.asc",
            "updatedAt.desc",
            "maxChapter.asc",
            "maxChapter.desc",
            "maxPage.asc",
            "maxPage.desc" })) @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        PageResponse<BookPreviewSettingResponse> response = bookPreviewSettingService.getPreviewSettings(
            page,
            size,
            sort
        );
        return ResponseEntity.ok(response);
    }

    @Operation(description = "試し読み設定取得")
    @GetMapping("/{id}")
    public ResponseEntity<BookPreviewSettingResponse> getPreviewSetting(
        @PathVariable @NonNull Long id
    ) {
        BookPreviewSettingResponse response = bookPreviewSettingService.getPreviewSetting(id);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "試し読み設定作成")
    @PostMapping("")
    public ResponseEntity<BookPreviewSettingResponse> createPreviewSetting(
        @Valid @RequestBody BookPreviewSettingRequest request
    ) {
        BookPreviewSettingResponse response = bookPreviewSettingService.createPreviewSetting(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "試し読み設定更新")
    @PutMapping("/{id}")
    public ResponseEntity<BookPreviewSettingResponse> updatePreviewSetting(
        @PathVariable @NonNull Long id,
        @Valid @RequestBody BookPreviewSettingRequest request
    ) {
        BookPreviewSettingResponse response = bookPreviewSettingService.updatePreviewSetting(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "試し読み設定削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePreviewSetting(
        @PathVariable @NonNull Long id
    ) {
        bookPreviewSettingService.deletePreviewSetting(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "試し読み設定削除（書籍ID指定）")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> deletePreviewSettingByBookId(
        @PathVariable @NonNull String bookId
    ) {
        bookPreviewSettingService.deletePreviewSettingByBookId(bookId);
        return ResponseEntity.noContent().build();
    }
}
