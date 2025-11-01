package com.example.my_books_backend.dto.user;

import org.springframework.lang.NonNull;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    @NonNull
    @NotNull
    private String displayName;
    @NonNull
    @NotNull
    private String avatarPath;
}
