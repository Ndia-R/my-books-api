package com.example.my_books_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.Genre;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
}
