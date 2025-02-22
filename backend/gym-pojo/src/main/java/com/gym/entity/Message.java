package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("messages")
public class Message implements Serializable {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Boolean isRead = false;

    @TableField(value = "sent_at", fill = FieldFill.INSERT)
    private LocalDateTime sentAt;
}
