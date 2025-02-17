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
        //这里很重要，我们这里获得不了session那么我们怎么获取当前用户id呢？
        //
//        metaObject.setValue("createUser", BaseContext.getCurrentId());
//        metaObject.setValue("updateUser",BaseContext.getCurrentId());
//        SecurityContextHolder.getContext()
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

        metaObject.setValue("updateTime",LocalDateTime.now());

//        metaObject.setValue("updateUser",BaseContext.getCurrentId());
        metaObject.setValue("updateUser",SecurityContextHolder.
                getContext().
                getAuthentication().getPrincipal());
    }
}

