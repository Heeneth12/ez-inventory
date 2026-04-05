package com.ezh.Inventory.notifications.common.controller;

import com.ezh.Inventory.notifications.common.dto.NotificationDistributor;
import com.ezh.Inventory.notifications.common.dto.NotificationRequest;
import com.ezh.Inventory.notifications.common.dto.NotificationResult;
import com.ezh.Inventory.notifications.common.entity.NotificationChannel;
import com.ezh.Inventory.notifications.common.entity.NotificationType;
import com.ezh.Inventory.notifications.common.entity.TargetType;
import com.ezh.Inventory.notifications.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    /**
     * Send a notification through any number of channels and recipients.
     *
     * <pre>
     * POST /api/notifications/send
     * {
     *   "type":        "INFO",
     *   "targetScope": "USER",
     *   "subject":     "Order Shipped",
     *   "body":        "Your order #4521 is on its way.",
     *   "distributors": [
     *     { "channel": "IN_APP",   "recipientIds": ["uuid-1", "uuid-2"] },
     *     { "channel": "EMAIL",    "toEmails":     ["a@x.com", "b@x.com"] },
     *     { "channel": "WHATSAPP", "toPhones":     ["919876543210"] }
     *   ],
     *   "metadata": { "sentBy": "admin-uuid" }
     * }
     * </pre>
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationResult> send(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.send(request));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read state
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Mark a specific delivery record as read.
     *
     * @param deliveryId ID of the {@code NotificationDelivery} row
     */
    @PostMapping("/delivery/{deliveryId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long deliveryId) {
        notificationService.markDeliveryAsRead(deliveryId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all unread IN_APP deliveries as read for a user.
     *
     * @param recipientRef the user's UUID (IN_APP recipientRef)
     */
    @PostMapping("/delivery/read-all")
    public ResponseEntity<Integer> markAllAsRead(@RequestParam String recipientRef) {
        return ResponseEntity.ok(notificationService.markAllAsRead(recipientRef));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Convenience endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/global")
    public ResponseEntity<NotificationResult> notifyGlobal(@RequestParam String subject,
                                                            @RequestParam String body) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.INFO).targetScope(TargetType.GLOBAL)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP).build()))
                .build()));
    }

    @PostMapping("/user")
    public ResponseEntity<NotificationResult> notifyUser(@RequestParam String userId,
                                                         @RequestParam String subject,
                                                         @RequestParam String body) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.INFO).targetScope(TargetType.USER).targetId(userId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .recipientIds(List.of(userId)).build()))
                .build()));
    }

    @PostMapping("/org/{orgId}")
    public ResponseEntity<NotificationResult> notifyOrg(@PathVariable String orgId,
                                                        @RequestParam String subject,
                                                        @RequestParam String body) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.INFO).targetScope(TargetType.TENANT).targetId(orgId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP).build()))
                .build()));
    }

    @PostMapping("/group/{groupId}")
    public ResponseEntity<NotificationResult> notifyGroup(@PathVariable String groupId,
                                                          @RequestParam String subject,
                                                          @RequestParam String body) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.INFO).targetScope(TargetType.GROUP).targetId(groupId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP).build()))
                .build()));
    }

    @PostMapping("/users/batch")
    public ResponseEntity<NotificationResult> notifyUserList(@RequestBody List<String> userIds,
                                                             @RequestParam String subject,
                                                             @RequestParam String body) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.INFO).targetScope(TargetType.USER)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .recipientIds(userIds).build()))
                .build()));
    }

    @PostMapping("/test-send")
    public ResponseEntity<NotificationResult> testSend(@RequestParam String userId) {
        return ResponseEntity.ok(notificationService.send(NotificationRequest.builder()
                .type(NotificationType.SUCCESS).targetScope(TargetType.USER).targetId(userId)
                .subject("Test Notification").body("Hello via Socket!")
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .recipientIds(List.of(userId)).build()))
                .build()));
    }
}
