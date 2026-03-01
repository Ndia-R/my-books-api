package com.example.my_books_api.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

import java.sql.Date;
import java.util.List;

/**
 * 書籍作成・更新リクエストDTO
 */
@Data
public class BookRequest {

    /**
     * 書籍ID（作成時は必須、更新時は使用しない）
     */
    @NotNull
    @NonNull
    @NotBlank(message = "書籍IDは必須です")
    @Size(max = 255, message = "書籍IDは255文字以内で入力してください")
    private String id;

    /**
     * タイトル
     */
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    private String title;

    /**
     * 説明
     */
    @NotBlank(message = "説明は必須です")
    private String description;

    /**
     * 著者（カンマ区切り文字列）
     */
    @NotBlank(message = "著者は必須です")
    private String authors;

    /**
     * 出版社
     */
    @NotBlank(message = "出版社は必須です")
    @Size(max = 255, message = "出版社は255文字以内で入力してください")
    private String publisher;

    /**
     * 出版日
     */
    @NotNull(message = "出版日は必須です")
    private Date publicationDate;

    /**
     * 価格（円）
     */
    @NotNull(message = "価格は必須です")
    @Positive(message = "価格は正の数値で入力してください")
    private Long price;

    /**
     * ページ数
     */
    @NotNull(message = "ページ数は必須です")
    @Positive(message = "ページ数は正の数値で入力してください")
    private Long pageCount;

    /**
     * ISBN
     */
    @NotBlank(message = "ISBNは必須です")
    @Size(max = 20, message = "ISBNは20文字以内で入力してください")
    private String isbn;

    /**
     * 画像パス（オプショナル）
     */
    @Size(max = 500, message = "画像パスは500文字以内で入力してください")
    private String imagePath;

    /**
     * ジャンルIDリスト（少なくとも1つ必要）
     */
    @NotNull(message = "ジャンルは必須です")
    @Size(min = 1, message = "少なくとも1つのジャンルを選択してください")
    private List<Long> genreIds;
}
