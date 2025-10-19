package com.example.my_books_backend.service;

public interface BookStatsService {

    /**
     * 書籍の統計情報（レビュー数、平均評価、人気度）を更新する
     * 
     * @param bookId 書籍ID
     */
    void updateBookStats(String bookId);
}
