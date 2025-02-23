package com.gym.json;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {
    /**
     * 插入操作，自动填充
     * @param metaObject
     */
    //这两个方法都是重写的父类方法，都是MP写好的方法
    @Override
    // 这个地方好像必须得所有字段都得有，不然会报错！！！！！！！！！！所以要先校验
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        log.info(metaObject.toString());
        if (metaObject.hasSetter("createTime")) {
            metaObject.setValue("createTime", LocalDateTime.now());
        }
        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }
        if (metaObject.hasSetter("recordedAt")) {
            metaObject.setValue("recordedAt", LocalDateTime.now());
        }
        if (metaObject.hasSetter("sentAt")) {
            metaObject.setValue("sentAt", LocalDateTime.now());
        }
//        在你的 JWT 示例中，当 Token 验证成功后，你创建了一个 UsernamePasswordAuthenticationToken 对象，
//        并把从 Redis 中查到的 User 对象作为 principal 传入。因此，此时调用
//        SecurityContextHolder.getContext().getAuthentication().getPrincipal()
//        返回的就是这个 User 对象，它包含了用户的详细信息（例如用户ID、角色等），供后续在业务逻辑中使用。

        // TODO 后续需要改成具体的用户，id等
        if (metaObject.hasSetter("createUser")) {
            metaObject.setValue("createUser", SecurityContextHolder.
                    getContext().
                    getAuthentication().getPrincipal());
        }
        if (metaObject.hasSetter("updateUser")) {
            metaObject.setValue("updateUser", SecurityContextHolder.
                    getContext().
                    getAuthentication().getPrincipal());
        }
    }

    /**
     * 更新操作，自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        log.info(metaObject.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);

        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }

        if (metaObject.hasSetter("updateUser")) {
            metaObject.setValue("updateUser", SecurityContextHolder.
                    getContext().
                    getAuthentication().getPrincipal());
        }
    }
}

