package com.ezh.Inventory.utils.common;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class UserHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected @Nullable Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery(); // Gets "userId=e374..."
        String userId = null;

        if (query != null && query.contains("userId=")) {
            // Simple parsing logic
            userId = query.split("userId=")[1].split("&")[0];
        }

        if (userId == null) {
            userId = "guest-" + UUID.randomUUID().toString();
        }

        final String finalUserId = userId;
        return () -> finalUserId;
    }
}