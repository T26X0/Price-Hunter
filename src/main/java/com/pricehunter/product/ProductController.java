package com.pricehunter.product;

import com.pricehunter.shared.ApiRoutes;
import com.pricehunter.shared.SliceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.Products.ROOT)
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Модели товаров и их конфигурации")
/** REST-контроллер ручного создания и публичного чтения моделей каталога. */
public class ProductController {

    private final ProductService productService;
    private final CatalogQueryService catalogQueryService;

    /** Создаёт модель после валидации тела запроса. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать модель товара вручную")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    /** Возвращает постраничный список моделей с ценовыми агрегатами актуальных предложений. */
    @GetMapping
    @Operation(summary = "Поиск моделей каталога")
    public SliceResponse<ProductListResponse> findAll(
            @Parameter(description = "Фрагмент названия или ключа товара") @RequestParam(required = false) String q,
            @Parameter(description = "Город, в котором считаются цены") @RequestParam(required = false) UUID cityId,
            @Parameter(description = "Код категории, например smartphones")
            @RequestParam(required = false) String category,
            @Parameter(description = "Бренд товара, например Apple") @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalogQueryService.findProducts(q, cityId, category, brand, page, size);
    }

    /** Возвращает полную карточку по стабильному catalogKey, а не по смешанному набору идентификаторов. */
    @GetMapping(ApiRoutes.Products.BY_IDENTIFIER)
    @Operation(summary = "Карточка модели товара")
    public ProductDetailResponse findByCatalogKey(
            @PathVariable String catalogKey,
            @Parameter(description = "Необязательный город для цен и наличия") @RequestParam(required = false) UUID cityId) {
        return catalogQueryService.findProduct(catalogKey, cityId);
    }

    /** Возвращает только варианты модели, когда детали самой модели frontend уже получил ранее. */
    @GetMapping(ApiRoutes.Products.VARIANTS)
    @Operation(summary = "Варианты модели товара")
    public List<ProductVariantResponse> findVariants(
            @PathVariable String catalogKey,
            @Parameter(description = "Необязательный город для цен и наличия") @RequestParam(required = false) UUID cityId) {
        return catalogQueryService.findVariants(catalogKey, cityId);
    }
}
