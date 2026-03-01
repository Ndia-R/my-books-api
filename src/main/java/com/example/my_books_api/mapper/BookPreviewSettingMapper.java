package com.example.my_books_api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import com.example.my_books_api.dto.PageResponse;
import com.example.my_books_api.dto.book_preview_setting.BookPreviewSettingResponse;
import com.example.my_books_api.entity.BookPreviewSetting;
import com.example.my_books_api.util.PageableUtils;

@Mapper(componentModel = "spring", uses = { BookMapper.class })
public interface BookPreviewSettingMapper {

    @Mapping(target = "actualMaxChapter", ignore = true)
    @Mapping(target = "chapters", ignore = true)
    BookPreviewSettingResponse toBookPreviewSettingResponse(BookPreviewSetting setting);

    List<BookPreviewSettingResponse> toBookPreviewSettingResponseList(List<BookPreviewSetting> settings);

    default PageResponse<BookPreviewSettingResponse> toPageResponse(Page<BookPreviewSetting> settings) {
        List<BookPreviewSettingResponse> responses = toBookPreviewSettingResponseList(settings.getContent());
        return PageableUtils.toPageResponse(settings, responses);
    }
}
