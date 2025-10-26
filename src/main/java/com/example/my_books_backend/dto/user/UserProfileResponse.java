package com.example.my_books_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String email; // JWTクレームから取得（IdP管理）
    private String name; // JWTクレームから取得（IdP管理・本名）
    private String displayName; // DBから取得（アプリ管理・表示名）
    private String avatarPath;
}
