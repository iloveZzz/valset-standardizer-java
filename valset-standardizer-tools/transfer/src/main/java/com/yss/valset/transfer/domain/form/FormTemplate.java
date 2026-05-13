package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.YssFormDefinition;

import java.util.Map;

/**
 * 表单模板抽象类。
 * <p>
 * 所有场景的表单模板都基于该抽象类实现，每个模板需提供唯一名称和场景描述。
 * 模板实现类应标注 {@link org.springframework.stereotype.Component} 以便被自动发现和注册。
 * </p>
 *
 * <p>示例实现：</p>
 * <pre>
 * &#064;Component
 * public class LeaveFormTemplate extends FormTemplate {
 *     &#064;Override
 *     public String getName() {
 *         return "leave";
 *     }
 *
 *     &#064;Override
 *     public String getDescription() {
 *         return "请假审批表单模板";
 *     }
 *
 *     &#064;Override
 *     public YssFormDefinition buildForm() {
 *         return YssFormilyDsl.form()
 *             .mode(Mode.CREATE)
 *             .nodes(...)
 *             .build()
 *             ;
 *     }
 * }
 * </pre>
 */
public abstract class FormTemplate {

    /**
     * 获取模板名称（唯一标识）。
     * <p>
     * 名称用于在 API 中标识模板，建议使用小写字母和下划线，如 "leave"、"expense_claim"。
     * </p>
     *
     * @return 模板名称，不允许为 null 或空字符串
     */
    public abstract String getName();

    public abstract Map<String, Object> initialValues();

    /**
     * 获取模板的场景描述。
     * <p>
     * 描述用于在 UI 中展示模板的用途，如 "请假审批表单模板"、"报销申请表单模板"。
     * </p>
     *
     * @return 场景描述，不允许为 null 或空字符串
     */
    public abstract String getDescription();

    /**
     * 构建表单定义。
     * <p>
     * 返回的对象符合 YssFormily 表单 Schema 规范，可直接序列化为 JSON 供前端使用。
     * </p>
     *
     * @return 表单定义对象，不允许为 null
     */
    public abstract YssFormDefinition buildForm();

    /**
     * 获取模板版本号，默认为 1.0.0。
     * <p>
     * 子类可覆盖此方法以提供版本信息，用于模板升级时的兼容性处理。
     * </p>
     *
     * @return 版本号字符串
     */
    public String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取模板的分类，默认为 "default"。
     * <p>
     * 分类用于对模板进行分组管理，如 "人事审批"、"财务审批" 等。
     * </p>
     *
     * @return 分类标识
     */
    public String getCategory() {
        return "default";
    }
}
