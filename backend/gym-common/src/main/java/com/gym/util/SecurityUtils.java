package com.gym.util;

import com.gym.entity.User;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
public final class SecurityUtils {

    // Private constructor to prevent instantiation
    private SecurityUtils() { }

    /**
     * Retrieves the current logged-in user's ID from the SecurityContext.
     * <p>
     * This method extracts the authentication principal from the SecurityContextHolder and
     * returns the user ID if the principal is an instance of {@link com.gym.entity.User}.
     * </p>
     *
     * @return the current user ID, or {@code null} if not authenticated or an error occurs.
     */
    public static Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getUserID(); // 请确保 User 类中有 getUserID() 方法
            }
        } catch (Exception e) {
            log.error("Error retrieving current user ID: ", e);
        }
        return null;
    }
}