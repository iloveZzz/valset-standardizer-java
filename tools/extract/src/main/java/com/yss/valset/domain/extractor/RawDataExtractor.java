package com.yss.valset.domain.extractor;

import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;

/**
 * 原始数据提取器接口。
 * <p>
 * 该接口定义 ETL 中“提取”阶段的领域抽象，负责将 Excel 或 CSV 文件中的行级数据
 * 以原样方式落入 ODS 层，不做领域解析。
 * </p>
 * <p>
 * 实现类应满足以下要求：
 * <ul>
 *   <li>以流式方式读取数据，尽量降低内存占用</li>
 *   <li>按列顺序将每一行序列化为 JSON</li>
 *   <li>批量写入 ODS 表，提高写入效率</li>
 *   <li>在异常场景下返回清晰、可追踪的错误信息</li>
 * </ul>
 * </p>
 *
 * @see com.yss.valset.domain.model.DataSourceConfig
 * @see com.yss.valset.domain.model.DataSourceType
 */
public interface RawDataExtractor {
    
    /**
     * 提取数据源中的全部行并持久化到 ODS 层。
     * <p>
     * 该方法会读取配置中指定的数据源，按行序列化为 JSON，并写入
     * {@code t_ods_valuation_filedata} 表，所有行都会关联任务和文件标识。
     * </p>
     *
     * @param config 数据源配置，包含类型和路径
     * @param taskId 任务标识
     * @param fileId 文件标识
     * @return 成功提取并持久化的总行数
     * @throws com.yss.valset.domain.exception.FileAccessException 文件无法打开或读取时抛出
     * @throws IllegalArgumentException 当 taskId 或 fileId 为空时抛出
     */
    int extract(DataSourceConfig config, Long taskId, Long fileId);
    
    /**
     * 返回当前提取器支持的数据源类型。
     * <p>
     * 该方法供提取器注册表根据数据源类型进行路由派发。
     * </p>
     *
     * @return 支持的数据源类型（EXCEL 或 CSV）
     */
    DataSourceType supportedType();
}
