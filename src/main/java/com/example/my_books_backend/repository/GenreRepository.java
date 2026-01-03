package com.example.my_books_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.Genre;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    // 全件取得
    List<Genre> findByIsDeletedFalse();

    // 1件取得
    Optional<Genre> findByIdAndIsDeletedFalse(Long id);

    // 複数ID指定取得
    List<Genre> findByIdInAndIsDeletedFalse(Collection<Long> ids);

    // 削除済みも含めて名前で検索
    Optional<Genre> findByName(String name);
}
