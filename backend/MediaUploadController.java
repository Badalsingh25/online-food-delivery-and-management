package com.hungerexpress.media;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private static final Path ROOT = Paths.get("uploads");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,String>> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (!Files.exists(ROOT)) Files.createDirectories(ROOT);
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String name = UUID.randomUUID().toString() + (ext!=null? ("."+ext) : "");
        Path dest = ROOT.resolve(name);
        Files.copy(file.getInputStream(), dest);
        // Serve via /api/media/{name}
        return ResponseEntity.ok(Map.of("url", "/api/media/"+name));
    }

    @GetMapping(value = "/{name}")
    public ResponseEntity<byte[]> get(@PathVariable String name) throws IOException {
        Path p = ROOT.resolve(name);
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(p);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(data);
    }
}
