package com.ezh.Inventory.notifications.common.entity;

import com.ezh.Inventory.notifications.common.dto.NotificationRequest;

/**
 * Delivery channel used when sending a notification.
 * A single {@link NotificationRequest}
 * may request one or more channels simultaneously.
 */
public enum NotificationChannel {

    /** Real-time in-app delivery via WebSocket / STOMP */
    IN_APP,

    /** Email delivery via JavaMailSender (spring-boot-starter-mail) */
    EMAIL,

    /** WhatsApp delivery via Meta Cloud API */
    WHATSAPP,

    /** Mobile push notification via FCM / APNs */
    PUSH
}
