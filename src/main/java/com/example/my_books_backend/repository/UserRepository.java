package com.example.my_books_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.dto.user.UserProfileCountsResponse;
import com.example.my_books_backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * ユーザーを取得（論理削除されていないもののみ）
     *
     * @param id ユーザーID
     * @return ユーザー
     */
    Optional<User> findByIdAndIsDeletedFalse(String id);

    // ユーザーのお気に入り、ブックマーク、レビューの数を取得
    @Query("""
        SELECT new com.example.my_books_backend.dto.user.UserProfileCountsResponse(
            (SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId AND f.isDeleted = false),
            (SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.isDeleted = false),
            (SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.isDeleted = false)
        )
        """)
    UserProfileCountsResponse getUserProfileCountsResponse(@Param("userId") String userId);
}
