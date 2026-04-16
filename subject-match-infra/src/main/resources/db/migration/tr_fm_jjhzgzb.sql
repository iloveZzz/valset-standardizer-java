CREATE TABLE t_tr_jjhzgzb (
    id BIGINT PRIMARY KEY COMMENT '主键',
    org_cd VARCHAR(30) COMMENT '机构代码',
    pd_cd VARCHAR(30) COMMENT '产品代码',
    biz_date VARCHAR(8) COMMENT '业务日期',
    subject_cd VARCHAR(100) COMMENT '科目代码',
    subject_nm VARCHAR(300) COMMENT '科目名称',
    pa_subject_cd VARCHAR(100) COMMENT '父科目代码',
    pa_subject_nm VARCHAR(300) COMMENT '父级科目名称',
    n_hldamt DECIMAL(26, 4) COMMENT '持仓数量',
    n_hldcst DECIMAL(26, 4) COMMENT '原币持仓成本',
    n_hldcst_locl DECIMAL(26, 4) COMMENT '本币持仓成本',
    n_hldmkv DECIMAL(26, 4) COMMENT '原币持仓市值',
    n_hldmkv_locl DECIMAL(26, 4) COMMENT '本币持仓市值',
    n_hldvva DECIMAL(26, 4) COMMENT '原币证券估增',
    n_hldvva_l DECIMAL(26, 4) COMMENT '本币证券估值',
    ccy_cd VARCHAR(3) COMMENT '币种代码',
    n_valrate DECIMAL(26, 4) COMMENT '货币估值汇率',
    n_price_cost DECIMAL(26, 4) COMMENT '单位成本',
    n_valprice DECIMAL(26, 4) COMMENT '证券估值行情',
    n_cb_jz_bl DECIMAL(26, 4) COMMENT '本币成本占比',
    n_sz_jz_bl DECIMAL(26, 4) COMMENT '本币市值占比',
    n_zc_bl DECIMAL(26, 8) COMMENT '资产占比',
    susp_info VARCHAR(300) COMMENT '停牌信息',
    valuat_equity VARCHAR(30) COMMENT '估值权益',
    fin_attr_id_d VARCHAR(30) COMMENT '财务属性-投资意图',
    fin_mkt_cd VARCHAR(30) COMMENT '财务市场代码',
    time_stamp DATETIME COMMENT '时间戳',
    cons_float_tp_cd VARCHAR(30) COMMENT '受限流通类别代码',
    source_tp VARCHAR(30) COMMENT '来源类型',
    source_sign VARCHAR(300) COMMENT '来源标记',
    sn SMALLINT COMMENT '序号',
    data_dt VARCHAR(8) COMMENT '数据日期',
    is_audt TINYINT COMMENT '是否审核',
    audt_id VARCHAR(30) COMMENT '审核ID',
    isin_cd VARCHAR(30) COMMENT 'ISIN代码'
) COMMENT '基金持仓估值表';

CREATE TABLE t_tr_index (
    id BIGINT PRIMARY KEY COMMENT '主键',
    org_cd VARCHAR(30) COMMENT '机构代码',
    pd_cd VARCHAR(60) COMMENT '产品代码',
    biz_date VARCHAR(8) COMMENT '业务日期',
    indx_nm VARCHAR(300) COMMENT '指标名称',
    indx_valu VARCHAR(300) COMMENT '指标值',
    source_tp VARCHAR(30) COMMENT '来源类型',
    source_sign VARCHAR(300) COMMENT '来源标记',
    time_stamp DATETIME COMMENT '时间戳',
    is_audt TINYINT COMMENT '是否审核',
    audt_id VARCHAR(30) COMMENT '审核ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产估值指标信息（原始数据）';

CREATE INDEX idx_t_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
CREATE INDEX idx_t_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
CREATE INDEX idx_t_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
CREATE INDEX idx_t_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);
CREATE INDEX idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);
