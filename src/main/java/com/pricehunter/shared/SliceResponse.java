package com.pricehunter.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

/**
 * Лёгкая форма постраничного ответа без дорогого SQL COUNT для крупных каталогов.
 * Frontend получает текущую порцию и признак существования следующей страницы.
 */
@Schema(description = "Одна порция списка и информация о следующей странице")
public record SliceResponse<T>(
        @Schema(description = "Элементы текущей страницы") List<T> items,
        @Schema(description = "Номер страницы, начиная с нуля", example = "0") int page,
        @Schema(description = "Запрошенный размер страницы", example = "20") int size,
        @Schema(description = "Есть ли следующая страница") boolean hasNext
) {

    /** Преобразует Spring Slice в стабильный DTO-контракт. */
    public static <S, T> SliceResponse<T> from(Slice<S> source, Function<S, T> mapper) {
        return new SliceResponse<>(source.getContent().stream().map(mapper).toList(),
                source.getNumber(), source.getSize(), source.hasNext());
    }
}
