package com.example.my_books_backend.dto.genre;

import org.hibernate.validator.constraints.Length;
import org.springframework.lang.NonNull;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequest {
    @NonNull
    @NotNull
    @Length(max = 50)
    private String name;

    @NonNull
    @NotNull
    @Length(max = 255)
    private String description;
}
