package com.gym.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tencentcloudapi.captcha.v20190722.CaptchaClient;
import com.tencentcloudapi.captcha.v20190722.models.DescribeCaptchaResultRequest;
import com.tencentcloudapi.captcha.v20190722.models.DescribeCaptchaResultResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;

@Slf4j
@Data
@Component
public class TencentCaptchaUtil {

    @Value("${tencent.captcha.secretId}")
    private String secretId;

    @Value("${tencent.captcha.secretKey}")
    private String secretKey;

    @Value("${tencent.captcha.captchaAppId}")
    private Long captchaAppId;

    @Value("${tencent.captcha.appSecretKey}")
    private String appSecretKey;

    /**
     * 验证腾讯拼图验证码
     *
     * @param ticket  前端返回的 Ticket
     * @param randStr 前端返回的 Randstr
     * @param userIp  用户 IP
     * @return 验证通过返回 true，否则 false
     */
    public boolean verifyCaptcha(String ticket, String randStr, String userIp) {
        try {
            // 1. 实例化一个认证对象，传入腾讯云账户 SecretId 和 SecretKey
            Credential cred = new Credential(secretId, secretKey);

            // 2. 实例化一个 http 选项，可选，如果有代理或特殊需求可配置
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("captcha.tencentcloudapi.com"); // 指定接入点

            // 3. 实例化一个 client 选项，可选
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 4. 实例化一个 CaptchaClient
            CaptchaClient client = new CaptchaClient(cred, "", clientProfile);

            // 5. 实例化请求对象
            DescribeCaptchaResultRequest req = new DescribeCaptchaResultRequest();
            // 必填参数，根据官方文档设置
            req.setCaptchaType(9L);         // 9 表示滑块验证码
            req.setTicket(ticket);
            req.setUserIp("2001:630:d0:5004:d93d:61f8:30af:b45e");
            req.setRandstr(randStr);
            req.setCaptchaAppId(captchaAppId);
            req.setAppSecretKey(appSecretKey);
            // 非必填参数，如有需要可设置
            // req.setBusinessId(123L);
            // req.setSceneId(1001L);
            // ...

            // 6. 调用接口，得到返回值
            DescribeCaptchaResultResponse resp = client.DescribeCaptchaResult(req);

            // 7. 从 resp 中获取验证状态
            // 当 resp.getCaptchaCode() == 1 时表示成功
            long code = resp.getCaptchaCode();
            String msg = resp.getCaptchaMsg();
            log.info("Captcha verify result: code={}, msg={}", code, msg);

            // code == 1 表示验证通过
            return (code == 1);
        } catch (TencentCloudSDKException e) {
            // 出现异常时可记录日志
            log.error("Captcha verify exception: {}", e.toString());
            return false;
        }
    }
}
