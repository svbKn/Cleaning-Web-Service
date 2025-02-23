package com.example.cleaning.services;

import com.example.cleaning.models.Image;
import com.example.cleaning.models.Product;
import com.example.cleaning.models.User;
import com.example.cleaning.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;


    public void saveProduct(Principal principal, Product product, MultipartFile file1, MultipartFile file2, MultipartFile file3) throws IOException {
        product.setUser(getUserByPrincipal(principal));
        Image image1;
        Image image2;
        Image image3;
        if (file1.getSize() != 0) {
            image1 = toImageEntity(file1);
            image1.setPreviewImage(true);
            product.addImageToProduct(image1);
        }
        if (file2.getSize() != 0) {
            image2 = toImageEntity(file2);
            product.addImageToProduct(image2);
        }
        if (file3.getSize() != 0) {
            image3 = toImageEntity(file3);
            product.addImageToProduct(image3);
        }
        log.info("Saving new Product. Title: {}; Author email: {}", product.getTitle(), product.getUser().getEmail());
        Product productFromDb = productRepository.save(product);
        productFromDb.setPreviewImageId(productFromDb.getImages().get(0).getId());
        productRepository.save(product);
    }

    public User getUserByPrincipal(Principal principal) {
        if (principal == null) return new User();
        return userRepository.findByEmail(principal.getName());
    }

    private Image toImageEntity(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }


    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }


    @Transactional
    public void softDeleteProduct(User user, Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            if (product.getUser().getId().equals(user.getId())) {
                product.setDeleted(true);
                productRepository.save(product);
                log.info("Product with id = {} was soft deleted by user {}", id, user.getEmail());
            } else {
                log.error("User: {} doesn't have permission to delete product with id = {}", user.getEmail(), id);
            }
        } else {
            log.error("Product with id = {} not found", id);
        }
    }

    @Transactional
    public void restoreProduct(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null && product.isDeleted()) {
            product.setDeleted(false);
            productRepository.save(product);
            log.info("Product with id = {} was restored", id);
        } else {
            log.error("Product with id = {} not found or not deleted", id);
        }
    }

    public List<Product> listAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> listActiveProducts(String title) {
        if (title != null && !title.isEmpty()) {
            return productRepository.findByTitleAndDeletedFalse(title);
        }
        return productRepository.findByDeletedFalse();
    }
     @Transactional
    public void deleteProduct(User user, Long id) {
        softDeleteProduct(user, id);
    }
}
