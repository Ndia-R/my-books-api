package com.example.my_books_api.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionPlanRequest {
    @NotNull(message = "サブスクリプションプランは必須です")
    @Pattern(regexp = "^(FREE|PREMIUM)$", message = "サブスクリプションプランは FREE または PREMIUM を指定してください")
    private String subscriptionPlan;
}
