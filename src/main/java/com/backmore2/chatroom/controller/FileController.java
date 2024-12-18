package com.backmore2.chatroom.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class FileController {
    private static final Logger logger = Logger.getLogger(FileController.class.getName());
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final Map<String, Long> fileUploadTimes = new ConcurrentHashMap<>();

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Path location = getFileStorageLocation();
        if (!Files.exists(location)) {
            Files.createDirectories(location);
        }
    }

    private Path getFileStorageLocation() throws IOException {
        Path location = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(location);
        return location;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "target", required = false) String target) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("error", "文件大小不能超过100MB");
                return ResponseEntity.badRequest().body(response);
            }

            // 生成唯一的文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 构建文件存储路径
            String fileName = fileId + fileExtension;
            Path targetLocation = getFileStorageLocation().resolve(fileName);
            
            // 使用try-with-resources确保资源正确关闭
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 记录文件上传时间
            fileUploadTimes.put(fileId, System.currentTimeMillis());
            
            response.put("success", true);
            response.put("fileId", fileId);
            response.put("fileName", originalFilename);
            
            logger.info(String.format("文件上传成功: %s, 用户: %s, 目标用户: %s", fileId, username, target));
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            logger.severe("文件上传失败: " + ex.getMessage());
            response.put("success", false);
            response.put("error", "文件上传失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        try {
            // 查找目录中匹配fileId开头的文件
            Path filePath = Files.list(getFileStorageLocation())
                    .filter(path -> path.getFileName().toString().startsWith(fileId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("文件未找到"));

            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                String contentType = "application/octet-stream";
                String originalFilename = filePath.getFileName().toString();
                
                // 如果文件名包含扩展名，尝试获取对应的 MediaType
                if (originalFilename.contains(".")) {
                    try {
                        contentType = Files.probeContentType(filePath);
                        if (contentType == null) {
                            contentType = "application/octet-stream";
                        }
                    } catch (IOException e) {
                        contentType = "application/octet-stream";
                    }
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            logger.severe("文件下载失败: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 定时清理超过24小时的文件
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupOldFiles() {
        try {
            Path location = getFileStorageLocation();
            long now = System.currentTimeMillis();
            long dayInMillis = 24 * 60 * 60 * 1000;

            Files.list(location).forEach(path -> {
                String fileName = path.getFileName().toString();
                String fileId = fileName.substring(0, fileName.lastIndexOf('.'));
                Long uploadTime = fileUploadTimes.get(fileId);

                if (uploadTime != null && (now - uploadTime > dayInMillis)) {
                    try {
                        Files.deleteIfExists(path);
                        fileUploadTimes.remove(fileId);
                        logger.info("已删除过期文件: " + fileName);
                    } catch (IOException e) {
                        logger.warning("删除文件失败: " + fileName + ", " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            logger.severe("清理文件失败: " + e.getMessage());
        }
    }

    // 清理临时文件
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            if (!deleted) {
                tempFile.deleteOnExit();
            }
        }
    }
} 