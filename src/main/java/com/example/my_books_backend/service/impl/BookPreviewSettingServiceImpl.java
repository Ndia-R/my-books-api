package com.example.my_books_backend.service.impl;

import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book_preview_setting.BookPreviewSettingRequest;
import com.example.my_books_backend.dto.book_preview_setting.BookPreviewSettingResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.entity.BookPreviewSetting;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookPreviewSettingMapper;
import com.example.my_books_backend.repository.BookPreviewSettingRepository;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.service.BookPreviewSettingService;
import com.example.my_books_backend.util.PageableUtils;

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

    private final BookPreviewSettingMapper bookPreviewSettingMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('book:manage:any')")
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
    @PreAuthorize("hasAuthority('book:manage:any')")
    public BookPreviewSettingResponse getPreviewSetting(@NonNull Long id) {
        BookPreviewSetting setting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));
        return bookPreviewSettingMapper.toBookPreviewSettingResponse(setting);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public BookPreviewSettingResponse getPreviewSettingByBookId(@NonNull String bookId) {
        Optional<BookPreviewSetting> bookPreviewSetting = bookPreviewSettingRepository.findByBookIdAndIsDeletedFalse(
            bookId
        );

        // 存在しなければデフォルト設定を返す
        if (bookPreviewSetting.isEmpty()) {
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));
            BookPreviewSetting defaultSetting = new BookPreviewSetting();
            defaultSetting.setBook(book);
            return bookPreviewSettingMapper.toBookPreviewSettingResponse(defaultSetting);
        }

        return bookPreviewSettingMapper.toBookPreviewSettingResponse(bookPreviewSetting.get());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:any')")
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
        if (request.getUnlimitedChapter() != null) {
            setting.setUnlimitedChapter(request.getUnlimitedChapter());
        }
        if (request.getUnlimitedPage() != null) {
            setting.setUnlimitedPage(request.getUnlimitedPage());
        }

        // 開放フラグが設定されていれば全開放（-1）に更新
        if (setting.isUnlimitedChapter()) {
            setting.setMaxChapter(-1L);
        }
        if (setting.isUnlimitedPage()) {
            setting.setMaxPage(-1L);
        }

        BookPreviewSetting saved = bookPreviewSettingRepository.save(setting);
        return bookPreviewSettingMapper.toBookPreviewSettingResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:any')")
    public BookPreviewSettingResponse updatePreviewSetting(@NonNull Long id, BookPreviewSettingRequest request) {
        BookPreviewSetting setting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        if (request.getMaxChapter() != null) {
            setting.setMaxChapter(request.getMaxChapter());
        }
        if (request.getMaxPage() != null) {
            setting.setMaxPage(request.getMaxPage());
        }
        if (request.getUnlimitedChapter() != null) {
            setting.setUnlimitedChapter(request.getUnlimitedChapter());
        }
        if (request.getUnlimitedPage() != null) {
            setting.setUnlimitedPage(request.getUnlimitedPage());
        }

        // 開放フラグが設定されていれば全開放（-1）に更新
        if (setting.isUnlimitedChapter()) {
            setting.setMaxChapter(-1L);
        }
        if (setting.isUnlimitedPage()) {
            setting.setMaxPage(-1L);
        }

        BookPreviewSetting saved = bookPreviewSettingRepository.save(setting);
        return bookPreviewSettingMapper.toBookPreviewSettingResponse(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:any')")
    public void deletePreviewSetting(@NonNull Long id) {
        BookPreviewSetting previewSetting = bookPreviewSettingRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        previewSetting.setIsDeleted(true);
        bookPreviewSettingRepository.save(previewSetting);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('book:manage:any')")
    public void deletePreviewSettingByBookId(String bookId) {
        BookPreviewSetting previewSetting = bookPreviewSettingRepository.findByBookIdAndIsDeletedFalse(bookId)
            .orElseThrow(() -> new NotFoundException("BookPreviewSetting not found"));

        previewSetting.setIsDeleted(true);
        bookPreviewSettingRepository.save(previewSetting);
    }
}
