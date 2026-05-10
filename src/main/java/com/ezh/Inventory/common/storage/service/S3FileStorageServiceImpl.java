package com.ezh.Inventory.common.storage.service;

import com.ezh.Inventory.common.storage.dto.FileRecordResponse;
import com.ezh.Inventory.common.storage.dto.FileUploadRequest;
import com.ezh.Inventory.common.storage.dto.PreSignedUrlResponse;
import com.ezh.Inventory.common.storage.entity.FileRecord;
import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import com.ezh.Inventory.common.storage.repository.FileRecordRepository;
import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.entity.ItemType;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRecordRepository fileRecordRepository;

    @Value("${aws.s3.bucket}")
    private String defaultBucket;

    @Override
    @Transactional
    public FileRecordResponse uploadFile(MultipartFile file,
                                         FileUploadRequest request) {
        try {
            String userEmail = UserContextUtil.getEmail();
            byte[] bytes = file.getBytes();
            String originalName = sanitizeFileName(file.getOriginalFilename());
            String extension = extractExtension(originalName);
            String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

            // S3 key: {tenantId}/{referenceType}/{referenceId}/{fileType}/{storedFileName}
            String s3Key = buildS3Key(request.getTenantId(),
                    request.getReferenceType(),
                    request.getReferenceId(),
                    request.getFileType(),
                    storedName);

            String checksum = md5Hex(bytes);
            String mimeType = resolveMimeType(file.getContentType(), extension);

            PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                    .bucket(defaultBucket)
                    .key(s3Key)
                    .contentType(mimeType)
                    .contentLength((long) bytes.length)
                    .metadata(java.util.Map.of(
                            "originalFileName", originalName,
                            "referenceId", request.getReferenceId(),
                            "referenceType", request.getReferenceType().name(),
                            "fileType", request.getFileType().name(),
                            "tenantId", request.getTenantId(),
                            "uploadedBy", userEmail,
                            "checksum", checksum));

            if (request.isPublic()) {
                putBuilder.acl(ObjectCannedACL.PUBLIC_READ);
            }

            PutObjectResponse putResponse = s3Client.putObject(
                    putBuilder.build(),
                    RequestBody.fromBytes(bytes));

            FileRecord record = FileRecord.builder()
                    .tenantId(request.getTenantId())
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .fileType(request.getFileType())
                    .originalFileName(originalName)
                    .storedFileName(storedName)
                    .s3Key(s3Key)
                    .s3Bucket(defaultBucket)
                    .s3VersionId(putResponse.versionId())
                    .mimeType(mimeType)
                    .fileSizeBytes((long) bytes.length)
                    .checksum(checksum)
                    .isPublic(request.isPublic())
                    .description(request.getDescription())
                    .tags(request.getTags())
                    .build();

            FileRecord saved = fileRecordRepository.save(record);
            log.info("File uploaded: uuid={} key={} size={} bytes", saved.getUuid(), s3Key, bytes.length);
            return toResponse(saved);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }
    }

    @Override
    public FileRecordResponse getFileRecord(String uuid) {
        return toResponse(findActiveOrThrow(uuid));
    }

    @Override
    public byte[] downloadFile(String uuid) {
        FileRecord record = findActiveOrThrow(uuid);
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(record.getS3Bucket())
                        .key(record.getS3Key())
                        .build());
        return response.asByteArray();
    }

    @Override
    public PreSignedUrlResponse generatePreSignedUrl(String uuid, int expirationMinutes) {
        FileRecord record = findActiveOrThrow(uuid);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(record.getS3Bucket())
                        .key(record.getS3Key())
                        .build())
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        Instant expiresAt = Instant.now().plusSeconds((long) expirationMinutes * 60);

        log.debug("Pre-signed URL generated for uuid={} expires={}", uuid, expiresAt);

        return PreSignedUrlResponse.builder()
                .fileUuid(uuid)
                .originalFileName(record.getOriginalFileName())
                .url(presigned.url().toString())
                .expiresAt(expiresAt)
                .expirationMinutes(expirationMinutes)
                .build();
    }

    @Override
    public List<FileRecordResponse> getFilesByReference(String referenceId,
                                                        FileReferenceType referenceType) {
        return fileRecordRepository
                .findByReferenceIdAndReferenceTypeAndIsDeletedFalse(referenceId, referenceType)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<FileRecordResponse> getFilesByReferenceAndType(String referenceId,
                                                               FileReferenceType referenceType,
                                                               FileType fileType) {
        return fileRecordRepository
                .findByReferenceIdAndReferenceTypeAndFileTypeAndIsDeletedFalse(referenceId, referenceType, fileType)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public Page<FileRecordResponse> getFilesByTenant(Long tenantId, FileType fileType, Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<FileRecord> fileRecords = fileRecordRepository.findByTenantIdAndFileTypeAndIsDeletedFalse(tenantId,
                fileType, pageable);
        return fileRecords.map(this::toResponse);
    }

    @Override
    @Transactional
    public CommonResponse<?> softDeleteFile(String uuid) {
        FileRecord record = findActiveOrThrow(uuid);
        record.setIsDeleted(true);
        fileRecordRepository.save(record);
        log.info("File soft-deleted: uuid={}", uuid);

        return CommonResponse.builder()
                .id(uuid)
                .message("File soft-deleted: " + uuid)
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> permanentlyDeleteFile(String uuid) {
        FileRecord record = findActiveOrThrow(uuid);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(record.getS3Bucket())
                .key(record.getS3Key())
                .build());
        fileRecordRepository.delete(record);
        log.warn("File permanently deleted: uuid={} key={}", uuid, record.getS3Key());
        return CommonResponse.builder()
                .id(uuid)
                .message("File permanently deleted:" + uuid + " key= " + record.getS3Key())
                .build();

    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FileRecord findActiveOrThrow(String uuid) {
        return fileRecordRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new NoSuchElementException("FileRecord not found or deleted: " + uuid));
    }

    /**
     * S3 key structure:
     * {@code {tenantId}/{referenceType}/{referenceId}/{fileType}/{storedFileName}}
     */
    private String buildS3Key(String tenantId, FileReferenceType refType,
                              String refId, FileType fileType, String storedFileName) {
        return String.join("/",
                tenantId,
                refType.name().toLowerCase(),
                refId,
                fileType.name().toLowerCase(),
                storedFileName);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank())
            return "upload";
        // Strip path traversal characters
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private String resolveMimeType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank()
                && !contentType.equals("application/octet-stream")) {
            return contentType;
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    private String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private FileRecordResponse toResponse(FileRecord r) {
        return FileRecordResponse.builder()
                .id(r.getId())
                .uuid(r.getUuid())
                .tenantId(r.getTenantId())
                .referenceId(r.getReferenceId())
                .referenceType(r.getReferenceType())
                .fileType(r.getFileType())
                .originalFileName(r.getOriginalFileName())
                .storedFileName(r.getStoredFileName())
                .s3Key(r.getS3Key())
                .s3Bucket(r.getS3Bucket())
                .s3VersionId(r.getS3VersionId())
                .mimeType(r.getMimeType())
                .fileSizeBytes(r.getFileSizeBytes())
                .checksum(r.getChecksum())
                .isPublic(Boolean.TRUE.equals(r.getIsPublic()))
                .description(r.getDescription())
                .tags(r.getTags())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
