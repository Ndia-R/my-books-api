package com.example.my_books_backend.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.BookChapterId;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookmarkMapper;
import com.example.my_books_backend.repository.BookChapterPageContentRepository;
import com.example.my_books_backend.repository.BookChapterRepository;
import com.example.my_books_backend.repository.BookmarkRepository;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkServiceImpl implements BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkMapper bookmarkMapper;

    private final BookChapterPageContentRepository bookChapterPageContentRepository;
    private final BookChapterRepository bookChapterRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<BookmarkResponse> getUserBookmarks(
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
            PageableUtils.BOOKMARK_ALLOWED_FIELDS
        );
        Page<Bookmark> pageObj = (bookId == null)
            ? bookmarkRepository.findByUserAndIsDeletedFalse(user, pageable)
            : bookmarkRepository.findByUserAndIsDeletedFalseAndPageContent_BookId(user, bookId, pageable);

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
     * 
     * @param bookmarkResponses ブックマークレスポンスリスト
     */
    private void addChapterTitles(List<BookmarkResponse> bookmarkResponses) {
        if (bookmarkResponses.isEmpty()) {
            return;
        }

        // 必要な(bookId, chapterNumber)ペアを収集
        Set<BookChapterId> bookChapterIds = bookmarkResponses.stream()
            .filter(response -> response.getChapterNumber() != null)
            .map(response -> new BookChapterId(response.getBook().getId(), response.getChapterNumber()))
            .collect(Collectors.toSet());

        if (bookChapterIds.isEmpty()) {
            return;
        }

        // 必要な章のみを取得
        List<BookChapter> bookChapters = bookChapterRepository.findByIdInAndIsDeletedFalse(bookChapterIds);

        // 各ブックマークに対応する章タイトルを設定
        bookmarkResponses.forEach(response -> {
            if (response.getChapterNumber() != null) {
                BookChapterId targetId = new BookChapterId(response.getBook().getId(), response.getChapterNumber());
                bookChapters.stream()
                    .filter(chapter -> chapter.getId().equals(targetId))
                    .findFirst()
                    .ifPresent(chapter -> response.setChapterTitle(chapter.getTitle()));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookmarkResponse createBookmark(BookmarkRequest request, User user) {
        BookChapterPageContent pageContent = bookChapterPageContentRepository
            .findByBookIdAndChapterNumberAndPageNumber(
                request.getBookId(),
                request.getChapterNumber(),
                request.getPageNumber()
            )
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndPageContent(user, pageContent);

        Bookmark bookmark = new Bookmark();
        if (existingBookmark.isPresent()) {
            bookmark = existingBookmark.get();
            if (bookmark.getIsDeleted()) {
                bookmark.setIsDeleted(false);
            } else {
                throw new ConflictException("すでにこのページにはブックマークが登録されています。");
            }
        }
        bookmark.setUser(user);
        bookmark.setPageContent(pageContent);
        bookmark.setNote(request.getNote());

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookmarkResponse updateBookmark(Long id, BookmarkRequest request, User user) {
        Bookmark bookmark = bookmarkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このブックマークを編集する権限がありません。");
        }

        String note = request.getNote();

        if (note != null) {
            bookmark.setNote(note);
        }

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteBookmark(Long id, User user) {
        Bookmark bookmark = bookmarkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このブックマークを削除する権限がありません");
        }

        bookmark.setIsDeleted(true);
        bookmarkRepository.save(bookmark);
    }
}
