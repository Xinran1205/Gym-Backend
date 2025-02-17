package com.gym.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//这个类是一个 Spring Boot 的配置类，用来配置 MyBatis-Plus 的拦截器功能。具体来说：
//
//@Configuration 注解：表示这是一个配置类，会在应用启动时被 Spring 容器加载。
//@Bean 注解：表示该方法返回的对象（这里是 MybatisPlusInterceptor）会被注册为一个 Spring Bean，其他组件可以自动注入使用。
//MybatisPlusInterceptor：这是 MyBatis-Plus 的一个拦截器，可以添加多个内部拦截器来扩展功能。
//PaginationInnerInterceptor：分页拦截器，用于在执行 SQL 时自动处理分页逻辑。
//OptimisticLockerInnerInterceptor：乐观锁拦截器，用于实现乐观锁机制，防止并发更新冲突。
//运行时机：
//在 Spring Boot 应用启动时，Spring 容器会扫描到这个配置类并加载它，然后执行其中的 mpInterceptor() 方法，
// 将返回的MybatisPlusInterceptor 实例注册到容器中。这意味着在应用启动期间就已经完成了拦截器的配置，
// 并且在后续执行 MyBatis 操作时会自动生效。

//实际上，你不会在你的业务代码中直接调用或注入这个 mpInterceptor，因为它是由 MyBatis-Plus 框架自动使用的拦截器。
//它的用途是在执行 SQL 操作时，由 MyBatis-Plus 自动从 Spring 容器中获取并应用这个拦截器，从而实现分页、乐观锁等功能。

// 乐观锁需要在实体类中添加 @Version 注解

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mpInterceptor() {
        //1.定义Mp拦截器
        MybatisPlusInterceptor mpInterceptor = new MybatisPlusInterceptor();
        //2.添加分页的拦截器
        mpInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        //3.添加乐观锁拦截器
        mpInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return mpInterceptor;
    }
}
