package com.example.my_books_backend.service.impl;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.FavoriteMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.FavoriteRepository;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.util.JwtClaimExtractor;
import com.example.my_books_backend.util.PageableUtils;
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
        String userId = jwtClaimExtractor.getCurrentUserId();

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
        String userId = jwtClaimExtractor.getCurrentUserId();

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
        String userId = jwtClaimExtractor.getCurrentUserId();
        if (!isOwner(id, userId)) {
            throw new ForbiddenException("削除する権限がありません");
        }

        Favorite favorite = favoriteRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('favorite:manage:own')")
    public void deleteFavoriteByBookId(String bookId) {
        String userId = jwtClaimExtractor.getCurrentUserId();

        Favorite favorite = favoriteRepository.findByUserIdAndBookId(userId, bookId)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }

    /**
     * 自分自身のデータかどうか
     * @param id
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    private boolean isOwner(@NonNull Long id, String userId) {
        return favoriteRepository.findById(id)
            .map(favorite -> favorite.getUser().getId().equals(userId))
            .orElse(false);
    }
}