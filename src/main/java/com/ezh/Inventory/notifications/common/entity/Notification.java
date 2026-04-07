package com.ezh.Inventory.notifications.common.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single notification broadcast — the <em>message</em> itself.
 *
 * <p>One {@code Notification} row is created per "send operation" regardless of
 * how many recipients or channels are involved. The actual per-recipient, per-channel
 * tracking is done in {@link NotificationDelivery}.
 *
 * <h3>Example — sending to a group via 3 channels</h3>
 * <pre>
 *   Notification  (1 row)
 *     subject = "Stock alert"
 *     body    = "Item X is running low."
 *     type    = WARNING
 *     targetScope = GROUP
 *     targetId    = "warehouse-team"
 * </pre>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends CommonSerializable {

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_scope", length = 50, nullable = false)
    private TargetType targetScope;

    @Column(name = "sent_by")
    private String sentBy;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NotificationDelivery> deliveries = new ArrayList<>();

}
