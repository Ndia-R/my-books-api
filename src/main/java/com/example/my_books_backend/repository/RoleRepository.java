package com.example.my_books_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.Role;
import com.example.my_books_backend.entity.enums.RoleName;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // ロール名からロールを取得
    Role findByName(RoleName name);
}
