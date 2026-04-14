package com.ezh.Inventory.common.storage.dto;

import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** Read-only view of a {@code FileRecord} returned to API consumers. */
@Data
@Builder
public class FileRecordResponse {

    private Long id;
    private String uuid;

    // Ownership
    private String tenantId;
    private String referenceId;
    private FileReferenceType referenceType;
    private FileType fileType;

    // File identity
    private String originalFileName;
    private String storedFileName;
    private String s3Key;
    private String s3Bucket;
    private String s3VersionId;

    // Metadata
    private String mimeType;
    private Long fileSizeBytes;
    private String checksum;
    private boolean isPublic;

    // Optional descriptors
    private String description;
    private String tags;

    // Audit
    private String uploadedBy;
    private Date createdAt;
    private Date updatedAt;

    /**
     * Pre-signed download URL – populated only when explicitly requested
     * (e.g. via the {@code /presigned} endpoint). Null otherwise.
     */
    private String preSignedUrl;
}
