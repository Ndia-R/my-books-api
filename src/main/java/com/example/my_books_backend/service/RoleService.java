package com.example.my_books_backend.service;

import java.util.List;
import com.example.my_books_backend.dto.role.RoleRequest;
import com.example.my_books_backend.dto.role.RoleResponse;

public interface RoleService {
    /**
     * すべてのロールを取得
     * 
     * @return ロールリスト
     */
    List<RoleResponse> getAllRoles();

    /**
     * 指定されたロールを取得
     * 
     * @param id ロールID
     * @return ロール
     */
    RoleResponse getRoleById(Long id);

    /**
     * ロールを作成
     * 
     * @param request ロール作成リクエスト
     * @return 作成されたロール情報
     */
    RoleResponse createRole(RoleRequest request);

    /**
     * ロールを更新
     * 
     * @param id 更新するロールのID
     * @param request ロール更新リクエスト
     * @return 更新されたロール情報
     */
    RoleResponse updateRole(Long id, RoleRequest request);

    /**
     * ロールを削除
     * 
     * @param id 削除するロールのID
     */
    void deleteRole(Long id);
}
