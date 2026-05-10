package com.ezh.Inventory.common.storage.repository;

import com.ezh.Inventory.common.storage.entity.FileRecord;
import com.ezh.Inventory.common.storage.entity.FileReferenceType;
import com.ezh.Inventory.common.storage.entity.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    /**
     * Find a single active file by its UUID.
     */
    Optional<FileRecord> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * All active files attached to a specific entity (e.g. all files on PO-123).
     */
    List<FileRecord> findByReferenceIdAndReferenceTypeAndIsDeletedFalse(
            String referenceId, FileReferenceType referenceType);

    /**
     * All active files of a specific category on a specific entity.
     */
    List<FileRecord> findByReferenceIdAndReferenceTypeAndFileTypeAndIsDeletedFalse(
            String referenceId, FileReferenceType referenceType, FileType fileType);

    /**
     * All active files belonging to a tenant.
     */
    List<FileRecord> findByTenantIdAndIsDeletedFalse(String tenantId);

    /**
     * All active files of a given type across a tenant.
     */
    List<FileRecord> findByTenantIdAndFileTypeAndIsDeletedFalse(
            String tenantId, FileType fileType);

    /**
     * All active files of a given reference type across a tenant (e.g. all INVOICE
     * files).
     */
    List<FileRecord> findByTenantIdAndReferenceTypeAndIsDeletedFalse(
            String tenantId, FileReferenceType referenceType);

    /**
     * Existence check – used before generating a pre-signed URL.
     */
    boolean existsByUuidAndIsDeletedFalse(String uuid);

    Page<FileRecord> findByTenantIdAndFileTypeAndIsDeletedFalse(Long tenantId, FileType fileType,
                                                                Pageable pageable);
}
