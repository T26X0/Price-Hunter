package com.pricehunter.product;

import com.pricehunter.shared.NotFoundException;
import com.pricehunter.shared.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only фасад публичного каталога.
 * Собирает DTO из узких проекций и не раскрывает JPA-сущности или внутренние связи frontend.
 */
@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantAttributeRepository attributeRepository;

    /** Ищет модели по названию, ключу, бренду, категории и необязательному городу. */
    @Transactional(readOnly = true)
    public SliceResponse<ProductListResponse> findProducts(String query, UUID cityId, String category, String brand,
                                                           int page, int size) {
        Pageable pageable = page(page, size);
        return SliceResponse.from(productRepository.searchCatalog(normalize(query), cityId, normalize(category),
                normalize(brand), pageable), ProductListResponse::from);
    }

    /** Возвращает карточку по единственному публичному идентификатору — catalogKey. */
    @Transactional(readOnly = true)
    public ProductDetailResponse findProduct(String catalogKey, UUID cityId) {
        Product product = productRepository.findBySkuIgnoreCase(requiredCatalogKey(catalogKey))
                .orElseThrow(() -> new NotFoundException("Product '" + catalogKey + "' was not found"));
        ProductDetailStatsProjection stats = productRepository.findCatalogStats(product.getId(), cityId);
        List<ProductVariantResponse> variants = findVariantsForProduct(product.getId(), cityId);
        return new ProductDetailResponse(product.getId(), product.getSku(), product.getBrand(), product.getName(),
                product.getCategoryCode(), product.getDescription(), stats.getMinEffectivePrice(), stats.getCurrency(),
                stats.getOfferCount(), variants);
    }

    /** Возвращает варианты указанной модели с фильтрацией цен и наличия по необязательному городу. */
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> findVariants(String catalogKey, UUID cityId) {
        Product product = productRepository.findBySkuIgnoreCase(requiredCatalogKey(catalogKey))
                .orElseThrow(() -> new NotFoundException("Product '" + catalogKey + "' was not found"));
        return findVariantsForProduct(product.getId(), cityId);
    }

    /** Выполняет два пакетных запроса: агрегаты вариантов и все их характеристики. */
    private List<ProductVariantResponse> findVariantsForProduct(UUID productId, UUID cityId) {
        Map<UUID, List<VariantAttributeResponse>> attributesByVariant = attributeRepository
                .findCatalogAttributes(productId)
                .stream()
                .collect(Collectors.groupingBy(VariantAttributeProjection::getVariantId,
                        Collectors.mapping(VariantAttributeResponse::from, Collectors.toList())));
        return variantRepository.findCatalogVariants(productId, cityId).stream()
                .map(variant -> ProductVariantResponse.from(variant,
                        attributesByVariant.getOrDefault(variant.getId(), List.of())))
                .toList();
    }

    /** Ограничивает пагинацию, чтобы один запрос не мог прочитать весь каталог. */
    private static Pageable page(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    /** Преобразует пустой необязательный фильтр в null для компактного SQL. */
    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Не позволяет передать пустой идентификатор товара в запрос к базе. */
    private static String requiredCatalogKey(String catalogKey) {
        String normalized = normalize(catalogKey);
        if (normalized == null) {
            throw new IllegalArgumentException("catalogKey must not be blank");
        }
        return normalized;
    }
}
