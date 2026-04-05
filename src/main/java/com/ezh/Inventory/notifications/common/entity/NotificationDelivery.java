package com.ezh.Inventory.notifications.common.entity;

import com.ezh.Inventory.utils.common.CommonSerializable;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * One delivery attempt: a single recipient on a single channel.
 *
 * <p>Kept intentionally minimal — routing data (email, phone, etc.) belongs in
 * the request, not in the tracking record. This table only answers:
 * <em>who got it, via which channel, did it succeed, and have they read it?</em>
 *
 * <p>Index strategy:
 * <ul>
 *   <li>{@code recipient_ref + channel} — user's notification feed</li>
 *   <li>{@code recipient_ref + is_read} — unread badge count</li>
 *   <li>{@code notification_id}         — analytics per notification</li>
 *   <li>{@code status + channel}        — retry sweep</li>
 * </ul>
 */
@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery extends CommonSerializable {

    /** The parent notification message. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /**
     * The actual recipient identifier for this channel:
     * <ul>
     *   <li>IN_APP   → userId (UUID)</li>
     *   <li>EMAIL    → email address</li>
     *   <li>WHATSAPP → E.164 phone number</li>
     *   <li>PUSH     → FCM / APNs device token</li>
     * </ul>
     */
    @Column(name = "recipient_ref", nullable = false)
    private String recipientRef;

    /** The channel used for this delivery attempt. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    /** Current lifecycle status. Defaults to PENDING. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private Date readAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
