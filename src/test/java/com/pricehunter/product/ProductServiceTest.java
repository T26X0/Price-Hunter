package com.pricehunter.product;

import com.pricehunter.shared.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsNormalizedProduct() {
        when(productRepository.existsBySkuIgnoreCase("SKU-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(new ProductRequest("  Phone  ", " sku-1 ", "  A phone  "));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Phone");
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.description()).isEqualTo("A phone");
    }

    @Test
    void rejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new ProductRequest("Phone", "sku-1", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU-1");
        verify(productRepository, never()).save(any());
    }

    @Test
    void listsProductsSortedByName() {
        when(productRepository.findAll(any(Sort.class))).thenReturn(List.of(new Product("Phone", "SKU-1", null)));

        assertThat(productService.findAll()).extracting(ProductResponse::name).containsExactly("Phone");
    }
}
