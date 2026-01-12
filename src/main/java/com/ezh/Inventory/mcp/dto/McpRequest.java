package com.ezh.Inventory.mcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    private String message;
    private Long tenantId;
    private Long userId;
    private Long conversationId;
}
