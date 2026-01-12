package com.ezh.Inventory.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private String sender; // 'user' or 'ai'
    private String content;
    private Date timestamp;
}
