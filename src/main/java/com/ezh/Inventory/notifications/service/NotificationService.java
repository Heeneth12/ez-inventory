package com.ezh.Inventory.notifications.service;

import com.ezh.Inventory.notifications.entity.Notification;
import com.ezh.Inventory.notifications.entity.NotificationType;
import com.ezh.Inventory.notifications.entity.TargetType;
import com.ezh.Inventory.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public void sendToApp(String title, String message, NotificationType type) {
        Notification note = saveNotification(title, message, type, TargetType.GLOBAL, null);
        messagingTemplate.convertAndSend("/topic/public", note);
        log.info("Sent Global Notification: {}", title);
    }

    public void sendToUser(String userId, String title, String message, NotificationType type) {
        Notification note = saveNotification(title, message, type, TargetType.USER, userId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", note);
        log.info("Sent Private Notification to: {}", userId);
    }

    public void sendToOrg(String orgId, String title, String message, NotificationType type) {
        Notification note = saveNotification(title, message, type, TargetType.TENANT, orgId);
        messagingTemplate.convertAndSend("/topic/org." + orgId, note);
        log.info("Sent Org Notification to: {}", orgId);
    }

    public void sendToGroup(String groupId, String title, String message, NotificationType type) {
        Notification note = saveNotification(title, message, type, TargetType.GROUP, groupId);
        messagingTemplate.convertAndSend("/topic/group." + groupId, note);
        log.info("Sent Group Notification to: {}", groupId);
    }

    public void sendToUserList(List<String> userIds, String title, String message, NotificationType type) {
        if (userIds == null || userIds.isEmpty()) return;
        userIds.forEach(userId -> sendToUser(userId, title, message, type));
        log.info("Sent batch notifications to {} users", userIds.size());
    }

    private Notification saveNotification(String title, String message, NotificationType type, TargetType targetType, String targetId) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        return notificationRepository.save(n);
    }
}