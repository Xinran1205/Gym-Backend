package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.FitnessCentreDao;
import com.gym.entity.FitnessCentre;
import com.gym.service.FitnessCentreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FitnessCentreServiceImpl extends ServiceImpl<FitnessCentreDao, FitnessCentre>
        implements FitnessCentreService {
}
