package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.dto.TaskViewDTO;

/**
 * 用于读取任务状态和有效负载的应用程序服务。
 */
public interface TaskQueryAppService {
    /**
     * 通过id查询任务。
     */
    TaskViewDTO queryTask(Long taskId);
}
