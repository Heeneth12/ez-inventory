package com.ezh.Inventory.notifications.common.entity;

/**
 * Lifecycle status of a single {@link NotificationDelivery} record.
 *
 * <pre>
 *  PENDING → SENT → READ        (happy path for IN_APP)
 *  PENDING → SENT               (EMAIL / WHATSAPP / PUSH — no read-receipt)
 *  PENDING → FAILED             (channel error)
 * </pre>
 */
public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    READ
}
