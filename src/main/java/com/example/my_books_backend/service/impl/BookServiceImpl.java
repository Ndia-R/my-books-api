package com.example.my_books_backend.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book.BookDetailsResponse;
import com.example.my_books_backend.dto.book.BookResponse;
import com.example.my_books_backend.dto.book_chapter.BookChapterResponse;
import com.example.my_books_backend.dto.book_chapter.BookTableOfContentsResponse;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.exception.BadRequestException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookMapper;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.BookChapterPageContentRepository;
import com.example.my_books_backend.service.BookService;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    private final BookChapterPageContentRepository bookChapterPageContentRepository;

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<BookResponse> getBooksByGenre(
        String genreIdsQuery,
        String conditionQuery,
        Long page,
        Long size,
        String sortString
    ) {
        if (!("SINGLE".equals(conditionQuery)
            || "AND".equals(conditionQuery)
            || "OR".equals(conditionQuery))) {
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

        Boolean isAndCondition = "AND".equals(conditionQuery);

        Page<Book> pageObj = isAndCondition
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
     * {@inheritDoc}
     */
    @Override
    public BookDetailsResponse getBookDetails(String id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        return bookMapper.toBookDetailsResponse(book);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BookTableOfContentsResponse getBookTableOfContents(String id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        List<BookChapterResponse> chapterResponses = bookChapterPageContentRepository
            .findChapterResponsesByBookId(id);

        BookTableOfContentsResponse response = new BookTableOfContentsResponse();
        response.setBookId(id);
        response.setTitle(book.getTitle());
        response.setChapters(chapterResponses);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BookChapterPageContentResponse getBookChapterPageContent(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    ) {
        return bookChapterPageContentRepository
            .findChapterPageContentResponse(bookId, chapterNumber, pageNumber)
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));
    }

    // ----プライベートメソッド----

    private Long parseGenreId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid genre ID: " + id);
        }
    }
}
