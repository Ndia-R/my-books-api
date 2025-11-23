package com.example.my_books_backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.my_books_backend.dto.PageResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageableUtilsTest {

    @Test
    void testOf_正常系_基本的なページネーション() {
        // Given
        long page = 1;
        long size = 20;
        String sortString = "title.desc";
        List<String> allowedFields = PageableUtils.BOOK_ALLOWED_FIELDS;

        // When
        Pageable pageable = PageableUtils.of(page, size, sortString, allowedFields);

        // Then
        assertEquals(0, pageable.getPageNumber(), "1ベースが0ベースに変換される");
        assertEquals(20, pageable.getPageSize());
        assertEquals("title: DESC,id: ASC", pageable.getSort().toString());
    }

    @Test
    void testOf_ページ番号が0以下の場合() {
        // Given
        long page = 0;
        long size = 20;

        // When
        Pageable pageable = PageableUtils.of(page, size, null, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals(0, pageable.getPageNumber(), "0以下は0に変換される");
    }

    @Test
    void testOf_サイズが0以下の場合() {
        // Given
        long page = 1;
        long size = 0;

        // When
        Pageable pageable = PageableUtils.of(page, size, null, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals(20, pageable.getPageSize(), "デフォルトサイズ20が適用される");
    }

    @Test
    void testOf_サイズが最大値を超える場合() {
        // Given
        long page = 1;
        long size = 2000;

        // When
        Pageable pageable = PageableUtils.of(page, size, null, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals(1000, pageable.getPageSize(), "最大サイズ1000が適用される");
    }

    @Test
    void testOf_ソート条件がnullの場合() {
        // Given
        long page = 1;
        long size = 20;

        // When
        Pageable pageable = PageableUtils.of(page, size, null, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("id: ASC", pageable.getSort().toString(), "デフォルトソート（id.asc）が適用される");
    }

    @Test
    void testOf_ソート条件が空文字列の場合() {
        // Given
        long page = 1;
        long size = 20;

        // When
        Pageable pageable = PageableUtils.of(page, size, "", PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("id: ASC", pageable.getSort().toString(), "デフォルトソート（id.asc）が適用される");
    }

    @Test
    void testOf_不正なソート条件の場合() {
        // Given
        long page = 1;
        long size = 20;
        String invalidSort = "invalid";

        // When
        Pageable pageable = PageableUtils.of(page, size, invalidSort, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("id: ASC", pageable.getSort().toString(), "デフォルトソート（id.asc）が適用される");
    }

    @Test
    void testOf_許可されていないフィールドでのソート() {
        // Given
        long page = 1;
        long size = 20;
        String sortString = "unauthorized.desc";

        // When
        Pageable pageable = PageableUtils.of(page, size, sortString, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("id: DESC,id: ASC", pageable.getSort().toString(), "許可されていないフィールドはidにフォールバック");
    }

    @Test
    void testOf_昇順ソート() {
        // Given
        long page = 1;
        long size = 20;
        String sortString = "title.asc";

        // When
        Pageable pageable = PageableUtils.of(page, size, sortString, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("title: ASC,id: ASC", pageable.getSort().toString());
    }

    @Test
    void testOf_降順ソート() {
        // Given
        long page = 1;
        long size = 20;
        String sortString = "popularity.desc";

        // When
        Pageable pageable = PageableUtils.of(page, size, sortString, PageableUtils.BOOK_ALLOWED_FIELDS);

        // Then
        assertEquals("popularity: DESC,id: ASC", pageable.getSort().toString());
    }

    @Test
    @SuppressWarnings("null")
    void testToPageResponse_正常系() {
        // Given
        List<String> content = Arrays.asList("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(content, Pageable.ofSize(20), 100);
        List<String> responseList = Arrays.asList("response1", "response2", "response3");

        // When
        PageResponse<String> pageResponse = PageableUtils.toPageResponse(page, responseList);

        // Then
        assertEquals(1L, pageResponse.getCurrentPage(), "0ベースが1ベースに変換される");
        assertEquals(20L, pageResponse.getPageSize());
        assertEquals(5L, pageResponse.getTotalPages());
        assertEquals(100L, pageResponse.getTotalItems());
        assertTrue(pageResponse.getHasNext());
        assertFalse(pageResponse.getHasPrevious());
        assertEquals(3, pageResponse.getData().size());
    }

    @Test
    void testToPageResponse_空のページ() {
        // Given
        List<String> emptyContent = new ArrayList<>();
        Page<String> emptyPage = new PageImpl<>(emptyContent, Pageable.ofSize(20), 0);
        List<String> emptyResponseList = new ArrayList<>();

        // When
        PageResponse<String> pageResponse = PageableUtils.toPageResponse(emptyPage, emptyResponseList);

        // Then
        assertEquals(1L, pageResponse.getCurrentPage());
        assertEquals(20L, pageResponse.getPageSize());
        assertEquals(0L, pageResponse.getTotalPages());
        assertEquals(0L, pageResponse.getTotalItems());
        assertFalse(pageResponse.getHasNext());
        assertFalse(pageResponse.getHasPrevious());
        assertEquals(0, pageResponse.getData().size());
    }

    @Test
    @SuppressWarnings("null")
    void testApplyTwoQueryStrategy_正常系() {
        // Given
        List<TestEntity> initialContent = Arrays.asList(
            new TestEntity(3L, "C"),
            new TestEntity(1L, "A"),
            new TestEntity(2L, "B")
        );
        Page<TestEntity> initialPage = new PageImpl<>(initialContent, Pageable.ofSize(3), 3);

        List<TestEntity> detailedEntities = Arrays.asList(
            new TestEntity(1L, "A-detailed"),
            new TestEntity(2L, "B-detailed"),
            new TestEntity(3L, "C-detailed")
        );

        // When
        Page<TestEntity> result = PageableUtils.applyTwoQueryStrategy(
            initialPage,
            ids -> detailedEntities,
            TestEntity::getId
        );

        // Then
        assertEquals(3, result.getContent().size());
        assertEquals(3L, result.getContent().get(0).getId(), "元の順序（3,1,2）が保持される");
        assertEquals(1L, result.getContent().get(1).getId());
        assertEquals(2L, result.getContent().get(2).getId());
        assertEquals("C-detailed", result.getContent().get(0).getName());
    }

    @Test
    void testApplyTwoQueryStrategy_空のページ() {
        // Given
        List<TestEntity> emptyContent = new ArrayList<>();
        Page<TestEntity> emptyPage = new PageImpl<>(emptyContent, Pageable.ofSize(20), 0);

        // When
        Page<TestEntity> result = PageableUtils.applyTwoQueryStrategy(
            emptyPage,
            ids -> new ArrayList<>(),
            TestEntity::getId
        );

        // Then
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testApplyTwoQueryStrategy_引数がnullの場合() {
        // Given
        Page<TestEntity> page = new PageImpl<>(new ArrayList<>());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            PageableUtils.applyTwoQueryStrategy(null, ids -> new ArrayList<>(), TestEntity::getId);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PageableUtils.applyTwoQueryStrategy(page, null, TestEntity::getId);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PageableUtils.applyTwoQueryStrategy(page, ids -> new ArrayList<>(), null);
        });
    }

    @Test
    void testAllowedFieldsLists() {
        // Book allowed fields
        assertTrue(PageableUtils.BOOK_ALLOWED_FIELDS.contains("title"));
        assertTrue(PageableUtils.BOOK_ALLOWED_FIELDS.contains("publicationDate"));
        assertTrue(PageableUtils.BOOK_ALLOWED_FIELDS.contains("reviewCount"));
        assertTrue(PageableUtils.BOOK_ALLOWED_FIELDS.contains("averageRating"));
        assertTrue(PageableUtils.BOOK_ALLOWED_FIELDS.contains("popularity"));

        // Review allowed fields
        assertTrue(PageableUtils.REVIEW_ALLOWED_FIELDS.contains("updatedAt"));
        assertTrue(PageableUtils.REVIEW_ALLOWED_FIELDS.contains("createdAt"));
        assertTrue(PageableUtils.REVIEW_ALLOWED_FIELDS.contains("rating"));

        // Favorite allowed fields
        assertTrue(PageableUtils.FAVORITE_ALLOWED_FIELDS.contains("updatedAt"));
        assertTrue(PageableUtils.FAVORITE_ALLOWED_FIELDS.contains("createdAt"));

        // Bookmark allowed fields
        assertTrue(PageableUtils.BOOKMARK_ALLOWED_FIELDS.contains("updatedAt"));
        assertTrue(PageableUtils.BOOKMARK_ALLOWED_FIELDS.contains("createdAt"));
    }

    // テスト用のシンプルなエンティティクラス
    private static class TestEntity {
        private Long id;
        private String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
