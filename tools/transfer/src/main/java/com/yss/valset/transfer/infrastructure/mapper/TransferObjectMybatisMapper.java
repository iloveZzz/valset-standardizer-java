package com.yss.valset.transfer.infrastructure.mapper;

import com.yss.valset.transfer.infrastructure.dto.MailInboxGroupDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件主对象 MyBatis Mapper。
 */
@Mapper
public interface TransferObjectMybatisMapper {

    /**
     * 查询邮件收件箱分组列表。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件ID
     * @param deliveryStatus 投递状态（DELIVERED/UNDELIVERED）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 邮件收件箱分组列表
     */
    List<MailInboxGroupDTO> loadMailInboxGroups(@Param("sourceCode") String sourceCode,
                                                 @Param("mailId") String mailId,
                                                 @Param("deliveryStatus") String deliveryStatus,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);

    /**
     * 统计邮件收件箱分组总数。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件ID
     * @param deliveryStatus 投递状态（DELIVERED/UNDELIVERED）
     * @return 分组总数
     */
    long countMailInboxGroups(@Param("sourceCode") String sourceCode,
                               @Param("mailId") String mailId,
                               @Param("deliveryStatus") String deliveryStatus);
}
