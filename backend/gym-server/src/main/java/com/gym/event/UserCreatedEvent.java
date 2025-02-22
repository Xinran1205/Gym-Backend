package com.gym.event;

import com.gym.entity.User;
import org.springframework.context.ApplicationEvent;

// 定义事件
public class UserCreatedEvent extends ApplicationEvent {
    private final User user;
    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
    public User getUser() {
        return user;
    }
}

