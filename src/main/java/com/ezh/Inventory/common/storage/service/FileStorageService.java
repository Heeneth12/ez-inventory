package com.ezh.Inventory.common.storage.service;

import com.ezh.Inventory.common.storage.dto.FileRecordResponse;
import com.ezh.Inventory.common.storage.dto.FileUploadRequest;
import com.ezh.Inventory.common.storage.dto.PreSignedUrlResponse;
import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import com.ezh.Inventory.utils.common.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Contract for S3-backed file storage used across the inventory application.
 *
 * <p>Every method that accepts or returns a file reference operates on the
 * {@code FileRecord.uuid} – never on raw S3 keys or database IDs, so that
 * callers remain decoupled from the storage implementation.
 */
public interface FileStorageService {

    /**
     * Upload a file to S3 and persist a {@code FileRecord} in the database.
     *
     * @param file       raw bytes from a multipart HTTP request
     * @param request    metadata describing what this file is and who owns it
     * @return read-only view of the newly created {@code FileRecord}
     */
    FileRecordResponse uploadFile(MultipartFile file, FileUploadRequest request);

    /**
     * Retrieve metadata for a single file by its UUID.
     * Does NOT include a pre-signed URL – call {@link #generatePreSignedUrl} for that.
     */
    FileRecordResponse getFileRecord(String uuid);

    /**
     * Download the raw bytes of a file from S3.
     * Use sparingly – prefer pre-signed URLs for large files.
     */
    byte[] downloadFile(String uuid);

    /**
     * Generate a time-limited pre-signed URL for direct S3 download.
     *
     * @param uuid              UUID of the {@code FileRecord}
     * @param expirationMinutes how long the URL should remain valid (max 60 * 24 * 7 = 10 080 min)
     */
    PreSignedUrlResponse generatePreSignedUrl(String uuid, int expirationMinutes);

    /**
     * All active files attached to a specific entity
     * (e.g. every file on Purchase Order {@code poUuid}).
     */
    List<FileRecordResponse> getFilesByReference(String referenceId, FileReferenceType referenceType);

    /**
     * Filtered subset: only files of a particular type on a specific entity
     * (e.g. just the PDFs on an invoice).
     */
    List<FileRecordResponse> getFilesByReferenceAndType(
            String referenceId, FileReferenceType referenceType, FileType fileType);

    /**
     * All active files belonging to a tenant, optionally filtered by {@link FileType}.
     * Pass {@code null} for {@code fileType} to get all files.
     */
    Page<FileRecordResponse> getFilesByTenant(Long tenantId, FileType fileType, Integer page, Integer size);

    /**
     * Soft-delete the {@code FileRecord} in the database.
     * The S3 object is NOT deleted (use lifecycle rules or a separate purge job for that).
     *
     * @param uuid UUID of the file to delete
     */
    CommonResponse<?> softDeleteFile(String uuid);

    /**
     * Hard-delete: removes the S3 object AND the database record.
     * Use only for GDPR/compliance purge scenarios.
     *
     * @param uuid UUID of the file to permanently destroy
     */
    CommonResponse<?> permanentlyDeleteFile(String uuid);
}
