package com.yss.valset.qlexpress.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * QLExpress 自定义函数配置持久化对象。
 */
@Data
@TableName("t_qlexpress_function")
public class QlexpressFunctionPO {

    @TableId(value = "function_id", type = IdType.ASSIGN_ID)
    private String functionId;

    @TableField("function_cn_name")
    private String functionCnName;

    @TableField("function_name")
    private String functionName;

    @TableField("remark")
    private String remark;

    @TableField("script_body")
    private String scriptBody;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("ext_info_json")
    private String extInfoJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
