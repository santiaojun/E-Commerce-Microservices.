package com.cs6650.product.service;

import com.cs6650.product.dto.Product;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    private final ConcurrentHashMap<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Create a new product
     */
    public Product createProduct(Product product) {
        Long productId = idGenerator.getAndIncrement();
        product.setProductId(productId);
        productStore.put(productId, product);
        return product;
    }

    /**
     * Get product by ID
     */
    public Optional<Product> getProductById(Long productId) {
        return Optional.ofNullable(productStore.get(productId));
    }

    /**
     * Get total number of products
     */
    public int getTotalProducts() {
        return productStore.size();
    }
}
