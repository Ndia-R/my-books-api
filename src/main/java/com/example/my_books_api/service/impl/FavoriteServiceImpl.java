package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.favorite.FavoriteRequest;
import com.example.my_books_api.dto.favorite.FavoriteResponse;
import com.example.my_books_api.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.Favorite;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.ForbiddenException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.FavoriteMapper;
import com.example.my_books_api.repository.BookRepository;
import com.example.my_books_api.repository.FavoriteRepository;
import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.FavoriteService;
import com.example.my_books_api.util.JwtClaimExtractor;
import com.example.my_books_api.util.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final FavoriteMapper favoriteMapper;

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final JwtClaimExtractor jwtClaimExtractor;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public FavoriteStatsResponse getBookFavoriteStats(String bookId) {
        return favoriteRepository.getFavoriteStatsResponse(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('favorite:manage:own')")
    public PageResponse<FavoriteResponse> getUserFavorites(
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        String userId = jwtClaimExtractor.getUserId();

        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.FAVORITE_ALLOWED_FIELDS
        );
        Page<Favorite> pageObj = (bookId == null)
            ? favoriteRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
            : favoriteRepository.findByUserIdAndIsDeletedFalseAndBookId(userId, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Favorite> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            favoriteRepository::findAllByIdInWithRelations,
            Favorite::getId
        );

        return favoriteMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('favorite:manage:own')")
    public FavoriteResponse createFavorite(FavoriteRequest request) {
        String userId = jwtClaimExtractor.getUserId();

        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new NotFoundException("Book not found"));

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserIdAndBookId(userId, request.getBookId());

        Favorite favorite;
        if (existingFavorite.isPresent()) {
            favorite = existingFavorite.get();
            if (favorite.getIsDeleted()) {
                favorite.setIsDeleted(false);
                favorite.setCreatedAt(LocalDateTime.now());
            } else {
                throw new ConflictException("すでにこの書籍にはお気に入りが登録されています。");
            }
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
            favorite = new Favorite();
            favorite.setUser(user);
        }
        favorite.setBook(book);

        Favorite savedFavorite = favoriteRepository.save(favorite);
        return favoriteMapper.toFavoriteResponse(savedFavorite);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('favorite:manage:own')")
    public void deleteFavorite(@NonNull Long id) {
        Favorite favorite = favoriteRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        String userId = jwtClaimExtractor.getUserId();
        if (!favorite.getUser().getId().equals(userId)) {
            throw new ForbiddenException("削除する権限がありません");
        }

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('favorite:manage:own')")
    public void deleteFavoriteByBookId(String bookId) {
        String userId = jwtClaimExtractor.getUserId();

        Favorite favorite = favoriteRepository.findByUserIdAndBookId(userId, bookId)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }
}