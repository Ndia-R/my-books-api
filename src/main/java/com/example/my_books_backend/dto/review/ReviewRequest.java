package com.example.my_books_backend.dto.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @NotNull(message = "書籍IDは必須です")
    @Size(max = 255, message = "書籍IDは255文字以内で入力してください")
    private String bookId;

    @NotNull(message = "コメントは必須です")
    @Size(min = 1, max = 1000, message = "コメントは1文字以上1000文字以内で入力してください")
    private String comment;

    @NotNull(message = "評価は必須です")
    @DecimalMin(value = "0.0", inclusive = true, message = "評価は0.0以上で入力してください")
    @DecimalMax(value = "5.0", inclusive = true, message = "評価は5.0以下で入力してください")
    private Double rating;
}
