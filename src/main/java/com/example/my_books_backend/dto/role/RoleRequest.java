package com.example.my_books_backend.dto.role;

import org.hibernate.validator.constraints.Length;
import com.example.my_books_backend.entity.enums.RoleName;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {
    @NotNull
    @Enumerated(EnumType.STRING)
    private RoleName name;

    @NotNull
    @Length(max = 255)
    private String description;
}
