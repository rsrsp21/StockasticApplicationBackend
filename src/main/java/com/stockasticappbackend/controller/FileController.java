package com.stockasticappbackend.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving static file resources.
 * Provides endpoints to access profile images, KYC documents, and stock images.
 * File access permissions vary by endpoint.
 */
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String profileDir;

    @Value("${file.upload-kyc}")
    private String kycDir;

    @Value("${file.upload-stocks:uploads/stocks}")
    private String stocksDir;

    /**
     * Serves a user profile image.
     * 
     * @param filename The name of the profile image file.
     * @return ResponseEntity containing the image as a Resource.
     */
    @GetMapping("/profile/{filename:.+}")
    public ResponseEntity<Resource> profileImage(@PathVariable String filename) {
        return serve(profileDir, filename);
    }

    /**
     * Serves a KYC document (admin access only).
     * 
     * @param path The path to the KYC document.
     * @return ResponseEntity containing the document as a Resource.
     */
    @GetMapping("/kyc/{path:.+}")
    public ResponseEntity<Resource> kycDocument(@PathVariable String path) {
        return serve(kycDir, path);
    }

    /**
     * Serves a stock image.
     * 
     * @param filename The name of the stock image file.
     * @return ResponseEntity containing the image as a Resource.
     */
    @GetMapping("/stocks/{filename:.+}")
    public ResponseEntity<Resource> stockImage(@PathVariable String filename) {
        return serve(stocksDir, filename);
    }

    /**
     * Helper method to serve a file from a specified directory.
     * 
     * @param baseDir The base directory path.
     * @param path    The relative file path within the directory.
     * @return ResponseEntity with the file content or appropriate error status.
     */
    private ResponseEntity<Resource> serve(String baseDir, String path) {
        try {
            Path filePath = Paths.get(baseDir)
                    .resolve(path)
                    .normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(
                            contentType != null
                                    ? contentType
                                    : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}