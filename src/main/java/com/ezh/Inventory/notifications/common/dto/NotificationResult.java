package com.ezh.Inventory.notifications.common.dto;

import com.ezh.Inventory.notifications.common.entity.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Result returned after processing a {@link NotificationRequest}.
 *
 * <p>Reports the saved notification ID, aggregate totals, and a per-channel
 * breakdown so callers know exactly what was delivered to whom.
 *
 * <pre>
 * {
 *   "notificationId": 42,
 *   "totalDispatched": 6,
 *   "totalSent": 5,
 *   "totalFailed": 1,
 *   "channelSummary": {
 *     "EMAIL":    { "sent": 2, "failed": 0, "failedRecipients": [] },
 *     "WHATSAPP": { "sent": 2, "failed": 1, "failedRecipients": ["9198XXXXXXXX"] },
 *     "IN_APP":   { "sent": 1, "failed": 0, "failedRecipients": [] }
 *   }
 * }
 * </pre>
 */
@Data
@Builder
public class NotificationResult {

    /** ID of the persisted {@code Notification} record. */
    private Long notificationId;

    /** Total number of individual delivery attempts across all channels. */
    private int totalDispatched;

    /** Total deliveries that reached the channel provider successfully. */
    private int totalSent;

    /** Total deliveries that failed. */
    private int totalFailed;

    /**
     * Breakdown per channel.
     * Key = channel, Value = {@link ChannelSummary} with counts and failed recipient refs.
     */
    private Map<NotificationChannel, ChannelSummary> channelSummary;

    // ── Nested summary ────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class ChannelSummary {

        /** Number of recipients successfully dispatched on this channel. */
        private int sent;

        /** Number of recipients that failed on this channel. */
        private int failed;

        /**
         * Recipient references (email / phone / userId / deviceToken) that failed.
         * Useful for retry logic or alerting.
         */
        private List<String> failedRecipients;
    }
}
