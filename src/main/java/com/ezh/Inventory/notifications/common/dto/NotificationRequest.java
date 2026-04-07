package com.ezh.Inventory.notifications.common.dto;

import com.ezh.Inventory.notifications.common.entity.NotificationType;
import com.ezh.Inventory.notifications.common.entity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified notification request.
 *
 * <p>The message (subject / body) is defined once. Each {@link NotificationDistributor}
 * in the {@code distributors} list owns a channel and its full recipient list,
 * supporting bulk delivery across any number of channels simultaneously.
 *
 * <pre>
 * POST /api/notifications/send
 * {
 *   "type":        "INFO",
 *   "targetScope": "USER",
 *   "from":        "noreply@ezh.com",
 *   "subject":     "Your order has been shipped",
 *   "body":        "Order #4521 is on its way.",
 *   "distributors": [
 *     {
 *       "channel": "IN_APP",
 *       "recipientIds": ["user-uuid-1", "user-uuid-2"]
 *     },
 *     {
 *       "channel": "EMAIL",
 *       "toEmails": ["alice@example.com", "bob@example.com"]
 *     },
 *     {
 *       "channel": "WHATSAPP",
 *       "toPhones": ["919876543210", "919876543211"]
 *     }
 *   ],
 *   "metadata": { "orderId": "4521", "sentBy": "admin-uuid" }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private String targetId;

    @Builder.Default
    private NotificationType type = NotificationType.INFO;

    /**
     * Audience scope this notification targets.
     * One notification has exactly one scope.
     */
    @NotNull
    private TargetType targetScope;

    private String from;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    /**
     * One entry per channel, each carrying its own recipient list.
     * At least one distributor is required.
     */
    @NotEmpty
    @Builder.Default
    private List<NotificationDistributor> distributors = new ArrayList<>();


    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
}
