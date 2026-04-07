package com.ezh.Inventory.notifications.common.service;

import com.ezh.Inventory.notifications.common.dto.NotificationDistributor;
import com.ezh.Inventory.notifications.common.dto.NotificationRequest;
import com.ezh.Inventory.notifications.common.dto.NotificationResult;
import com.ezh.Inventory.notifications.common.dto.NotificationResult.ChannelSummary;
import com.ezh.Inventory.notifications.common.entity.*;
import com.ezh.Inventory.notifications.common.repository.NotificationDeliveryRepository;
import com.ezh.Inventory.notifications.common.repository.NotificationRepository;
import com.ezh.Inventory.notifications.gmail.service.GmailService;
import com.ezh.Inventory.notifications.whatsapp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Unified notification service.
 *
 * <p>Call {@link #send(NotificationRequest)} to dispatch a notification
 * through any number of channels and any number of recipients per channel
 * in one operation.
 *
 * <p>Persistence model:
 * <ul>
 *   <li>One {@link Notification} row per send operation — the message record.</li>
 *   <li>One {@link NotificationDelivery} row per recipient × channel — the tracking record.</li>
 * </ul>
 *
 * <p>Example: 3 distributors with 2 recipients each → 1 Notification + 6 delivery rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate          messagingTemplate;
    private final NotificationRepository         notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final WhatsAppService                whatsAppService;
    private final GmailService                   gmailService;


    /**
     * Deliver a notification through all distributors in the request.
     * Each distributor owns a channel and its recipient list.
     * Channels and recipients are processed independently — a failure on
     * one does not block others.
     *
     * @param request populated {@link NotificationRequest}
     * @return {@link NotificationResult} with per-channel sent/failed counts
     */
    @Transactional
    public NotificationResult send(NotificationRequest request) {

        // 1 ── Persist the notification message record
        Notification notification = saveNotification(request);

        // 2 ── For each channel distributor, iterate recipients and deliver
        int totalSent = 0, totalFailed = 0;
        Map<NotificationChannel, ChannelSummary> summaries = new EnumMap<>(NotificationChannel.class);

        for (NotificationDistributor distributor : request.getDistributors()) {

            NotificationChannel channel = distributor.getChannel();
            List<String> recipients     = resolveRecipients(distributor, request);

            int sent = 0, failed = 0;
            List<String> failedRefs = new ArrayList<>();

            for (String recipientRef : recipients) {
                boolean ok = dispatch(channel, recipientRef, request);
                saveDelivery(notification, channel, recipientRef, ok);

                if (ok) {
                    sent++;
                } else {
                    failed++;
                    failedRefs.add(recipientRef);
                }

                log.info("channel={} | ref={} | ok={} | subject='{}'",
                        channel, recipientRef, ok, request.getSubject());
            }

            summaries.put(channel, ChannelSummary.builder()
                    .sent(sent)
                    .failed(failed)
                    .failedRecipients(failedRefs)
                    .build());

            totalSent   += sent;
            totalFailed += failed;
        }

        return NotificationResult.builder()
                .notificationId(notification.getId())
                .totalDispatched(totalSent + totalFailed)
                .totalSent(totalSent)
                .totalFailed(totalFailed)
                .channelSummary(summaries)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Convenience helpers — build a request and call send()
    // ──────────────────────────────────────────────────────────────────────────

    /** Broadcast to all connected users (GLOBAL / IN_APP). */
    public NotificationResult sendToApp(String subject, String body, NotificationType type) {
        return send(NotificationRequest.builder()
                .type(type).targetScope(TargetType.GLOBAL)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .build()))   // empty recipientIds = topic broadcast
                .build());
    }

    /** Send IN_APP to a single user. */
    public NotificationResult sendToUser(String userId, String subject, String body, NotificationType type) {
        return send(NotificationRequest.builder()
                .type(type).targetScope(TargetType.USER).targetId(userId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .recipientIds(List.of(userId))
                        .build()))
                .build());
    }

    /** Broadcast IN_APP to a whole org. */
    public NotificationResult sendToOrg(String orgId, String subject, String body, NotificationType type) {
        return send(NotificationRequest.builder()
                .type(type).targetScope(TargetType.TENANT).targetId(orgId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .build()))
                .build());
    }

    /** Broadcast IN_APP to a group. */
    public NotificationResult sendToGroup(String groupId, String subject, String body, NotificationType type) {
        return send(NotificationRequest.builder()
                .type(type).targetScope(TargetType.GROUP).targetId(groupId)
                .subject(subject).body(body)
                .distributors(List.of(NotificationDistributor.builder()
                        .channel(NotificationChannel.IN_APP)
                        .build()))
                .build());
    }

    /** Send IN_APP to a list of users. One Notification per user. */
    public void sendToUserList(List<String> userIds, String subject, String body, NotificationType type) {
        if (userIds == null || userIds.isEmpty()) return;
        userIds.forEach(userId -> sendToUser(userId, subject, body, type));
        log.info("Batch dispatched to {} users", userIds.size());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read state
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public boolean markDeliveryAsRead(Long deliveryId) {
        return deliveryRepository.markAsRead(deliveryId, new Date()) > 0;
    }

    @Transactional
    public int markAllAsRead(String recipientRef) {
        return deliveryRepository.markAllAsReadForUser(recipientRef, new Date());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Channel dispatch — one recipient, one channel, returns success flag
    // ──────────────────────────────────────────────────────────────────────────

    private boolean dispatch(NotificationChannel channel, String recipientRef,
                              NotificationRequest req) {
        return switch (channel) {
            case IN_APP   -> dispatchInApp(recipientRef, req);
            case EMAIL    -> dispatchEmail(recipientRef, req);
            case WHATSAPP -> dispatchWhatsApp(recipientRef, req);
            case PUSH     -> dispatchPush(recipientRef, req);
        };
    }

    private boolean dispatchInApp(String recipientRef, NotificationRequest req) {
        try {
            // recipientRef is the userId for USER scope; null/empty for broadcasts
            switch (req.getTargetScope()) {
                case GLOBAL -> messagingTemplate.convertAndSend("/topic/public", req);
                case TENANT -> messagingTemplate.convertAndSend("/topic/org." + req.getTargetId(), req);
                case GROUP  -> messagingTemplate.convertAndSend("/topic/group." + req.getTargetId(), req);
                case USER   -> messagingTemplate.convertAndSendToUser(recipientRef, "/queue/notifications", req);
            }
            return true;
        } catch (Exception e) {
            log.error("IN_APP failed | ref={} | {}", recipientRef, e.getMessage());
            return false;
        }
    }

    private boolean dispatchEmail(String email, NotificationRequest req) {
        return gmailService.sendNotification(email, req.getSubject(), req.getBody());
    }

    private boolean dispatchWhatsApp(String phone, NotificationRequest req) {
        return whatsAppService.sendNotification(phone, req.getSubject(), req.getBody());
    }

    private boolean dispatchPush(String deviceToken, NotificationRequest req) {
        // TODO: Firebase FCM / APNs integration
        log.warn("PUSH not yet implemented | token={}", deviceToken);
        return false;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Notification saveNotification(NotificationRequest req) {
        return notificationRepository.save(Notification.builder()
                .subject(req.getSubject())
                .body(req.getBody())
                .type(req.getType() != null ? req.getType() : NotificationType.INFO)
                .targetScope(req.getTargetScope())
                .sentBy(req.getMetadata() != null ? req.getMetadata().get("sentBy") : null)
                .build());
    }

    private void saveDelivery(Notification notification, NotificationChannel channel,
                               String recipientRef, boolean sent) {
        deliveryRepository.save(NotificationDelivery.builder()
                .notification(notification)
                .recipientRef(recipientRef)
                .channel(channel)
                .status(sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED)
                .build());
    }

    /**
     * Returns the recipient list from the distributor.
     * For IN_APP broadcast scopes (GLOBAL / TENANT / GROUP) with no explicit
     * recipientIds, returns a synthetic single entry so a delivery row is still
     * created for audit purposes.
     */
    private List<String> resolveRecipients(NotificationDistributor distributor,
                                            NotificationRequest req) {
        List<String> list = distributor.recipients();

        if (!list.isEmpty()) return list;

        // IN_APP broadcast — no individual IDs, create one audit record
        if (distributor.getChannel() == NotificationChannel.IN_APP) {
            String broadcastRef = switch (req.getTargetScope()) {
                case GLOBAL -> "BROADCAST:GLOBAL";
                case TENANT -> "BROADCAST:TENANT:" + req.getTargetId();
                case GROUP  -> "BROADCAST:GROUP:"  + req.getTargetId();
                case USER   -> req.getTargetId() != null ? req.getTargetId() : "UNKNOWN";
            };
            return List.of(broadcastRef);
        }

        return List.of();
    }
}
