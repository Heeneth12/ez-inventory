package com.ezh.Inventory.common.storage.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** Response envelope for a generated pre-signed S3 URL. */
@Data
@Builder
public class PreSignedUrlResponse {

    /** UUID of the FileRecord this URL grants access to. */
    private String fileUuid;

    /** The original filename for display purposes. */
    private String originalFileName;

    /** Temporary, signed URL that grants access to the S3 object. */
    private String url;

    /** UTC instant at which the URL stops being valid. */
    private Instant expiresAt;

    /** Minutes the URL remains valid (for display in UIs). */
    private int expirationMinutes;
}
