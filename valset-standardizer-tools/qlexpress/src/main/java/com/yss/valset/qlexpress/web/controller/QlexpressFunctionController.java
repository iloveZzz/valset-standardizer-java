package com.yss.valset.qlexpress.web.controller;

import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionDebugCommand;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionUpsertCommand;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionDebugResultDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionMutationResponse;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionViewDTO;
import com.yss.valset.qlexpress.application.service.QlexpressFunctionManagementAppService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * QLExpress 自定义函数维护接口。
 */
@RestController
@RequestMapping("/qlexpress-functions")
@RequiredArgsConstructor
public class QlexpressFunctionController {

    private final QlexpressFunctionManagementAppService qlexpressFunctionManagementAppService;

    @GetMapping
    @Operation(summary = "分页查询 QLExpress 函数")
    public PageResult<QlexpressFunctionViewDTO> pageFunctions(@RequestParam(value = "functionCnName", required = false) String functionCnName,
                                                              @RequestParam(value = "functionName", required = false) String functionName,
                                                              @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                              @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                              @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return qlexpressFunctionManagementAppService.pageFunctions(functionCnName, functionName, enabled, pageIndex, pageSize);
    }

    @GetMapping("/{functionId}")
    @Operation(summary = "查询 QLExpress 函数详情")
    public SingleResult<QlexpressFunctionViewDTO> getFunction(@PathVariable String functionId) {
        return SingleResult.of(qlexpressFunctionManagementAppService.getFunction(functionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建 QLExpress 函数")
    public SingleResult<QlexpressFunctionMutationResponse> createFunction(@Valid @RequestBody QlexpressFunctionUpsertCommand command) {
        command.setFunctionId(null);
        return SingleResult.of(qlexpressFunctionManagementAppService.upsertFunction(command));
    }

    @PutMapping("/{functionId}")
    @Operation(summary = "更新 QLExpress 函数")
    public SingleResult<QlexpressFunctionMutationResponse> updateFunction(@PathVariable String functionId,
                                                                          @Valid @RequestBody QlexpressFunctionUpsertCommand command) {
        command.setFunctionId(functionId);
        return SingleResult.of(qlexpressFunctionManagementAppService.upsertFunction(command));
    }

    @DeleteMapping("/{functionId}")
    @Operation(summary = "删除 QLExpress 函数")
    public SingleResult<QlexpressFunctionMutationResponse> deleteFunction(@PathVariable String functionId) {
        return SingleResult.of(qlexpressFunctionManagementAppService.deleteFunction(functionId));
    }

    @PostMapping("/{functionId}/enable")
    @Operation(summary = "启用 QLExpress 函数")
    public SingleResult<QlexpressFunctionMutationResponse> enableFunction(@PathVariable String functionId) {
        return SingleResult.of(qlexpressFunctionManagementAppService.enableFunction(functionId));
    }

    @PostMapping("/{functionId}/disable")
    @Operation(summary = "停用 QLExpress 函数")
    public SingleResult<QlexpressFunctionMutationResponse> disableFunction(@PathVariable String functionId) {
        return SingleResult.of(qlexpressFunctionManagementAppService.disableFunction(functionId));
    }

    @PostMapping("/debug")
    @Operation(summary = "在线调试 QLExpress 函数")
    public SingleResult<QlexpressFunctionDebugResultDTO> debug(@RequestBody QlexpressFunctionDebugCommand command) {
        return SingleResult.of(qlexpressFunctionManagementAppService.debug(command));
    }
}
