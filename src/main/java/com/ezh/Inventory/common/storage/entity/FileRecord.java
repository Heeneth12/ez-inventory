package com.ezh.Inventory.common.storage.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Persistent record for every file uploaded to AWS S3.
 *
 * <p>S3 key anatomy:
 * <pre>
 *   {tenantId}/{referenceType}/{referenceId}/{fileType}/{storedFileName}
 * </pre>
 *
 * <p>Relationships:
 * <ul>
 *   <li>{@code referenceType} – which domain entity type owns this file</li>
 *   <li>{@code referenceId}   – UUID of that domain entity</li>
 *   <li>{@code fileType}      – semantic category of the file</li>
 * </ul>
 */
@Entity
@Table(name = "file_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FileRecord extends CommonSerializable {


    /** Multi-tenant identifier – matches tenantId used across the application. */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * UUID of the entity this file is attached to
     * (e.g. the UUID of a PurchaseOrder, Item, Invoice …).
     */
    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    /** Domain entity type that owns this file. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 60)
    private FileReferenceType referenceType;

    /** Semantic category of the file (image, PDF, Excel export …). */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 60)
    private FileType fileType;

    // ── File Identity ─────────────────────────────────────────────────────────

    /** Original filename as provided by the uploader. */
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /**
     * Stored filename in S3 – always a UUID + original extension.
     * Example: {@code a3f1cc2d-4b5e-4f6a-9c1d-abc123456789.pdf}
     */
    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    /**
     * Full S3 object key.
     * Example: {@code tenant-42/PURCHASE_ORDER/po-uuid/PURCHASE_ORDER_PDF/stored.pdf}
     */
    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    /** S3 bucket where the file lives. */
    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;

    /**
     * S3 object version ID (non-null only when bucket versioning is enabled).
     * Useful for auditing exactly which version of a document was uploaded.
     */
    @Column(name = "s3_version_id")
    private String s3VersionId;

    // ── File Metadata ─────────────────────────────────────────────────────────

    /** MIME type detected/provided at upload time (e.g. {@code image/jpeg}). */
    @Column(name = "mime_type", length = 128)
    private String mimeType;

    /** File size in bytes at upload time. */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** MD5 checksum of the raw bytes for integrity verification. */
    @Column(name = "checksum", length = 64)
    private String checksum;

    // ── Access Control ────────────────────────────────────────────────────────

    /**
     * When {@code true} the file is stored with public-read ACL and can be
     * accessed without a pre-signed URL.  Defaults to {@code false}.
     */
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    // ── Optional Descriptive Fields ───────────────────────────────────────────

    /** Human-readable description of what this file represents. */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * Comma-separated tags for free-form search.
     * Example: {@code "q1-2025,audit,signed"}
     */
    @Column(name = "tags", length = 512)
    private String tags;

}
