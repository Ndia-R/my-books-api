package com.example.my_books_api.dto.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String displayName; // DBから取得（アプリ管理・表示名）
    private String avatarPath;
    private String subscriptionPlan;

    // JWTクレームから取得（IdP管理）
    private String username;
    private String email;
    private String familyName;
    private String givenName;
    private List<String> roles;
    private List<String> groups;
}
