package com.ezh.Inventory.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationDto {
    private Long id;
    private Long tenantId;
    private String appKey;
    private String title;
    private List<ChatMessageDto> messagesDto = new ArrayList<>();
    private String createdBy;
    private Date createdAt;
}
