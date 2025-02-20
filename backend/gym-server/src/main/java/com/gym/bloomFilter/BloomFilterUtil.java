package com.gym.bloomFilter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.gym.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import com.gym.service.UserService;

import java.util.List;

@Component
public class BloomFilterUtil {

    // 只用于用户数据（USER:xxx）缓存的防穿透
    private BloomFilter<Long> userBloomFilter;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        // 初始化布隆过滤器，参数根据实际情况设置（预期元素数量、误判率）
        userBloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000, 0.01);
        List<Long> allUserIds = userService.listObjs(
                new LambdaQueryWrapper<User>().select(User::getUserID),
                o -> Long.valueOf(o.toString())
        );
        for (Long id : allUserIds) {
            userBloomFilter.put(id);
        }
    }

    public boolean mightContainUser(Long userId) {
        return userBloomFilter.mightContain(userId);
    }

    public void addUserToBloomFilter(Long userId) {
        userBloomFilter.put(userId);
    }
}
