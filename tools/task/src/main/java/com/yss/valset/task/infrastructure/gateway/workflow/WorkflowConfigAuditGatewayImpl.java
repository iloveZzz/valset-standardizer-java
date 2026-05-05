package com.yss.valset.task.infrastructure.gateway.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditRecordCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditQueryCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowConfigAuditDTO;
import com.yss.valset.task.application.port.workflow.WorkflowConfigAuditGateway;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowConfigAuditPO;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowConfigAuditRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * MyBatis 支持的工作流配置审计网关。
 */
@Component
public class WorkflowConfigAuditGatewayImpl implements WorkflowConfigAuditGateway {

    private final WorkflowConfigAuditRepository auditRepository;

    public WorkflowConfigAuditGatewayImpl(WorkflowConfigAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void record(WorkflowConfigAuditRecordCommand command) {
        if (command == null || !StringUtils.hasText(command.getWorkflowCode()) || !StringUtils.hasText(command.getActionType())) {
            return;
        }
        WorkflowConfigAuditPO po = new WorkflowConfigAuditPO();
        po.setAuditId(newId());
        po.setWorkflowId(command.getWorkflowId());
        po.setWorkflowCode(command.getWorkflowCode());
        po.setVersionNo(command.getVersionNo());
        po.setActionType(command.getActionType());
        po.setActionResult(StringUtils.hasText(command.getActionResult()) ? command.getActionResult() : "SUCCESS");
        po.setOperatorName(command.getOperatorName());
        po.setOperatorId(command.getOperatorId());
        po.setBeforeJson(command.getBeforeJson());
        po.setAfterJson(command.getAfterJson());
        po.setRemark(command.getRemark());
        po.setCreatedAt(command.getCreatedAt() == null ? LocalDateTime.now() : command.getCreatedAt());
        auditRepository.insert(po);
    }

    @Override
    public PageResult<WorkflowConfigAuditDTO> pageAudits(WorkflowConfigAuditQueryCommand query) {
        int pageIndex = normalizePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        Page<WorkflowConfigAuditPO> page = auditRepository.selectPage(
                new Page<>(pageIndex, pageSize),
                Wrappers.lambdaQuery(WorkflowConfigAuditPO.class)
                        .eq(StringUtils.hasText(query == null ? null : query.getWorkflowCode()), WorkflowConfigAuditPO::getWorkflowCode, query == null ? null : query.getWorkflowCode())
                        .eq(query != null && query.getVersionNo() != null, WorkflowConfigAuditPO::getVersionNo, query == null ? null : query.getVersionNo())
                        .eq(StringUtils.hasText(query == null ? null : query.getActionType()), WorkflowConfigAuditPO::getActionType, query == null ? null : query.getActionType())
                        .eq(StringUtils.hasText(query == null ? null : query.getActionResult()), WorkflowConfigAuditPO::getActionResult, query == null ? null : query.getActionResult())
                        .orderByDesc(WorkflowConfigAuditPO::getCreatedAt)
                        .orderByDesc(WorkflowConfigAuditPO::getAuditId)
        );
        List<WorkflowConfigAuditDTO> records = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public Optional<WorkflowConfigAuditDTO> findById(String auditId) {
        if (!StringUtils.hasText(auditId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(auditRepository.selectById(auditId)).map(this::toDTO);
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private WorkflowConfigAuditDTO toDTO(WorkflowConfigAuditPO po) {
        if (po == null) {
            return null;
        }
        WorkflowConfigAuditDTO dto = new WorkflowConfigAuditDTO();
        dto.setAuditId(po.getAuditId());
        dto.setWorkflowId(po.getWorkflowId());
        dto.setWorkflowCode(po.getWorkflowCode());
        dto.setVersionNo(po.getVersionNo());
        dto.setActionType(po.getActionType());
        dto.setActionResult(po.getActionResult());
        dto.setOperatorName(po.getOperatorName());
        dto.setOperatorId(po.getOperatorId());
        dto.setBeforeJson(po.getBeforeJson());
        dto.setAfterJson(po.getAfterJson());
        dto.setRemark(po.getRemark());
        dto.setCreatedAt(po.getCreatedAt());
        return dto;
    }

    private int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 1 ? 1 : pageIndex;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
    }
}
