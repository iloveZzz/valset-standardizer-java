package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.application.dto.TransferFormTemplateViewDTO;
import com.yss.valset.transfer.application.dto.TransferFormTemplateGroupDTO;
import com.yss.valset.transfer.application.service.TransferFormTemplateQueryService;
import com.yss.valset.transfer.domain.form.FormTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 默认 Transfer 表单模板查询服务。
 */
@Service
public class DefaultTransferFormTemplateQueryService implements TransferFormTemplateQueryService {

    private final ApplicationContext applicationContext;

    public DefaultTransferFormTemplateQueryService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<TransferFormTemplateViewDTO> listTemplates() {
        return applicationContext.getBeansOfType(FormTemplate.class)
                .values()
                .stream()
                .sorted(Comparator.comparing(FormTemplate::getCategory).thenComparing(FormTemplate::getName))
                .map(this::toView)
                .toList();
    }

    @Override
    public List<TransferFormTemplateGroupDTO> listGroupedTemplates() {
        Map<String, List<TransferFormTemplateViewDTO>> grouped = new LinkedHashMap<>();
        for (TransferFormTemplateViewDTO template : listTemplates()) {
            grouped.computeIfAbsent(template.getCategory(), key -> new java.util.ArrayList<>()).add(template);
        }
        return grouped.entrySet().stream()
                .map(entry -> TransferFormTemplateGroupDTO.builder()
                        .category(entry.getKey())
                        .categoryName(categoryNameOf(entry.getKey()))
                        .templates(entry.getValue())
                        .build())
                .toList();
    }

    @Override
    public TransferFormTemplateViewDTO getTemplate(String name) {
        return applicationContext.getBeansOfType(FormTemplate.class)
                .values()
                .stream()
                .filter(template -> template.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(this::toView)
                .orElseThrow(() -> new IllegalStateException("未找到表单模板，name=" + name));
    }

    private TransferFormTemplateViewDTO toView(FormTemplate template) {
        return TransferFormTemplateViewDTO.builder()
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .version(template.getVersion())
                .formDefinition(template.buildForm())
                .initialValues(template.initialValues())
                .build();
    }

    private String categoryNameOf(String category) {
        if (category == null) {
            return "未分类";
        }
        return switch (category) {
            case "transfer_source" -> "来源";
            case "transfer_target" -> "目标";
            case "transfer_rule" -> "路由规则";
            case "transfer_route" -> "路由配置";
            case "storage" -> "存储";
            default -> category;
        };
    }
}
