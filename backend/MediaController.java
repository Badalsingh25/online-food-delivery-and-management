package com.hungerexpress.media;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    
    private static final String UPLOAD_DIR = "uploads/";
    
    public MediaController() {
        // Create uploads directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("[MediaController] Created uploads directory: " + uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[MediaController] Failed to create uploads directory: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("[MediaController] Upload request received. File: " + file.getOriginalFilename());
        
        if (file.isEmpty()) {
            System.out.println("[MediaController] File is empty");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            
            // Save file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return URL
            String url = "/api/media/" + filename;
            System.out.println("[MediaController] File uploaded successfully: " + url);
            
            return ResponseEntity.ok(new UploadResponse(url));
            
        } catch (IOException e) {
            System.err.println("[MediaController] Upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
            
            if (!Files.exists(filePath)) {
                System.out.println("[MediaController] File not found: " + filename);
                return ResponseEntity.notFound().build();
            }
            
            byte[] data = Files.readAllBytes(filePath);
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(data);
                    
        } catch (IOException e) {
            System.err.println("[MediaController] Failed to read file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    record UploadResponse(String url) {}
}
