package com.pricehunter.product;

import com.pricehunter.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/** Бизнес-операции ручного каталога: создание без дублей и чтение моделей. */
public class ProductService {

    private final ProductRepository productRepository;

    /** Нормализует SKU и создаёт товар, если такой ключ ещё не занят. */
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists");
        }

        String description = request.description() == null ? null : request.description().trim();
        Product product = new Product(request.name().trim(), sku, description);
        return ProductResponse.from(productRepository.save(product));
    }

    /** Возвращает DTO каталога через оптимизированную проекцию. */
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllSummaries()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
