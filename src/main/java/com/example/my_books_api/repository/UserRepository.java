package com.example.my_books_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.my_books_api.dto.user.UserProfileCountsResponse;
import com.example.my_books_api.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // 1件取得
    Optional<User> findByIdAndIsDeletedFalse(String id);

    // ユーザー一覧取得
    Page<User> findByIsDeletedFalse(Pageable pageable);

    // ユーザーのお気に入り、ブックマーク、レビューの数を取得
    @Query("""
        SELECT new com.example.my_books_api.dto.user.UserProfileCountsResponse(
            (SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId AND f.isDeleted = false),
            (SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.isDeleted = false),
            (SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.isDeleted = false)
        )
        """)
    UserProfileCountsResponse getUserProfileCountsResponse(@Param("userId") String userId);

    // サブスクリプションプラン取得
    @Query("SELECT u.subscriptionPlan FROM User u WHERE u.id = :userId AND u.isDeleted = false")
    Optional<String> findSubscriptionPlanByUserId(@Param("userId") String userId);

    // 2クエリ戦略用：IDリストから関連データを含むリストを取得
    @Query("""
        SELECT DISTINCT u
        FROM User u
        WHERE u.id IN :ids
        """)
    List<User> findAllByIdInWithRelations(@Param("ids") List<String> ids);
}
