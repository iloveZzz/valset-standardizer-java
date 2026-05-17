package com.yss.valset.parser.infrastructure.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.valset.parser.infrastructure.entity.ParseQueuePO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 待解析任务仓储。
 */
@Mapper
@Repository
public interface ParseQueueRepository extends BasePlusRepository<ParseQueuePO> {

    /**
     * 仅在队列仍处于待订阅状态时完成订阅接管。
     *
     * @return 受影响行数，1 表示接管成功，0 表示已被其他实例接管或状态已变化
     */
    int subscribeIfPending(@Param("queueId") Long queueId,
                           @Param("subscribedBy") String subscribedBy,
                           @Param("subscribedAt") LocalDateTime subscribedAt,
                           @Param("updatedAt") LocalDateTime updatedAt);
}
