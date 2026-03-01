package com.example.my_books_api.dto.book_preview_setting;

import org.springframework.lang.NonNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPreviewSettingRequest {
    @NonNull
    @NotNull(message = "書籍IDは必須です")
    @Size(max = 255, message = "書籍IDは255文字以内で入力してください")
    private String bookId;

    @Min(value = -1, message = "最大章番号は-1以上で入力してください（-1は無制限）")
    private Long maxChapter;

    @Min(value = -1, message = "最大ページ番号は-1以上で入力してください（-1は無制限）")
    private Long maxPage;
}
