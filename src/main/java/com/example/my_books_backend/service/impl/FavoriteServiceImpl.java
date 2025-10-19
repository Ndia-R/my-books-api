package com.example.my_books_backend.service.impl;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.favorite.FavoriteRequest;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.dto.PageResponse;
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
import com.example.my_books_backend.service.FavoriteService;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final FavoriteMapper favoriteMapper;

    private final BookRepository bookRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<FavoriteResponse> getUserFavorites(
        User user,
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.FAVORITE_ALLOWED_FIELDS
        );
        Page<Favorite> pageObj = (bookId == null)
            ? favoriteRepository.findByUserAndIsDeletedFalse(user, pageable)
            : favoriteRepository.findByUserAndIsDeletedFalseAndBookId(user, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Favorite> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            favoriteRepository::findAllByIdInWithRelations,
            Favorite::getId
        );

        return favoriteMapper.toPageResponse(updatedPageObj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FavoriteStatsResponse getBookFavoriteStats(String bookId) {
        return favoriteRepository.getFavoriteStatsResponse(bookId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public FavoriteResponse createFavorite(FavoriteRequest request, User user) {
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new NotFoundException("Book not found"));

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndBook(user, book);

        Favorite favorite = new Favorite();
        if (existingFavorite.isPresent()) {
            favorite = existingFavorite.get();
            if (favorite.getIsDeleted()) {
                favorite.setIsDeleted(false);
            } else {
                throw new ConflictException("すでにこの書籍にはお気に入りが登録されています。");
            }
        }
        favorite.setUser(user);
        favorite.setBook(book);

        Favorite savedFavorite = favoriteRepository.save(favorite);
        return favoriteMapper.toFavoriteResponse(savedFavorite);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteFavorite(Long id, User user) {
        Favorite favorite = favoriteRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このお気に入りを削除する権限がありません");
        }

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteFavoriteByBookId(String bookId, User user) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new NotFoundException("book not found"));

        Favorite favorite = favoriteRepository.findByUserAndBook(user, book)
            .orElseThrow(() -> new NotFoundException("favorite not found"));

        favorite.setIsDeleted(true);
        favoriteRepository.save(favorite);
    }
}
