package com.gym.event;

import com.gym.entity.User;
import com.gym.service.TrainerProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrainerProfileEventListener {

    @Autowired
    private TrainerProfileService trainerProfileService;

    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        User user = event.getUser();
        // 判断是否 Trainer
        if (user.getRole() == User.Role.trainer) {
            trainerProfileService.createDefaultTrainerProfile(user.getUserID(), user.getName());
        }
    }
}

