package com.ezh.Inventory.common.storage.controller;

import com.ezh.Inventory.common.storage.dto.FileRecordResponse;
import com.ezh.Inventory.common.storage.dto.FileUploadRequest;
import com.ezh.Inventory.common.storage.dto.PreSignedUrlResponse;
import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import com.ezh.Inventory.common.storage.service.FileStorageService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API for the S3 file storage module.
 *
 * <p>Base path: {@code /v1/files}
 *
 * <p>All endpoints are JWT-protected (see {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileStorageController {

    private static final int DEFAULT_PRESIGN_MINUTES = 60;

    private final FileStorageService fileStorageService;

    /**
     * POST /v1/files/upload
     * Upload a file along with its ownership metadata.
     *
     * <p>Multipart form fields:
     * <ul>
     *   <li>{@code file}          – binary file part</li>
     *   <li>{@code referenceId}   – UUID of the owning entity</li>
     *   <li>{@code referenceType} – e.g. PURCHASE_ORDER, INVOICE …</li>
     *   <li>{@code fileType}      – e.g. PURCHASE_ORDER_PDF, ITEM_IMAGE …</li>
     *   <li>{@code tenantId}      – multi-tenant identifier</li>
     *   <li>{@code description}   – (optional) human-readable label</li>
     *   <li>{@code tags}          – (optional) comma-separated tags</li>
     *   <li>{@code isPublic}      – (optional, default false)</li>
     * </ul>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileRecordResponse> upload(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute FileUploadRequest request) {

        FileRecordResponse response = fileStorageService.uploadFile(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * GET /v1/files/{uuid}
     * Fetch metadata for a single file. Does NOT return file bytes or URL.
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<FileRecordResponse> getRecord(@PathVariable String uuid) {
        return ResponseEntity.ok(fileStorageService.getFileRecord(uuid));
    }

    /**
     * GET /v1/files/{uuid}/download
     * Stream file bytes directly from S3 (suitable for small files).
     * For large files, prefer the {@code /presigned} endpoint.
     */
    @GetMapping("/{uuid}/download")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        FileRecordResponse meta  = fileStorageService.getFileRecord(uuid);
        byte[]             bytes = fileStorageService.downloadFile(uuid);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        meta.getMimeType() != null ? meta.getMimeType() : "application/octet-stream"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    /**
     * GET /v1/files/{uuid}/presigned?expirationMinutes=60
     * Generate a time-limited pre-signed S3 URL for direct download.
     */
    @GetMapping("/{uuid}/presigned")
    public ResponseEntity<PreSignedUrlResponse> presigned(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "60") int expirationMinutes) {

        return ResponseEntity.ok(
                fileStorageService.generatePreSignedUrl(uuid, expirationMinutes));
    }

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * GET /v1/files/reference/{referenceId}?referenceType=PURCHASE_ORDER
     * All files attached to a specific entity.
     */
    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<List<FileRecordResponse>> byReference(
            @PathVariable String referenceId,
            @RequestParam FileReferenceType referenceType,
            @RequestParam(required = false) FileType fileType) {

        List<FileRecordResponse> result = (fileType == null)
                ? fileStorageService.getFilesByReference(referenceId, referenceType)
                : fileStorageService.getFilesByReferenceAndType(referenceId, referenceType, fileType);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /v1/files/tenant/{tenantId}?fileType=INVOICE_PDF&page=0&size=10
     * All files belonging to a tenant, optionally filtered by file type.
     */
    @GetMapping("/tenant")
    public ResponseEntity<ResponseResource<Page<FileRecordResponse>>> byTenant(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) FileType fileType) {

        Page<FileRecordResponse> response = fileStorageService.getFilesByTenant(tenantId, fileType, page, size);

        return ResponseEntity.ok(
                ResponseResource.success(HttpStatus.OK, response, "Files fetched successfully")
        );
    }


    /**
     * DELETE /v1/files/{uuid}
     * Soft-delete: marks the record as deleted; S3 object is retained.
     */
    @DeleteMapping("/{uuid}")
    public ResponseResource<CommonResponse<?>> softDelete(@PathVariable String uuid) {
         CommonResponse<?> response =  fileStorageService.softDeleteFile(uuid);
        return ResponseResource.success(HttpStatus.OK, response, "Approvals fetched successfully");
    }

    /**
     * DELETE /v1/files/{uuid}/permanent
     * Hard-delete: removes both the S3 object and the database record.
     * Intended for GDPR/compliance purge requests only.
     */
    @DeleteMapping("/{uuid}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable String uuid) {
        fileStorageService.permanentlyDeleteFile(uuid);
        return ResponseEntity.noContent().build();
    }
}
