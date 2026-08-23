package com.pricehunter.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String sku,
        @Size(max = 2000) String description
) {
}
