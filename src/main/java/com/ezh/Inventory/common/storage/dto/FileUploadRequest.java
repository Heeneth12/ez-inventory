package com.ezh.Inventory.common.storage.dto;

import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Metadata that must accompany every file upload.
 * The actual file bytes come via {@code MultipartFile} in the controller.
 */
@Data
public class FileUploadRequest {

    /** UUID of the entity this file is attached to. */
    @NotBlank(message = "referenceId is required")
    private String referenceId;

    /** Domain entity type that owns the file. */
    @NotNull(message = "referenceType is required")
    private FileReferenceType referenceType;

    /** Semantic category of the file. */
    @NotNull(message = "fileType is required")
    private FileType fileType;

    /** Multi-tenant identifier. */
    @NotBlank(message = "tenantId is required")
    private String tenantId;

    /** Human-readable description (optional). */
    private String description;

    /** Comma-separated tags for search (optional). */
    private String tags;

    /**
     * When {@code true}, the file is stored with public-read ACL.
     * Defaults to {@code false} – always use pre-signed URLs.
     */
    private boolean isPublic = false;
}
