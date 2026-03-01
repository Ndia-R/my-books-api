package com.example.my_books_api.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book.BookDetailsResponse;
import com.example.my_books_api.dto.book.BookRequest;
import com.example.my_books_api.dto.book.BookResponse;
import com.example.my_books_api.dto.book_chapter.BookChapterResponse;
import com.example.my_books_api.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_api.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingPublicResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookPreviewSetting;
import com.example.my_books_api.exception.BadRequestException;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.ForbiddenException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.exception.UpgradeRequiredException;
import com.example.my_books_api.mapper.BookMapper;
import com.example.my_books_api.entity.Genre;
import com.example.my_books_api.repository.BookRepository;
import com.example.my_books_api.repository.BookmarkRepository;
import com.example.my_books_api.repository.FavoriteRepository;
import com.example.my_books_api.repository.BookChapterPageContentRepository;
import com.example.my_books_api.repository.BookChapterRepository;
import com.example.my_books_api.repository.BookPreviewSettingRepository;
import com.example.my_books_api.repository.GenreRepository;
import com.example.my_books_api.repository.ReviewRepository;
import com.example.my_books_api.service.BookService;
import com.example.my_books_api.service.SubscriptionService;
import com.example.my_books_api.util.JwtClaimExtractor;
import com.example.my_books_api.util.PageableUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final ReviewRepository reviewRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookChapterRepository bookChapterRepository;
    private final BookChapterPageContentRepository bookChapterPageContentRepository;
    private final BookPreviewSettingRepository bookPreviewSettingRepository;

    private final BookMapper bookMapper;
    private final SubscriptionService subscriptionService;
    private final JwtClaimExtractor jwtClaimExtractor;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<BookResponse> getBooks(
        Long page,
        Long size,
        String sortString
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOK_ALLOWED_FIELDS
        );
        Page<Book> pageObj = bookRepository.findByIsDeletedFalse(pageable);

        // 2クエリ戦略を適用
        Page<Book> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookRepository::findAllByIdInWithRelations,
            Book::getId
        );

        return bookMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<BookResponse> getBooksByTitleKeyword(
        String keyword,
        Long page,
        Long size,
        String sortString
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOK_ALLOWED_FIELDS
        );
        Page<Book> pageObj = bookRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);

        // 2クエリ戦略を適用
        Page<Book> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookRepository::findAllByIdInWithRelations,
            Book::getId
        );

        return bookMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<BookResponse> getBooksByGenre(
        String genreIdsQuery,
        String conditionQuery,
        Long page,
        Long size,
        String sortString
    ) {
        if (!Set.of("SINGLE", "AND", "OR").contains(conditionQuery)) {
            throw new BadRequestException("検索条件が不正です。");
        }

        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOK_ALLOWED_FIELDS
        );

        List<Long> genreIds = Arrays.stream(genreIdsQuery.split(","))
            .map(String::trim)
            .map(this::parseGenreId)
            .collect(Collectors.toList());

        Page<Book> pageObj = "AND".equals(conditionQuery)
            ? bookRepository.findBooksHavingAllGenres(genreIds, (long) genreIds.size(), pageable)
            : bookRepository.findDistinctByGenres_IdInAndIsDeletedFalse(genreIds, pageable);

        // 2クエリ戦略を適用
        Page<Book> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookRepository::findAllByIdInWithRelations,
            Book::getId
        );

        return bookMapper.toPageResponse(updatedPageObj);
    }

    /**
     * 文字列のジャンルIDをLongに変換する
     * @param id
     * @return
     */
    private Long parseGenreId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid genre ID: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public BookDetailsResponse getBookDetails(@NonNull String id) {
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        return bookMapper.toBookDetailsResponse(book);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public BookDetailsResponse createBook(@Valid BookRequest request) {
        String bookId = request.getId();
        if (bookId == null) {
            throw new BadRequestException("書籍IDは必須です");
        }

        // 削除済みも含めて既存のIDをチェック
        Optional<Book> existingBook = bookRepository.findById(bookId);

        Book book;
        if (existingBook.isPresent()) {
            book = existingBook.get();
            if (book.getIsDeleted()) {
                book.setIsDeleted(false);
                book.setCreatedAt(LocalDateTime.now());
            } else {
                throw new ConflictException("書籍ID '" + bookId + "' は既に存在します");
            }
        } else {
            book = new Book();
            book.setId(bookId);
            book.setReviewCount(0L);
            book.setAverageRating(0.0);
            book.setPopularity(0.0);
        }

        // プロパティの反映（ジャンルチェック含む）
        updateBookProperties(book, request);

        Book savedBook = bookRepository.save(book);
        return bookMapper.toBookDetailsResponse(savedBook);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public BookDetailsResponse updateBook(String id, BookRequest request) {
        // 削除されていない本のみ更新可能
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("書籍ID '" + id + "' が見つかりません"));

        updateBookProperties(book, request);

        Book updatedBook = bookRepository.save(java.util.Objects.requireNonNull(book));
        return bookMapper.toBookDetailsResponse(updatedBook);
    }

    /**
     * リクエストからエンティティへ値を詰め替える（共通処理）
     * @param book 書籍エンティティ
     * @param request 書籍リクエスト
     */
    private void updateBookProperties(Book book, BookRequest request) {
        List<Long> genreIds = request.getGenreIds();
        if (genreIds == null) {
            genreIds = new ArrayList<>();
        }
        List<Genre> genres = genreRepository.findAllById(genreIds);
        if (genres.size() != genreIds.size()) {
            throw new BadRequestException("指定されたジャンルIDの一部が存在しません");
        }

        book.setTitle(request.getTitle());
        book.setDescription(request.getDescription());
        book.setAuthors(request.getAuthors());
        book.setPublisher(request.getPublisher());
        book.setPublicationDate(request.getPublicationDate());
        book.setPrice(request.getPrice());
        book.setPageCount(request.getPageCount());
        book.setIsbn(request.getIsbn());
        book.setImagePath(request.getImagePath());
        book.setGenres(genres);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public void deleteBook(String id) {
        // 書籍の存在確認
        Book book = bookRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        // 書籍自体を論理削除
        book.setIsDeleted(true);

        // コンテンツ（章・ページ）の連鎖削除
        bookChapterRepository.softDeleteAllByBookId(id);
        bookChapterPageContentRepository.softDeleteAllByBookId(id);

        // ユーザーデータの連鎖削除
        reviewRepository.softDeleteAllByBookId(id);
        favoriteRepository.softDeleteAllByBookId(id);
        bookmarkRepository.softDeleteAllByBookId(id);
    }

    // --- 他のメソッド (目次・ページ取得等) ---

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public BookTableOfContentsResponse getBookTableOfContents(@NonNull String id) {
        // 書籍が有効かチェック
        if (!bookRepository.existsByIdAndIsDeletedFalse(id)) {
            throw new NotFoundException("Book not found");
        }
        List<BookChapterResponse> chapterResponses = bookChapterPageContentRepository.findChapterResponsesByBookId(id);

        Book book = bookRepository.findById(id).get();
        BookTableOfContentsResponse response = new BookTableOfContentsResponse();
        response.setBookId(id);
        response.setTitle(book.getTitle());
        response.setChapters(chapterResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public BookChapterPageContentResponse getBookChapterPagePreview(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    ) {
        // 試し読み設定に基づいて閲覧可否を判定
        Optional<Boolean> previewAllowedResult = bookPreviewSettingRepository.isPreviewAllowed(
            bookId,
            chapterNumber,
            pageNumber
        );

        if (previewAllowedResult.isEmpty()) {
            // 設定がない場合: デフォルト設定（第1章全体）を適用
            if (chapterNumber != 1) {
                throw new ForbiddenException("閲覧する権限がありません");
            }
            // 第1章は全ページOK（何もしない）
        } else if (!previewAllowedResult.get()) {
            // 設定があるが判定がfalse
            throw new ForbiddenException("閲覧する権限がありません");
        }

        return bookChapterPageContentRepository
            .findChapterPageContentResponse(bookId, chapterNumber, pageNumber)
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('book-content:read:all')")
    public BookChapterPageContentResponse getBookChapterPageContent(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    ) {
        String userId = jwtClaimExtractor.getUserId();

        // PREMIUMユーザーは全コンテンツにアクセス可能
        if (subscriptionService.isPremium(userId)) {
            return bookChapterPageContentRepository
                .findChapterPageContentResponse(bookId, chapterNumber, pageNumber)
                .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));
        }

        // FREEユーザー: 試し読み範囲チェック
        Optional<Boolean> previewAllowedResult = bookPreviewSettingRepository.isPreviewAllowed(
            bookId,
            chapterNumber,
            pageNumber
        );

        if (previewAllowedResult.isEmpty()) {
            // 設定がない場合: デフォルト設定（第1章全体）を適用
            if (chapterNumber != 1) {
                throw new UpgradeRequiredException(
                    "このコンテンツの閲覧にはPREMIUMプランへのアップグレードが必要です"
                );
            }
        } else if (!previewAllowedResult.get()) {
            throw new UpgradeRequiredException(
                "このコンテンツの閲覧にはPREMIUMプランへのアップグレードが必要です"
            );
        }

        return bookChapterPageContentRepository
            .findChapterPageContentResponse(bookId, chapterNumber, pageNumber)
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public BookPreviewSettingPublicResponse getPreviewSettingByBookId(@NonNull String bookId) {
        Optional<BookPreviewSetting> bookPreviewSetting = bookPreviewSettingRepository.findByBookIdAndIsDeletedFalse(
            bookId
        );

        // 存在しなければデフォルト設定を返す
        if (bookPreviewSetting.isEmpty()) {
            if (!bookRepository.existsById(bookId)) {
                throw new NotFoundException("Book not found");
            }
            return new BookPreviewSettingPublicResponse(bookId, 1L, -1L);
        }

        BookPreviewSetting setting = bookPreviewSetting.get();
        return new BookPreviewSettingPublicResponse(
            setting.getBook().getId(),
            setting.getMaxChapter(),
            setting.getMaxPage()
        );
    }
}
