package com.example.my_books_api.entity;

import java.util.List;

import org.springframework.lang.NonNull;

import com.example.my_books_api.entity.base.EntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class User extends EntityBase {
    @Id
    @NonNull
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "subscription_plan", nullable = false)
    private String subscriptionPlan;

    @OneToMany(mappedBy = "user")
    private List<Review> reviews;

    @OneToMany(mappedBy = "user")
    private List<Favorite> favorites;

    @OneToMany(mappedBy = "user")
    private List<Bookmark> bookmarks;

}
