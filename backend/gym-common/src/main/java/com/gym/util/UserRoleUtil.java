package com.gym.util;


import com.gym.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

public class UserRoleUtil {

    /**
     * 将自定义角色枚举转为 Security 能识别的 "ROLE_xxx"
     */
    public static List<SimpleGrantedAuthority> buildAuthorities(User.Role role) {
        // 例如: Admin -> ROLE_Admin
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
