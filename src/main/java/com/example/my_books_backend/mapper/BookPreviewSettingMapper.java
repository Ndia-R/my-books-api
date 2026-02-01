package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.book_preview_setting.BookPreviewSettingResponse;
import com.example.my_books_backend.entity.BookPreviewSetting;
import com.example.my_books_backend.util.PageableUtils;

@Mapper(componentModel = "spring", uses = {BookMapper.class})
public interface BookPreviewSettingMapper {

    BookPreviewSettingResponse toBookPreviewSettingResponse(BookPreviewSetting setting);

    List<BookPreviewSettingResponse> toBookPreviewSettingResponseList(List<BookPreviewSetting> settings);

    default PageResponse<BookPreviewSettingResponse> toPageResponse(Page<BookPreviewSetting> settings) {
        List<BookPreviewSettingResponse> responses = toBookPreviewSettingResponseList(settings.getContent());
        return PageableUtils.toPageResponse(settings, responses);
    }
}
