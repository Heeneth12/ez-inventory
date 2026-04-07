package com.ezh.Inventory.notifications.common.dto;

import com.ezh.Inventory.notifications.common.entity.NotificationChannel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines who receives a notification on a specific channel.
 *
 * <p>One {@link NotificationRequest} carries a list of distributors — one per channel.
 * Each distributor owns the full recipient list for that channel, so bulk sending is
 * handled cleanly without any single-value / multi-value mismatch.
 *
 * <pre>
 * "distributors": [
 *   {
 *     "channel": "IN_APP",
 *     "recipientIds": ["user-uuid-1", "user-uuid-2"]
 *   },
 *   {
 *     "channel": "EMAIL",
 *     "toEmails": ["alice@example.com", "bob@example.com"]
 *   },
 *   {
 *     "channel": "WHATSAPP",
 *     "toPhones": ["919876543210", "919876543211"]
 *   },
 *   {
 *     "channel": "PUSH",
 *     "deviceTokens": ["fcm-token-a", "fcm-token-b"]
 *   }
 * ]
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDistributor {

    /** The delivery channel this distributor targets. */
    @NotNull
    private NotificationChannel channel;

    /**
     * User UUIDs for WebSocket (IN_APP) delivery.
     * Leave empty when {@code channel} is not IN_APP,
     * or when the targetScope is GLOBAL / TENANT / GROUP (broadcast — no individual IDs needed).
     */
    @Builder.Default
    private List<String> recipientIds = new ArrayList<>();

    /**
     * Email addresses for EMAIL delivery.
     * Each address produces one {@code NotificationDelivery} row.
     */
    @Builder.Default
    private List<String> toEmails = new ArrayList<>();

    /**
     * E.164 phone numbers for WHATSAPP delivery (no leading '+').
     * Example: {@code "919876543210"}.
     */
    @Builder.Default
    private List<String> toPhones = new ArrayList<>();

    /**
     * FCM / APNs device tokens for PUSH delivery.
     */
    @Builder.Default
    private List<String> deviceTokens = new ArrayList<>();

    /**
     * Returns the relevant recipient list for this channel.
     * Used by the service to iterate without a switch per call-site.
     */
    public List<String> recipients() {
        return switch (channel) {
            case IN_APP   -> recipientIds;
            case EMAIL    -> toEmails;
            case WHATSAPP -> toPhones;
            case PUSH     -> deviceTokens;
        };
    }
}
