package com.example.my_books_backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import com.example.my_books_backend.dto.PageResponse;
import java.util.function.Function;

public class PageableUtils {
    // application.propertiesの値と一致させる
    private static final long DEFAULT_PAGE_SIZE = 20;
    private static final long MAX_PAGE_SIZE = 1000;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.ASC;

    // ソート可能なフィールドのリスト（エンドポイントで指定可能なフィールド）
    public static final List<String> BOOK_ALLOWED_FIELDS = new ArrayList<>(
        List.of("title", "publicationDate", "reviewCount", "averageRating", "popularity")
    );
    public static final List<String> REVIEW_ALLOWED_FIELDS = new ArrayList<>(
        List.of("updatedAt", "createdAt", "rating")
    );
    public static final List<String> FAVORITE_ALLOWED_FIELDS = new ArrayList<>(
        List.of("updatedAt", "createdAt")
    );
    public static final List<String> BOOKMARK_ALLOWED_FIELDS = new ArrayList<>(
        List.of("updatedAt", "createdAt")
    );

    /**
     * ページネーション用のPageableオブジェクトを作成
     * Spring Data JPAの慣習に合わせてメソッド名は「of」
     * 
     * @param page ページ番号（1ベース）
     * @param size 1ページあたりの最大結果件数
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param category ソート可能なフィールドのリスト
     * @return Pageableオブジェクト 
     */
    public static Pageable of(
        long page,
        long size,
        String sortString,
        List<String> category
    ) {
        page = Math.max(0, page - 1); // pageableは内部的に0ベースなので、1ベース→0ベースへ
        size = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Sort sort = parseSort(sortString, category);

        return PageRequest.of((int) page, (int) size, sort);
    }

    /**
     * ソート条件の解析
     * 
     * @param sortString ソート条件（例: "xxxx.desc", "xxxx.asc"）
     * @param category ソート可能なフィールドのリスト
     * @return Sortオブジェクト
     */
    private static Sort parseSort(String sortString, List<String> category) {
        if (sortString == null || sortString.trim().isEmpty()) {
            return Sort.by(DEFAULT_SORT_DIRECTION, DEFAULT_SORT_FIELD);
        }

        String[] sortParams = sortString.trim().split("\\.");
        if (sortParams.length != 2) {
            return Sort.by(DEFAULT_SORT_DIRECTION, DEFAULT_SORT_FIELD);
        }

        String sortField = sortParams[0].trim();
        if (!category.contains(sortField)) {
            sortField = DEFAULT_SORT_FIELD;
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(sortParams[1].trim());
        } catch (IllegalArgumentException e) {
            sortDirection = DEFAULT_SORT_DIRECTION;
        }

        // 第二ソートは「id」とする
        return Sort.by(sortDirection, sortField).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * 汎用的なPageResponse変換メソッド
     * 
     * @param <T> エンティティの型
     * @param <R> レスポンスの型
     * @param page Pageオブジェクト
     * @param responseList 変換済みのレスポンスリスト
     * @return PageResponseオブジェクト
     */
    public static <T, R> PageResponse<R> toPageResponse(Page<T> page, List<R> responseList) {
        return new PageResponse<R>(
            (long) page.getNumber() + 1, // Pageableの内部的にはデフォルトで0ベースだが、エンドポイントとしては1ベースなので+1する
            (long) page.getSize(),
            (long) page.getTotalPages(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious(),
            responseList
        );
    }

    /**
     * 2クエリ戦略でソート順序を保持するためのユーティリティメソッド
     * IDリストの順序に従ってリストを並び替える
     * 
     * @param <T> エンティティの型
     * @param <ID> IDの型
     * @param ids 元のIDリスト（正しい順序）
     * @param list 並び替える対象のリスト
     * @param idExtractor IDを抽出する関数
     * @return ソート順序が復元されたリスト
     */
    private static <T, ID> List<T> restoreSortOrder(
        List<ID> ids,
        List<T> list,
        Function<T, ID> idExtractor
    ) {
        if (ids.isEmpty() || list.isEmpty()) {
            return List.of();
        }

        // ソート順序を保持するためのマップを作成（パフォーマンス最適化）
        Map<ID, Integer> idOrder = IntStream.range(0, ids.size())
            .boxed()
            .collect(Collectors.toMap(ids::get, i -> i));

        // 元のソート順序でリストを並び替え
        return list.stream()
            .sorted((item1, item2) -> {
                Integer order1 = idOrder.get(idExtractor.apply(item1));
                Integer order2 = idOrder.get(idExtractor.apply(item2));
                
                // null安全性の確保
                if (order1 == null && order2 == null) return 0;
                if (order1 == null) return 1;
                if (order2 == null) return -1;
                
                return order1.compareTo(order2);
            })
            .collect(Collectors.toList());
    }

    /**
     * 2クエリ戦略でのページネーション処理を統一化するユーティリティメソッド
     * 
     * @param <T> エンティティの型
     * @param <ID> IDの型
     * @param initialPage 初回クエリの結果ページ
     * @param repositoryFinder リポジトリからIDリストで詳細データを取得する関数
     * @param idExtractor エンティティからIDを抽出する関数
     * @return ソート順序が保持された新しいPageオブジェクト
     * @throws IllegalArgumentException 引数がnullの場合
     */
    public static <T, ID> Page<T> applyTwoQueryStrategy(
        Page<T> initialPage,
        Function<List<ID>, List<T>> repositoryFinder,
        Function<T, ID> idExtractor
    ) {
        if (initialPage == null || repositoryFinder == null || idExtractor == null) {
            throw new IllegalArgumentException("引数にnullは指定できません");
        }

        // 空のページの場合は早期リターン
        if (initialPage.getContent().isEmpty()) {
            return new PageImpl<>(
                List.of(),
                initialPage.getPageable(),
                initialPage.getTotalElements()
            );
        }

        // IDリストを取得
        List<ID> ids = initialPage.getContent().stream()
            .map(idExtractor)
            .collect(Collectors.toList());

        // 詳細データを取得
        List<T> detailedList = repositoryFinder.apply(ids);

        // ソート順序を復元
        List<T> sortedList = restoreSortOrder(ids, detailedList, idExtractor);

        // 新しいPageオブジェクトを作成
        return new PageImpl<>(
            sortedList,
            initialPage.getPageable(),
            initialPage.getTotalElements()
        );
    }
}
