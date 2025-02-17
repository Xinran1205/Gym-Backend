package com.gym.context;

// 可以不需要这个类，详情如下
// 如果你使用Spring Security，那么在过滤器中认证成功后，用户信息会存入SecurityContextHolder，你可以通过这个上下文在Controller中获取当前用户的信息，所以不需要另外再用ThreadLocal来保存userId。
//
//使用Spring Security的好处在于：
//
//集成性：Spring Security自带的安全上下文（SecurityContextHolder）已经能很好的管理认证信息。
//方便性：通过@AuthenticationPrincipal或SecurityContextHolder.getContext().getAuthentication()等方式，可以轻松获取当前用户信息。
//一致性：避免了多种机制并存，减少维护复杂性。
//所以你可以直接依赖Spring Security提供的机制，而不必再写一个BaseContext类来存储userId。
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * set value
     * @param id
     */
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    /**
     * get value
     * @return
     */
    public static Long getCurrentId(){
        return threadLocal.get();
    }
}

//在Spring Security中，有几种方式可以在Controller中获取当前登录用户的信息，比如用户ID。下面列举几种常见的方法：
//
//通过SecurityContextHolder手动获取
//在Controller中通过如下代码获取当前的Authentication对象，然后从中提取用户信息：
//
//Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//if (authentication != null && authentication.isAuthenticated()) {
//// 假设这里的principal是我们在过滤器中传入的User对象
//User user = (User) authentication.getPrincipal();
//Long userId = user.getId();
//// 使用userId进行后续操作
//}
//这种方法比较通用，但需要手动进行类型转换和null判断。
//

//使用@AuthenticationPrincipal注解
//Spring Security提供了一个非常便捷的注解@AuthenticationPrincipal，可以直接将当前用户的对象注入到Controller方法的参数中：
//
//@GetMapping("/example")
//public ResponseEntity<?> example(@AuthenticationPrincipal User user) {
//    Long userId = user.getId();
//    // 使用userId进行后续操作
//    return ResponseEntity.ok("User id is " + userId);
//}
//使用这种方式可以让代码更加简洁，Spring会自动从SecurityContext中获取principal并注入。
//
//注意：
//在你的过滤器中，确保在验证通过后已经将正确的用户对象（包含用户ID等信息）存放到SecurityContextHolder中，这样上述方法才能正常工作。
//
//根据你的具体需求和项目编码习惯，可以选择以上任一方法来获取当前登录用户的ID。
