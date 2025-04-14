package com.example.api.controller;

import com.example.api.AbstractIntegrationTests;
import com.example.api.dto.ProductDTO;
import com.example.api.entity.Product;
import com.example.api.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIntegrationTest extends AbstractIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final String baseUrl = "http://localhost:" + port + "/api";

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }


    @Test
    void shouldCreateNewProduct() {
        // Given
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName("Test Product");
        productDTO.setDescription("This is a test product");
        productDTO.setPrice(new BigDecimal("19.99"));
        productDTO.setStockQuantity(100);

        // When
        ResponseEntity<ProductDTO> response = restTemplate.postForEntity(baseUrl + "/products", productDTO, ProductDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Test Product");
        assertThat(response.getBody().getPrice()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        createTestProduct("Product 1", new BigDecimal("10.00"), 10);
        createTestProduct("Product 2", new BigDecimal("20.00"), 20);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(baseUrl + "/products", HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductDTO>>() {
        });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    @Test
    void shouldSearchProductsByName() {
        // Given
        createTestProduct("Special Product", new BigDecimal("15.00"), 5);
        createTestProduct("Regular Product", new BigDecimal("10.00"), 10);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(baseUrl + "/products/search?name=Special", HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductDTO>>() {
        });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Special Product");
    }

    @Test
    void shouldGetInStockProducts() {
        // Given
        createTestProduct("In Stock", new BigDecimal("15.00"), 5);
        createTestProduct("Out of Stock", new BigDecimal("10.00"), 0);

        // When
        ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(baseUrl + "/products/in-stock", HttpMethod.GET, null, new ParameterizedTypeReference<List<ProductDTO>>() {
        });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("In Stock");
    }

    private Product createTestProduct(String name, BigDecimal price, int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        return productRepository.save(product);
    }
}