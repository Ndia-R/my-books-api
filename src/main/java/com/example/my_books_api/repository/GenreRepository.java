package com.example.my_books_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_api.entity.Genre;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    // 名前で検索
    Optional<Genre> findByName(String name);
}
