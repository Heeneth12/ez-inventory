package com.ezh.Inventory.notifications.common.repository;

import com.ezh.Inventory.notifications.common.entity.DeliveryStatus;
import com.ezh.Inventory.notifications.common.entity.NotificationChannel;
import com.ezh.Inventory.notifications.common.entity.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    // ── User's in-app feed ────────────────────────────────────────────────────

    /** All IN_APP deliveries for a user, newest first. */
    List<NotificationDelivery> findByRecipientRefAndChannelOrderByCreatedAtDesc(
            String recipientRef, NotificationChannel channel);

    /** Paginated IN_APP feed — preferred for large inboxes. */
    Page<NotificationDelivery> findByRecipientRefAndChannelOrderByCreatedAtDesc(
            String recipientRef, NotificationChannel channel, Pageable pageable);

    /** Unread IN_APP deliveries for a user. */
    List<NotificationDelivery> findByRecipientRefAndChannelAndIsReadFalseOrderByCreatedAtDesc(
            String recipientRef, NotificationChannel channel);

    // ── Counts ────────────────────────────────────────────────────────────────

    /** Unread count for a user — drives the notification badge. */
    long countByRecipientRefAndChannelAndIsReadFalse(String recipientRef, NotificationChannel channel);

    /** Count by status for a specific notification. */
    long countByNotificationIdAndStatus(Long notificationId, DeliveryStatus status);

    // ── Per-notification analytics ────────────────────────────────────────────

    /** All deliveries for a notification (admin view). */
    List<NotificationDelivery> findByNotificationId(Long notificationId);

    /** Deliveries for a notification filtered by channel. */
    List<NotificationDelivery> findByNotificationIdAndChannel(Long notificationId, NotificationChannel channel);

    /** Deliveries for a notification filtered by status. */
    List<NotificationDelivery> findByNotificationIdAndStatus(Long notificationId, DeliveryStatus status);

    /**
     * Status breakdown for a notification: returns [status, count] pairs.
     *
     * <pre>
     * repo.countByStatusForNotification(42L)
     * → [[SENT, 48], [FAILED, 2], [READ, 10]]
     * </pre>
     */
    @Query("SELECT d.status, COUNT(d) FROM NotificationDelivery d " +
           "WHERE d.notification.id = :notificationId GROUP BY d.status")
    List<Object[]> countByStatusForNotification(@Param("notificationId") Long notificationId);

    /**
     * Full channel × status breakdown for a notification.
     * Returns [channel, status, count] triples.
     */
    @Query("SELECT d.channel, d.status, COUNT(d) FROM NotificationDelivery d " +
           "WHERE d.notification.id = :notificationId GROUP BY d.channel, d.status")
    List<Object[]> deliverySummaryForNotification(@Param("notificationId") Long notificationId);

    // ── Retry / ops ───────────────────────────────────────────────────────────

    /** FAILED deliveries for a specific channel — used by retry logic. */
    List<NotificationDelivery> findByStatusAndChannel(DeliveryStatus status, NotificationChannel channel);

    // ── Mark as read ──────────────────────────────────────────────────────────

    /**
     * Mark a single delivery as read — avoids a SELECT before UPDATE.
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationDelivery d " +
           "SET d.isRead = true, d.status = 'READ', d.readAt = :readAt " +
           "WHERE d.id = :id")
    int markAsRead(@Param("id") Long id, @Param("readAt") Date readAt);

    /**
     * Mark all unread IN_APP deliveries for a user as read in one UPDATE.
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationDelivery d " +
           "SET d.isRead = true, d.status = 'READ', d.readAt = :readAt " +
           "WHERE d.recipientRef = :recipientRef " +
           "AND d.channel = com.ezh.Inventory.notifications.common.entity.NotificationChannel.IN_APP " +
           "AND d.isRead = false")
    int markAllAsReadForUser(@Param("recipientRef") String recipientRef, @Param("readAt") Date readAt);
}
