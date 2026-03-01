package com.example.my_books_api.service.impl;

import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingRequest;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingResponse;
import com.example.my_books_api.entity.Book;
import com.example.my_books_api.entity.BookPreviewSetting;
import com.example.my_books_api.exception.BadRequestException;
import com.example.my_books_api.exception.ConflictException;
import com.example.my_books_api.exception.NotFoundException;
import com.example.my_books_api.mapper.BookPreviewSettingMapper;
import com.example.my_books_api.repository.BookChapterPageContentRepository;
import com.example.my_books_api.repository.BookChapterRepository;
import com.example.my_books_api.repository.BookPreviewSettingRepository;
import com.example.my_books_api.repository.BookRepository;
import com.example.my_books_api.service.BookPreviewSettingService;
import com.example.my_books_api.util.PageableUtils;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookPreviewSettingServiceImpl implements BookPreviewSettingService {

    private final BookPreviewSettingRepository bookPreviewSettingRepository;
    private final BookRepository bookRepository;
    private final BookChapterRepository bookChapterRepository;
    private final BookChapterPageContentRepository bookChapterPageContentRepository;

    private final BookPreviewSettingMapper bookPreviewSettingMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('book:manage:all')")
    public PageResponse<BookPreviewSettingResponse> getPreviewSettings(
        Long page,
        Long size,
        String sortString
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOK_PREVIEW_SETTING_ALLOWED_FIELDS
        );
        Page<BookPreviewSetting> pageObj = bookPreviewSettingRepository.findByIsDeletedFalse(pageable);

        // 2クエリ戦略を適用
        Page<BookPreviewSetting> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookPreviewSettingRepository::findAllByIdInWithRelations,
            BookPreviewSetting::getId
        );

        return bookPreviewSettingMapper.toPageResponse(updatedPageObj);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('book:manage:all')")
    public BookPreviewSettingResponse getPreviewSetting(@NonNull Long id) {
        BookPreviewSetting setting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));
        return toResponseWithMetadata(setting);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public BookPreviewSettingResponse createPreviewSetting(BookPreviewSettingRequest request) {
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new NotFoundException("Book not found"));

        Optional<BookPreviewSetting> existingSetting = bookPreviewSettingRepository.findByBookId(request.getBookId());

        BookPreviewSetting setting;
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            if (setting.getIsDeleted()) {
                setting.setIsDeleted(false);
                setting.setCreatedAt(LocalDateTime.now());
            } else {
                throw new ConflictException("すでにこの書籍にはプレビュー設定が登録されています。");
            }
        } else {
            setting = new BookPreviewSetting();
            setting.setBook(book);
        }

        // nullの場合はエンティティのデフォルト値を使用（新規作成時）または既存値を維持（復活時）
        if (request.getMaxChapter() != null) {
            setting.setMaxChapter(request.getMaxChapter());
        }
        if (request.getMaxPage() != null) {
            setting.setMaxPage(request.getMaxPage());
        }

        // 実際の書籍コンテンツとの整合性チェック
        validateChapterAndPage(request.getBookId(), setting.getMaxChapter(), setting.getMaxPage());

        BookPreviewSetting saved = bookPreviewSettingRepository.save(setting);
        return toResponseWithMetadata(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public BookPreviewSettingResponse updatePreviewSetting(@NonNull Long id, BookPreviewSettingRequest request) {
        BookPreviewSetting setting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        if (request.getMaxChapter() != null) {
            setting.setMaxChapter(request.getMaxChapter());
        }
        if (request.getMaxPage() != null) {
            setting.setMaxPage(request.getMaxPage());
        }

        // 実際の書籍コンテンツとの整合性チェック
        validateChapterAndPage(setting.getBook().getId(), setting.getMaxChapter(), setting.getMaxPage());

        BookPreviewSetting saved = bookPreviewSettingRepository.save(setting);
        return toResponseWithMetadata(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public void deletePreviewSetting(@NonNull Long id) {
        BookPreviewSetting previewSetting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        previewSetting.setIsDeleted(true);
        bookPreviewSettingRepository.save(previewSetting);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:all')")
    public void deletePreviewSettingByBookId(String bookId) {
        BookPreviewSetting previewSetting = bookPreviewSettingRepository.findByBookIdAndIsDeletedFalse(bookId)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        previewSetting.setIsDeleted(true);
        bookPreviewSettingRepository.save(previewSetting);
    }

    /**
     * 試し読み設定のmaxChapter・maxPageが実際の書籍コンテンツの範囲内かを検証する。
     * -1（無制限）の場合はバリデーションをスキップする。
     */
    private void validateChapterAndPage(String bookId, Long maxChapter, Long maxPage) {
        // maxChapterが-1（無制限）ならスキップ
        if (maxChapter != null && maxChapter != -1) {
            Long actualMaxChapter = bookChapterRepository.findMaxChapterNumber(bookId)
                .orElseThrow(() -> new BadRequestException("この書籍にはまだ章が登録されていません。"));
            if (maxChapter > actualMaxChapter) {
                throw new BadRequestException(
                    "maxChapterが実際の最大章番号（" + actualMaxChapter + "）を超えています。"
                );
            }
        }

        // maxPageが-1（無制限）またはmaxChapterが-1（無制限）ならスキップ
        if (maxPage != null && maxPage != -1 && maxChapter != null && maxChapter != -1) {
            Long actualMaxPage = bookChapterPageContentRepository
                .findMaxPageNumber(bookId, maxChapter)
                .orElseThrow(
                    () -> new BadRequestException(
                        "章番号 " + maxChapter + " にはまだページが登録されていません。"
                    )
                );
            if (maxPage > actualMaxPage) {
                throw new BadRequestException(
                    "maxPageが章 " + maxChapter + " の実際の最大ページ番号（" + actualMaxPage + "）を超えています。"
                );
            }
        }
    }

    /**
     * BookPreviewSettingResponseに書籍の章・ページメタデータを付与して返す。
     */
    private BookPreviewSettingResponse toResponseWithMetadata(BookPreviewSetting setting) {
        String bookId = setting.getBook().getId();
        BookPreviewSettingResponse response = bookPreviewSettingMapper.toBookPreviewSettingResponse(setting);
        response.setActualMaxChapter(
            bookChapterRepository.findMaxChapterNumber(bookId).orElse(0L)
        );
        response.setChapters(
            bookChapterPageContentRepository.findChapterResponsesByBookId(bookId)
        );
        return response;
    }
}
