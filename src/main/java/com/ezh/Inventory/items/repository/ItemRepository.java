package com.ezh.Inventory.items.repository;

import com.ezh.Inventory.items.entity.Item;
import com.ezh.Inventory.items.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByItemCode(String itemCode);

    List<Item> findByIdIn(List<Long> ids);

    @Query("SELECT i FROM Item i WHERE i.id IN :ids")
    List<Item> findIdAndNameByIdIn(@Param("ids") List<Long> ids);

    Optional<Item> findByIdAndTenantId(Long id, Long tenantId);

    Page<Item> findAllByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT i.name FROM Item i WHERE i.id = :id")
    Optional<String> findNameById(@Param("id") Long id);

    List<Item> findAllByIsActiveTrue();

    @Query("""
                SELECT i FROM Item i
                WHERE i.isActive = true AND (
                       LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                       LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :query, '%')) OR
                       LOWER(i.barcode) LIKE LOWER(CONCAT('%', :query, '%'))
                )
                ORDER BY 
                    CASE 
                        WHEN LOWER(i.name) = LOWER(:query) THEN 1
                        WHEN LOWER(i.itemCode) = LOWER(:query) THEN 2
                        ELSE 3
                    END,
                    i.name ASC
            """)
    List<Item> smartSearch(@Param("query") String query);

    @Query("""
            SELECT i FROM Item i 
            WHERE i.tenantId = :tenantId
            AND (:searchQuery IS NULL OR 
                LOWER(i.name) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)) OR 
                LOWER(i.itemCode) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text)) OR 
                LOWER(i.barcode) LIKE LOWER(CAST(CONCAT('%', :searchQuery, '%') AS text))) 
            AND (:active IS NULL OR i.isActive = :active) 
            AND (CAST(:itemTypes AS text) IS NULL OR i.itemType IN :itemTypes)
            AND (:brand IS NULL OR LOWER(i.brand) LIKE LOWER(CAST(CONCAT('%', :brand, '%') AS text))) 
            AND (:category IS NULL OR LOWER(i.category) LIKE LOWER(CAST(CONCAT('%', :category, '%') AS text)))
            """)
    Page<Item> searchItems(
            @Param("tenantId") Long tenantId,
            @Param("searchQuery") String searchQuery,
            @Param("active") Boolean active,
            @Param("itemTypes") List<ItemType> itemTypes,
            @Param("brand") String brand,
            @Param("category") String category,
            Pageable pageable
    );

}