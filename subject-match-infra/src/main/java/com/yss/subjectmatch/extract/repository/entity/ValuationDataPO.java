package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
@TableName("t_ods_valuation_data")
public class ValuationDataPO {
    
    @TableField("subject_code")
    private String subjectCode;

    @TableField("subject_name")
    private String subjectName;
    
    // TODO: Add other fields mapped to t_ods_valuation_data
}
