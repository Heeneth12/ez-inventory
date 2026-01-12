package com.ezh.Inventory.mcp.service;

import com.ezh.Inventory.mcp.dto.ChatConversationDto;
import com.ezh.Inventory.mcp.dto.ChatMessageDto;
import com.ezh.Inventory.mcp.dto.McpRequest;
import com.ezh.Inventory.mcp.entity.ChatConversation;
import com.ezh.Inventory.mcp.entity.ChatMessage;
import com.ezh.Inventory.mcp.repository.ChatConversationRepository;
import com.ezh.Inventory.mcp.repository.ChatMessageRepository;
import com.ezh.Inventory.utils.UserContextUtil;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MCPServiceImpl implements MCPService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mcp.ai.url}")
    private String mcpUrl;

    @Override
    @Transactional
    public ChatMessageDto processUserMessage(String userMessage, Long conversationId, String authToken) {

        // 1. Get Context
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Long userId = UserContextUtil.getUserId();

        // 2. Retrieve or Create Conversation
        ChatConversation conversation;
        if (conversationId != null && conversationId > 0) {
            conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new CommonException("Conversation not found", HttpStatus.NOT_FOUND));
        } else {
            // Auto-create if ID is missing (First message)
            conversation = createConversationInternal(userMessage, tenantId, userId);
        }

        // 3. Save User Message to DB
        ChatMessage userMsgEntity = ChatMessage.builder()
                .conversation(conversation)
                .sender("user")
                .content(userMessage)
                .timestamp(new Date())
                .build();
        chatMessageRepository.save(userMsgEntity);

        // Update conversation "last updated" time (for sorting history)
        conversation.setUpdatedAt(new Date());
        chatConversationRepository.save(conversation);

        // 4. Prepare Data for Node.js
        McpRequest mcpRequest = McpRequest.builder()
                .message(userMessage)
                .conversationId(conversation.getId())
                .tenantId(tenantId)
                .userId(userId)
                .build();

        // 5. Call Node.js MCP Server
        String aiResponseText = callMCPServer(mcpRequest, authToken);

        // 6. Save AI Response to DB
        ChatMessage aiMsgEntity = ChatMessage.builder()
                .conversation(conversation)
                .sender("ai")
                .content(aiResponseText)
                .timestamp(new Date())
                .build();
        chatMessageRepository.save(aiMsgEntity);

        // 7. Return DTO (Frontend needs the new conversationId if it was just created)
        return ChatMessageDto.builder()
                .id(aiMsgEntity.getId())
                .conversationId(conversation.getId())
                .content(aiResponseText)
                .sender("ai")
                .timestamp(aiMsgEntity.getTimestamp())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversationDto> getUserConversations() {
        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        List<ChatConversation> conversations = chatConversationRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        return conversations.stream().map(this::buildConversationDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getConversationMessages(Long conversationId) {
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        return messages.stream().map(this::buildMessageDto).collect(Collectors.toList());
    }


    private String callMCPServer(McpRequest requestPayload, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Forward the Bearer token so Node.js can make authorized callbacks if needed
            if (token != null && !token.startsWith("Bearer ")) {
                token = "Bearer " + token;
            }
            headers.set("Authorization", token);

            HttpEntity<McpRequest> request = new HttpEntity<>(requestPayload, headers);

            // Call Node Server
            ResponseEntity<Map> response = restTemplate.postForEntity(mcpUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("reply")) {
                return (String) response.getBody().get("reply");
            }
            return "No response received from AI.";

        } catch (Exception e) {
            log.error("Error calling AI Node Server: {}", e.getMessage());
            return "I'm having trouble connecting to my brain right now. Please try again.";
        }
    }

    private ChatConversation createConversationInternal(String firstMessage, Long tenantId, Long userId) {
        String title = firstMessage.length() > 30 ? firstMessage.substring(0, 30) + "..." : firstMessage;
        ChatConversation conversation = ChatConversation.builder()
                .tenantId(tenantId)
                .title(title)
                .appKey("EZH_INV_001") // Or pass dynamically if needed
                .createdBy(String.valueOf(userId))
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        return chatConversationRepository.save(conversation);
    }

    private ChatConversationDto buildConversationDto(ChatConversation entity) {
        if (entity == null) return null;
        return ChatConversationDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .title(entity.getTitle())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ChatMessageDto buildMessageDto(ChatMessage entity) {
        if (entity == null) return null;
        return ChatMessageDto.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation().getId())
                .sender(entity.getSender())
                .content(entity.getContent())
                .timestamp(entity.getTimestamp())
                .build();
    }
}