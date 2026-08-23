package com.pricehunter.product;

import com.pricehunter.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
