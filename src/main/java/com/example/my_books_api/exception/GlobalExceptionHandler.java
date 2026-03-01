package com.example.my_books_api.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.lang.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.my_books_api.dto.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ NotFoundException.class })
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        log.error("リソースが見つかりません: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler({ BadRequestException.class })
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.error("不正なリクエスト: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_REQUEST",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler({ ValidationException.class })
    public ResponseEntity<Object> handleValidation(ValidationException ex, WebRequest request) {
        log.error("バリデーションエラー: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_FAILED",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler({ ConflictException.class })
    public ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        log.error("競合エラー: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "CONFLICT",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler({ UnauthorizedException.class })
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        log.error("認証エラー: {}", ex.getMessage(), ex);

        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(
            "UNAUTHORIZED",
            ex.getMessage(),
            HttpStatus.UNAUTHORIZED.value(),
            path
        );
        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.UNAUTHORIZED,
            request
        );
    }

    @ExceptionHandler({ UpgradeRequiredException.class })
    public ResponseEntity<Object> handleUpgradeRequired(UpgradeRequiredException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        log.warn("プランアップグレード必要: {} - パス: {}", ex.getMessage(), path);

        ErrorResponse errorResponse = new ErrorResponse(
            "UPGRADE_REQUIRED",
            ex.getMessage(),
            HttpStatus.FORBIDDEN.value(),
            path
        );
        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.FORBIDDEN,
            request
        );
    }

    /**
     * 認可エラーを統合的に処理
     * - ForbiddenException: アプリケーション独自の認可エラー
     * - AuthorizationDeniedException: Spring Security @PreAuthorize等のメソッドレベル認可
     * - AccessDeniedException: Spring Securityフィルターチェーンレベル認可
     */
    @ExceptionHandler({
        ForbiddenException.class,
        AuthorizationDeniedException.class,
        AccessDeniedException.class
    })
    public ResponseEntity<Object> handleForbidden(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        String message;

        // カスタム例外の場合は詳細メッセージを使用、Spring Securityの場合は統一メッセージ
        if (ex instanceof ForbiddenException) {
            log.error("認可エラー: {} - パス: {}", ex.getMessage(), path);
            message = ex.getMessage();
        } else {
            // Spring Securityの認可拒否は正常な動作なのでWARNレベル
            log.warn("認可拒否: {} - パス: {}", ex.getClass().getSimpleName(), path);
            message = "このリソースへのアクセス権限がありません";
        }

        ErrorResponse errorResponse = new ErrorResponse(
            "FORBIDDEN",
            message,
            HttpStatus.FORBIDDEN.value(),
            path
        );
        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.FORBIDDEN,
            request
        );
    }

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<Object> handleGeneral(Exception ex, WebRequest request) {
        log.error("予期しないエラー: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "内部サーバーエラーが発生しました",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        log.error("バリデーションエラー: {}", ex.getMessage(), ex);

        // 複数のフィールドエラーをまとめる
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> errorMessages = new ArrayList<>();
        for (final FieldError error : fieldErrors) {
            errorMessages.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_FAILED",
            String.join(",", errorMessages),
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", "")
        );

        return this.handleExceptionInternal(
            ex,
            errorResponse,
            headers,
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        @NonNull Exception ex,
        @Nullable Object body,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode statusCode,
        @NonNull WebRequest request
    ) {
        log.error("内部例外: {} - {}", statusCode, ex.getMessage(), ex);

        // bodyがnullまたはErrorResponse以外の場合、統一されたErrorResponseを作成
        if (!(body instanceof ErrorResponse)) {
            String errorCode = "HTTP_" + statusCode.value();
            String message = getDefaultMessageForStatus(statusCode);
            String path = request.getDescription(false).replace("uri=", "");

            body = new ErrorResponse(errorCode, message, statusCode.value(), path);
        }

        return ResponseEntity.status(statusCode).headers(headers).body(body);
    }

    private String getDefaultMessageForStatus(HttpStatusCode status) {
        return switch (status.value()) {
        case 400 -> "不正なリクエストです";
        case 401 -> "認証が必要です";
        case 403 -> "アクセスが拒否されました";
        case 404 -> "リソースが見つかりません";
        case 405 -> "許可されていないメソッドです";
        case 500 -> "内部サーバーエラーが発生しました";
        default -> "エラーが発生しました";
        };
    }
}
