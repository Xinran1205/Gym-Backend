package com.gym.config;


import com.gym.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableMethodSecurity   // 启用方法级别的权限注解 @PreAuthorize
// 一个http请求会经过一系列的过滤器，这些过滤器被称为过滤器链（Filter Chain）。
// 一共两个过滤器
// 先执行自定义的 JwtAuthenticationFilter 过滤器，再执行 UsernamePasswordAuthenticationFilter 过滤器
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 在 Spring 框架中，如果一个类只有一个构造函数，那么 Spring 会自动使用这个构造函数进行依赖注入，不需要再使用 @Autowired 注解。
    // 这种方式称为“隐式构造器注入”。例如，在你的 SecurityConfig 中：
    // 这里因为 SecurityConfig 只有一个构造器，Spring 自动会将上下文中类型为 JwtAuthenticationFilter 的 Bean 注入进去，
    // 所以不需要额外标注 @Autowired。这种写法不仅更简洁，也更符合 Spring Boot 以及 Spring Framework 推荐的最佳实践。
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Spring Security 过滤器链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 在 JWT 场景下，我们通常不需要 CSRF（跨站请求伪造）保护，因为 JWT 通常用于无状态认证（stateless authentication），
        // 服务端不会存储用户状态。这行代码就是关闭 Spring Security 默认的 CSRF 防护。
//        http.csrf().disable();
        http.cors().and().csrf().disable();

        // 这个代码是关闭session，因为我们使用 JWT 代替了 session，所以不需要 session。
        http
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and();
        // 其他配置


        // 2. 配置哪些接口放行（例如注册、登录、验证码接口）
        http.authorizeHttpRequests()
                .antMatchers("/user/signup", "/user/verify-code", "/user/login",
                        "/user/forgot-password", "/user/reset-password", "/user/google-login"
                // swagger
                        // http://localhost:8080/doc.html  swagger地址，后期上线可以关闭
                ,"/favicon.ico", "/swagger-resources/**", "/swagger-ui.html", "/doc.html", "/webjars/**",
                        "/v2/api-docs", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**").permitAll()
                .anyRequest().authenticated();

        // 3. 在 UsernamePasswordAuthenticationFilter 前加入自定义的 JWT 过滤器
        // 这个用户名密码的校验可以删除，因为我们使用 JWT 代替了用户名密码的校验
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 这行代码会根据前面所有的配置构建出一个 SecurityFilterChain 对象，
        // Spring Security 在启动时会加载这个过滤器链来处理所有 HTTP 请求。
        return http.build();
    }

    /**
     * 用于在登录时做认证，需要从 AuthenticationConfiguration 获取
     * 如果要在代码里调用 AuthenticationManager，可以注入这个 Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
