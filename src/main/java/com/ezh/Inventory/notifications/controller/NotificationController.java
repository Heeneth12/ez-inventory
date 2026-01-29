package com.ezh.Inventory.notifications.controller;

import com.ezh.Inventory.notifications.entity.NotificationType;
import com.ezh.Inventory.notifications.repository.NotificationRepository;
import com.ezh.Inventory.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository; // Added this!

    @PostMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }


    @PostMapping("/global")
    public void notifyApp(@RequestParam String message) {
        notificationService.sendToApp("System Alert", message, NotificationType.INFO);
    }

    @PostMapping("/user")
    public void notifyUser(@RequestParam String userId, @RequestParam String message) {
        notificationService.sendToUser(userId, "Personal Message", message, NotificationType.INFO);
    }

    @PostMapping("/org/{orgId}")
    public void notifyOrg(@PathVariable String orgId, @RequestParam String message) {
        notificationService.sendToOrg(orgId, "Organization Update", message, NotificationType.INFO);
    }

    @PostMapping("/group/{groupId}")
    public void notifyGroup(@PathVariable String groupId, @RequestParam String message) {
        notificationService.sendToGroup(groupId, "Team Announcement", message, NotificationType.INFO);
    }

    @PostMapping("/users/batch")
    public void notifyUserList(@RequestBody List<String> userIds, @RequestParam String message) {
        notificationService.sendToUserList(userIds, "Batch Alert", message, NotificationType.INFO);
    }

    // Test Endpoint
    @PostMapping("/test-send")
    public void testSend(@RequestParam String userId) {
        notificationService.sendToUser(userId, "Test Title", "Hello via Socket!", NotificationType.SUCCESS);
    }
}