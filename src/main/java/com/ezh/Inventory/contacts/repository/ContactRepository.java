package com.ezh.Inventory.contacts.repository;

import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.entiry.ContactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    Boolean existsByContactCode(String contactCode);

    Optional<Contact> findByIdAndTenantId(Long id, Long tenantId);


    @Query(value = """
                SELECT c FROM Contact c
                WHERE 
                    (
                        LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                        LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                        c.phone LIKE CONCAT('%', :search, '%') OR
                        c.gstNumber LIKE CONCAT('%', :search, '%') OR
                        LOWER(c.contactCode) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR :search IS NULL
                    )
                    AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) OR :name IS NULL)
                    AND (LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')) OR :email IS NULL)
                    AND (c.phone LIKE CONCAT('%', :phone, '%') OR :phone IS NULL)
                    AND (c.gstNumber LIKE CONCAT('%', :gst, '%') OR :gst IS NULL)
                    AND (CAST(c.type AS string) = :type OR :type IS NULL)
                    AND (c.active = :active OR :active IS NULL)
            """)
    Page<Contact> searchContacts(
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
