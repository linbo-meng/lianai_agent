package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 临时文件下载（tmp 目录下的 pdf / file / download）
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "file", "download");

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam String type,
            @RequestParam String name) {
        if (!ALLOWED_TYPES.contains(type)) {
            return ResponseEntity.badRequest().build();
        }

        String safeName = Path.of(name).getFileName().toString().trim();
        // 兼容前端可能传来的引号包裹
        safeName = safeName.replaceAll("^[\"']+|[\"']+$", "");
        if (safeName.isBlank() || safeName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path baseDir = Path.of(FileConstant.FILE_SAVE_DIR, type).toAbsolutePath().normalize();
        Path target = baseDir.resolve(safeName).normalize();
        if (!target.startsWith(baseDir) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(target);
        String asciiFallback = safeName.replaceAll("[^\\x20-\\x7E]", "_");
        if (asciiFallback.isBlank()) {
            asciiFallback = "file.bin";
        }
        String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");

        MediaType mediaType = resolveMediaType(type, safeName);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedName)
                .contentLength(target.toFile().length())
                .body(resource);
    }

    private static MediaType resolveMediaType(String type, String name) {
        String lower = name.toLowerCase();
        if ("pdf".equals(type) || lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
