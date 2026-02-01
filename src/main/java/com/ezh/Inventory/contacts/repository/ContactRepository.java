package com.ezh.Inventory.contacts.repository;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.entiry.ContactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Boolean existsByContactCode(String contactCode);

    @Query("SELECT c.name FROM Contact c WHERE c.id = :id")
    String findNameById(@Param("id") Long id);


    Optional<Contact> findByIdAndTenantId(Long id, Long tenantId);

    @Query("""
                SELECT c FROM Contact c
                WHERE c.tenantId = :tenantId
                  AND (
                        CAST(:search AS string) IS NULL OR
                        LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                        LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                        c.phone LIKE CONCAT('%', CAST(:search AS string), '%') OR
                        c.gstNumber LIKE CONCAT('%', CAST(:search AS string), '%') OR
                        LOWER(c.contactCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                    )
                  AND (CAST(:name AS string) IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
                  AND (CAST(:email AS string) IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:email AS string), '%')))
                  AND (CAST(:phone AS string) IS NULL OR c.phone LIKE CONCAT('%', CAST(:phone AS string), '%'))
                  AND (CAST(:gst AS string) IS NULL OR c.gstNumber LIKE CONCAT('%', CAST(:gst AS string), '%'))
                  AND (:type IS NULL OR c.contactType = :type)
                  AND (:active IS NULL OR c.active = :active)
            """)
    Page<Contact> searchContacts(
            @Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("gst") String gstNumber,
            @Param("type") ContactType type,
            @Param("active") Boolean active,
            Pageable pageable
    );
}