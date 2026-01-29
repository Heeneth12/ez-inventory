package com.ezh.Inventory.notifications.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends CommonSerializable {

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50, nullable = false)
    private TargetType targetType;

    private String targetId;
    private boolean isRead = false;
}