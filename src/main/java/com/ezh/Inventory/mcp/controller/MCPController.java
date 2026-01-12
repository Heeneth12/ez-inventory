package com.ezh.Inventory.mcp.controller;

import com.ezh.Inventory.mcp.dto.ChatConversationDto;
import com.ezh.Inventory.mcp.dto.ChatMessageDto;
import com.ezh.Inventory.mcp.dto.ChatRequest;
import com.ezh.Inventory.mcp.service.MCPService;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/mcp/chat")
@Slf4j
@RequiredArgsConstructor
public class MCPController {

    private final MCPService mcpService;

    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<ChatMessageDto> sendMessage(@RequestBody ChatRequest request,
                                                        HttpServletRequest httpRequest) throws CommonException {
        String authToken = httpRequest.getHeader("Authorization");
        ChatMessageDto response = mcpService.processUserMessage(request.getMessage(), request.getConversationId(), authToken);
        return ResponseResource.success(HttpStatus.OK, response, "AI Response received");
    }

    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<ChatConversationDto>> getHistory() throws CommonException {
        log.info("Entering getHistory");
        List<ChatConversationDto> response = mcpService.getUserConversations();
        return ResponseResource.success(HttpStatus.OK, response, "Chat history fetched successfully");
    }

    @GetMapping(value = "/{conversationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<ChatMessageDto>> getMessages(@PathVariable Long conversationId) throws CommonException {
        log.info("Entering getMessages for conversation: {}", conversationId);
        List<ChatMessageDto> response = mcpService.getConversationMessages(conversationId);
        return ResponseResource.success(HttpStatus.OK, response, "Messages fetched successfully");
    }
}