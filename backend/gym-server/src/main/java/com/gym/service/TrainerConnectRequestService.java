package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.TrainerConnectDecisionDTO;
import com.gym.dto.TrainerConnectRequestDTO;
import com.gym.entity.TrainerConnectRequest;

public interface TrainerConnectRequestService extends IService<TrainerConnectRequest> {
    /**
     * 统计指定 member 当前待审核（Pending）状态的连接申请数量
     */
    int countPendingRequests(Long memberId);

    /**
     * 提交连接申请，将 member 与 trainer 绑定，状态为 Pending
     */
    void submitConnectRequest(TrainerConnectRequestDTO dto, Long memberId);

    void acceptConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId);

    void rejectConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId);
}

