package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.bookmark.BookmarkRequest;
import com.example.my_books_api.dto.bookmark.BookmarkResponse;
import com.example.my_books_api.entity.BookChapter;
import com.example.my_books_api.entity.BookChapterId;
import com.example.my_books_api.entity.BookChapterPageContent;
import com.example.my_books_api.entity.Bookmark;
import com.example.my_books_api.entity.User;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.ForbiddenException;
import com.example.my_books_api.exception.UpgradeRequiredException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.BookmarkMapper;
import com.example.my_books_api.repository.BookChapterPageContentRepository;
import com.example.my_books_api.repository.BookChapterRepository;
import com.example.my_books_api.repository.BookmarkRepository;
import com.example.my_books_api.repository.UserRepository;
import com.example.my_books_api.service.BookmarkService;
import com.example.my_books_api.service.SubscriptionService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkMapper bookmarkMapper;

    private final BookChapterPageContentRepository bookChapterPageContentRepository;
    private final BookChapterRepository bookChapterRepository;
    private final UserRepository userRepository;
    private final JwtClaimExtractor jwtClaimExtractor;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('bookmark:manage:own')")
    public PageResponse<BookmarkResponse> getUserBookmarks(
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        String userId = jwtClaimExtractor.getUserId();

        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "ブックマーク機能の利用にはPREMIUMプランへのアップグレードが必要です"
            );
        }

        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOKMARK_ALLOWED_FIELDS
        );
        Page<Bookmark> pageObj = (bookId == null)
            ? bookmarkRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
            : bookmarkRepository.findByUserIdAndIsDeletedFalseAndPageContentBookId(userId, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Bookmark> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookmarkRepository::findAllByIdInWithRelations,
            Bookmark::getId
        );

        PageResponse<BookmarkResponse> response = bookmarkMapper.toPageResponse(updatedPageObj);

        // 章タイトルを動的に取得して追加
        addChapterTitles(response.getData());

        return response;
    }

    /**
     * ブックマークリストに章タイトルを動的に追加する
     * @param bookmarkResponses ブックマークレスポンスリスト
     */
    private void addChapterTitles(List<BookmarkResponse> bookmarkResponses) {
        if (bookmarkResponses == null || bookmarkResponses.isEmpty()) {
            return;
        }

        // 必要な(bookId, chapterNumber)ペアを収集
        Set<BookChapterId> bookChapterIds = bookmarkResponses.stream()
            .filter(response -> response.getChapterNumber() != null && response.getBook() != null)
            .map(response -> new BookChapterId(response.getBook().getId(), response.getChapterNumber()))
            .collect(Collectors.toSet());

        if (bookChapterIds.isEmpty()) {
            return;
        }

        // 必要な章情報をDBから一括取得
        List<BookChapter> bookChapters = bookChapterRepository.findByIdInAndIsDeletedFalse(bookChapterIds);

        // 取得したリストをMapに変換 (Key: BookChapterId, Value: Title)
        Map<BookChapterId, String> titleMap = bookChapters.stream()
            .collect(
                Collectors.toMap(
                    BookChapter::getId,
                    BookChapter::getTitle,
                    (existing, replacement) -> existing // 重複があった場合は既存を優先
                )
            );

        // 各ブックマークレスポンスにタイトルをマッピング
        bookmarkResponses.forEach(response -> {
            if (response.getChapterNumber() != null && response.getBook() != null) {
                BookChapterId targetId = new BookChapterId(
                    response.getBook().getId(),
                    response.getChapterNumber()
                );
                String title = titleMap.get(targetId);
                if (title != null) {
                    response.setChapterTitle(title);
                }
            }
        });
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('bookmark:manage:own')")
    public BookmarkResponse createBookmark(BookmarkRequest request) {
        String userId = jwtClaimExtractor.getUserId();

        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "ブックマーク機能の利用にはPREMIUMプランへのアップグレードが必要です"
            );
        }

        BookChapterPageContent pageContent = bookChapterPageContentRepository
            .findByBookIdAndChapterNumberAndPageNumber(
                request.getBookId(),
                request.getChapterNumber(),
                request.getPageNumber()
            )
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));

        Optional<Bookmark> existingBookmark = bookmarkRepository
            .findByUserIdAndPageContentBookIdAndPageContentChapterNumberAndPageContentPageNumber(
                userId,
                request.getBookId(),
                request.getChapterNumber(),
                request.getPageNumber()
            );

        Bookmark bookmark;
        if (existingBookmark.isPresent()) {
            bookmark = existingBookmark.get();
            if (bookmark.getIsDeleted()) {
                bookmark.setIsDeleted(false);
                bookmark.setCreatedAt(LocalDateTime.now());
            } else {
                throw new ConflictException("すでにこのページにはブックマークが登録されています。");
            }
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
            bookmark = new Bookmark();
            bookmark.setUser(user);
        }
        bookmark.setPageContent(pageContent);
        bookmark.setNote(request.getNote());

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('bookmark:manage:own')")
    public BookmarkResponse updateBookmark(@NonNull Long id, BookmarkRequest request) {
        Bookmark bookmark = bookmarkRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        String userId = jwtClaimExtractor.getUserId();
        if (!bookmark.getUser().getId().equals(userId)) {
            throw new ForbiddenException("編集する権限がありません");
        }
        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "ブックマーク機能の利用にはPREMIUMプランへのアップグレードが必要です"
            );
        }

        if (request.getNote() != null) {
            bookmark.setNote(request.getNote());
        }

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('bookmark:manage:own')")
    public void deleteBookmark(@NonNull Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        String userId = jwtClaimExtractor.getUserId();
        if (!bookmark.getUser().getId().equals(userId)) {
            throw new ForbiddenException("削除する権限がありません");
        }
        if (!subscriptionService.isPremium(userId)) {
            throw new UpgradeRequiredException(
                "ブックマーク機能の利用にはPREMIUMプランへのアップグレードが必要です"
            );
        }

        bookmark.setIsDeleted(true);
        bookmarkRepository.save(bookmark);
    }
}