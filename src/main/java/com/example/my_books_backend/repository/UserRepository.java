package com.example.my_books_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // ユーザー情報取得（ロールも同時に取得する）
    @EntityGraph(attributePaths = { "roles" })
    @NonNull
    Optional<User> findById(@NonNull Long id);

    // メールアドレスが存在するか
    Boolean existsByEmail(String email);

    // ユーザーのお気に入り、ブックマーク、レビューの数を取得
    @Query("""
        SELECT new com.example.my_books_backend.dto.user.UserProfileCountsResponse(
            (SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId AND f.isDeleted = false),
            (SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.isDeleted = false),
            (SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.isDeleted = false)
        )
        """)
    UserProfileCountsResponse getUserProfileCountsResponse(@Param("userId") Long userId);
}
