package com.ezh.Inventory.utils.common;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@MappedSuperclass
@Data
public abstract class CommonSerializable implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    protected Long id;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    protected String uuid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Date createdAt;

    @Column(name = "created_by")
    protected Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    protected Date updatedAt;

    @Column(name = "is_deleted")
    protected Boolean isDeleted;

    @PrePersist
    protected void onCreate() {
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
        }
    }

}
