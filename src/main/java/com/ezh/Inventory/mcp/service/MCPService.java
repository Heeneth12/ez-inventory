package com.ezh.Inventory.mcp.service;

import com.ezh.Inventory.mcp.dto.ChatConversationDto;
import com.ezh.Inventory.mcp.dto.ChatMessageDto;

import java.util.List;

public interface MCPService {

    ChatMessageDto processUserMessage(String userMessage, Long conversationId, String authToken);

    List<ChatConversationDto> getUserConversations();

    List<ChatMessageDto> getConversationMessages(Long conversationId);

}