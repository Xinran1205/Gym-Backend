package com.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.dto.TrainerConnectDecisionDTO;
import com.gym.dto.TrainerConnectRequestDTO;
import com.gym.entity.TrainerConnectRequest;
import com.gym.vo.ConnectedMemberVO;
import com.gym.vo.PendingConnectRequestVO;

import java.util.List;

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

    /**
     * 判断当前 member 是否已和指定 trainer 建立连接
     *
     * @param memberId 当前 member 的ID
     * @param trainerId 指定 trainer 的ID
     * @return true 表示已连接，false 表示未连接
     */
    boolean isConnected(Long memberId, Long trainerId);

    /**
     * 教练查询所有待审核（Pending）的连接申请
     */
    List<PendingConnectRequestVO> getPendingConnectRequestsForTrainer(Long trainerId);

    /** 获取与当前教练已建立连接 (Accepted) 的所有会员 */
    List<ConnectedMemberVO> listConnectedMembers(Long trainerId);
}

