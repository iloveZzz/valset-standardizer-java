import { computed, onMounted, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import {
  deleteEtlWorkflowDefinition,
  getEtlWorkflowDefinition,
  listEtlWorkflowDefinitions,
  listEtlWorkflowPlatforms,
  offlineEtlWorkflowDefinition,
  onlineEtlWorkflowDefinition,
  saveEtlWorkflowDefinition,
  runEtlWorkflowDefinition,
  syncEtlWorkflowDefinition,
  validateEtlWorkflowDefinition,
  type EtlPlatformType,
  type WorkflowDefinitionDTO,
  type WorkflowEngineBindingDTO,
  type WorkflowPlatformMetadataDTO,
  type WorkflowStageDTO,
  type WorkflowSyncStatus,
} from "@/api/etlWorkflowConfig";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import type {
  EtlWorkflowConfigPage,
  EtlWorkflowDefinitionFormState,
  EtlWorkflowDefinitionRow,
  EtlWorkflowEngineBindingFormState,
  EtlWorkflowStageFormState,
} from "../types";

const PLATFORM_LABELS: Record<EtlPlatformType, string> = {
  SPRING_BATCH: "Spring Batch",
  DOLPHIN_SCHEDULER: "DolphinScheduler",
  XXL_JOB: "XXL-JOB",
};

const PLATFORM_ORDER: EtlPlatformType[] = [
  "SPRING_BATCH",
  "DOLPHIN_SCHEDULER",
  "XXL_JOB",
];

const DOLPHINSCHEDULER_SYNC_KEY = "dolphinschedulerSync";

const createStageRow = (
  stage: Partial<WorkflowStageDTO> = {},
  index = 0,
): EtlWorkflowStageFormState => ({
  __rowKey: `${Date.now()}-${Math.random().toString(16).slice(2)}-${index}`,
  stageCode: stage.stageCode ?? "",
  stageName: stage.stageName ?? "",
  stageOrder: stage.stageOrder ?? index + 1,
  description: stage.description ?? "",
  retryable: stage.retryable ?? true,
  timeoutSeconds: stage.timeoutSeconds ?? null,
});

const createBindingState = (
  binding: WorkflowEngineBindingDTO | null | undefined,
  platformType: EtlPlatformType = "SPRING_BATCH",
): EtlWorkflowEngineBindingFormState => ({
  platformType: binding?.platformType ?? platformType,
  externalWorkflowId: binding?.externalWorkflowId ?? "",
  externalProjectCode: binding?.externalProjectCode ?? "",
  externalNamespace: binding?.externalNamespace ?? "",
  externalJobGroup: binding?.externalJobGroup ?? "",
  externalJobHandler: binding?.externalJobHandler ?? "",
  configJson: binding?.configJson ?? "",
  attributesText: JSON.stringify(
    stripInternalBindingAttributes(binding?.attributes ?? {}),
    null,
    2,
  ),
});

const createFormState = (
  definition?: WorkflowDefinitionDTO | null,
): EtlWorkflowDefinitionFormState => ({
  workflowCode: definition?.workflowCode ?? "",
  workflowName: definition?.workflowName ?? "",
  workflowVersionNo: definition?.workflowVersionNo ?? 1,
  platformType: definition?.platformType ?? "SPRING_BATCH",
  description: definition?.description ?? "",
  enabled: definition?.enabled ?? true,
  stages: (definition?.stages ?? []).map((stage, index) =>
    createStageRow(stage, index),
  ),
  engineBinding: createBindingState(
    definition?.engineBinding ?? null,
    definition?.platformType ?? "SPRING_BATCH",
  ),
});

const normalizeWorkflowKey = (
  workflowCode?: string | null,
  workflowVersionNo?: number | null,
) =>
  `${String(workflowCode ?? "").trim()}@${String(workflowVersionNo ?? "").trim()}`;

const normalizePlatformType = (
  value?: EtlPlatformType | string | null,
): EtlPlatformType => {
  const nextValue = String(value ?? "")
    .trim()
    .toUpperCase() as EtlPlatformType;
  return PLATFORM_ORDER.includes(nextValue) ? nextValue : "SPRING_BATCH";
};

const parseAttributesText = (value: string) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return {};
  }

  const parsed = JSON.parse(text) as unknown;
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error("attributes must be a plain object");
  }

  return parsed as Record<string, unknown>;
};

const stripInternalBindingAttributes = (
  value: Record<string, unknown>,
): Record<string, unknown> => {
  const attributes = { ...(value ?? {}) };
  delete attributes[DOLPHINSCHEDULER_SYNC_KEY];
  return attributes;
};

const buildDefinitionPayload = (
  formState: EtlWorkflowDefinitionFormState,
): WorkflowDefinitionDTO => {
  const cleanedStages = formState.stages
    .map((stage, index) => ({
      stageCode: String(stage.stageCode ?? "").trim(),
      stageName: String(stage.stageName ?? "").trim(),
      stageOrder: Number.isFinite(Number(stage.stageOrder))
        ? Number(stage.stageOrder)
        : index + 1,
      description: String(stage.description ?? "").trim(),
      retryable: Boolean(stage.retryable),
      timeoutSeconds:
        stage.timeoutSeconds === null || stage.timeoutSeconds === undefined
          ? undefined
          : Number(stage.timeoutSeconds),
    }))
    .filter(
      (stage) => stage.stageCode.length > 0 || stage.stageName.length > 0,
    );

  const bindingAttributes = parseAttributesText(
    formState.engineBinding.attributesText,
  );

  return {
    workflowCode: String(formState.workflowCode ?? "").trim(),
    workflowName: String(formState.workflowName ?? "").trim(),
    workflowVersionNo: Number(formState.workflowVersionNo ?? 1),
    platformType: normalizePlatformType(formState.platformType),
    description: String(formState.description ?? "").trim(),
    enabled: Boolean(formState.enabled),
    stages: cleanedStages,
    engineBinding: {
      platformType: normalizePlatformType(formState.engineBinding.platformType),
      externalWorkflowId: String(
        formState.engineBinding.externalWorkflowId ?? "",
      ).trim(),
      externalProjectCode: String(
        formState.engineBinding.externalProjectCode ?? "",
      ).trim(),
      externalNamespace: String(
        formState.engineBinding.externalNamespace ?? "",
      ).trim(),
      externalJobGroup: String(
        formState.engineBinding.externalJobGroup ?? "",
      ).trim(),
      externalJobHandler: String(
        formState.engineBinding.externalJobHandler ?? "",
      ).trim(),
      configJson: String(formState.engineBinding.configJson ?? "").trim(),
      attributes: bindingAttributes,
    },
  };
};

const formatPlatformSummary = (
  row: WorkflowDefinitionDTO | null | undefined,
  metadataMap: Map<string, WorkflowPlatformMetadataDTO>,
) => {
  const platformType = normalizePlatformType(row?.platformType);
  const bindingPlatform = normalizePlatformType(
    row?.engineBinding?.platformType ?? platformType,
  );
  const metadata =
    metadataMap.get(platformType) || metadataMap.get(bindingPlatform);
  const bindingLabel =
    row?.engineBinding?.externalWorkflowId?.trim() ||
    row?.engineBinding?.externalJobHandler?.trim() ||
    row?.engineBinding?.externalProjectCode?.trim() ||
    row?.engineBinding?.externalNamespace?.trim() ||
    "未绑定";

  return `${metadata?.platformName || PLATFORM_LABELS[platformType]} · ${bindingLabel}`;
};

const formatWorkflowStatusLabel = (enabled?: boolean | null) =>
  enabled === false ? "停用" : "启用";

const formatSyncStatusLabel = (status?: WorkflowSyncStatus | null) => {
  switch (status) {
    case "SYNCED":
      return "已同步";
    case "SYNCING":
      return "同步中";
    case "FAILED":
      return "同步失败";
    case "UNSYNCED":
    default:
      return "未同步";
  }
};

const formatExternalReleaseStateLabel = (
  externalOnline?: boolean | null,
  externalReleaseState?: string | null,
) => {
  const releaseState = String(externalReleaseState ?? "")
    .trim()
    .toUpperCase();
  if (releaseState === "ONLINE" || externalOnline === true) {
    return "已上线";
  }
  if (releaseState === "OFFLINE" || externalOnline === false) {
    return "已下线";
  }
  return "未确认";
};

const formatSyncTimeLabel = (
  value?: string | null,
  fallbackVersion?: number | null,
) => {
  const text = String(value ?? "").trim();
  if (text) {
    return text
      .replace("T", " ")
      .replace(/\.\d+$/, "")
      .slice(0, 19);
  }
  return fallbackVersion == null ? "-" : `v${fallbackVersion}`;
};

const toDefinitionRow = (
  definition: WorkflowDefinitionDTO,
  metadataMap: Map<string, WorkflowPlatformMetadataDTO>,
): EtlWorkflowDefinitionRow => {
  const workflowKey = normalizeWorkflowKey(
    definition.workflowCode,
    definition.workflowVersionNo,
  );
  const platformType = normalizePlatformType(definition.platformType);
  const bindingPlatform = normalizePlatformType(
    definition.engineBinding?.platformType ?? platformType,
  );
  const metadata =
    metadataMap.get(platformType) || metadataMap.get(bindingPlatform) || null;

  return {
    ...definition,
    workflowKey,
    stageCount: definition.stages?.length ?? 0,
    bindingSummary: formatPlatformSummary(definition, metadataMap),
    platformLabel: PLATFORM_LABELS[platformType] || String(platformType),
    statusLabel: formatWorkflowStatusLabel(definition.enabled),
    syncStatus: definition.engineBinding?.syncStatus ?? null,
    syncStatusLabel: formatSyncStatusLabel(
      definition.engineBinding?.syncStatus,
    ),
    externalOnline: definition.engineBinding?.externalOnline ?? null,
    externalReleaseState:
      definition.engineBinding?.externalReleaseState ?? null,
    externalStateLabel: formatExternalReleaseStateLabel(
      definition.engineBinding?.externalOnline ?? null,
      definition.engineBinding?.externalReleaseState ?? null,
    ),
    syncTimeLabel: formatSyncTimeLabel(
      definition.engineBinding?.lastSyncedAt ?? null,
      definition.engineBinding?.remoteWorkflowVersionNo ?? null,
    ),
    syncFailureReason: definition.engineBinding?.syncFailureReason ?? null,
    supportedOperations: metadata?.supportedOperations ?? [],
  };
};

const apiDefaults: WorkflowDefinitionDTO[] = [];

export const useEtlWorkflowConfigPage = (): {
  page: EtlWorkflowConfigPage;
} => {
  const loading = ref(false);
  const saving = ref(false);
  const validating = ref(false);
  const definitionList = ref<WorkflowDefinitionDTO[]>(apiDefaults);
  const platformMetadataList = ref<WorkflowPlatformMetadataDTO[]>([]);
  const formState = reactive<EtlWorkflowDefinitionFormState>(
    createFormState(null),
  );
  const query = reactive({
    keyword: "",
    platformType: "",
    enabled: "",
  });
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 20,
    total: 0,
    remote: true,
    showSizeChanger: false,
    showQuickJumper: false,
  });

  const platformMetadataMap = computed(
    () =>
      new Map(
        platformMetadataList.value.map((item) => [
          normalizePlatformType(item.platformType),
          item,
        ]),
      ),
  );

  const definitionRows = computed<EtlWorkflowDefinitionRow[]>(() =>
    definitionList.value.map((definition) =>
      toDefinitionRow(definition, platformMetadataMap.value),
    ),
  );

  const filteredDefinitionRows = computed<EtlWorkflowDefinitionRow[]>(() => {
    const keyword = String(query.keyword ?? "")
      .trim()
      .toLowerCase();
    const platformType = normalizePlatformType(query.platformType || null);
    const platformFilterEnabled = Boolean(
      String(query.platformType ?? "").trim(),
    );
    const enabledFilter = String(query.enabled ?? "").trim();

    return definitionRows.value.filter((row) => {
      const keywordMatched =
        !keyword ||
        [
          row.workflowCode,
          row.workflowName,
          row.workflowVersionNo,
          row.description,
          row.platformLabel,
          row.bindingSummary,
          row.stageCount,
        ]
          .map((item) => String(item ?? "").toLowerCase())
          .some((item) => item.includes(keyword));
      const platformMatched =
        !platformFilterEnabled ||
        normalizePlatformType(row.platformType) === platformType;
      const enabledMatched =
        !enabledFilter ||
        (enabledFilter === "true" && row.enabled !== false) ||
        (enabledFilter === "false" && row.enabled === false);

      return keywordMatched && platformMatched && enabledMatched;
    });
  });

  const tableData = computed<EtlWorkflowDefinitionRow[]>(() => {
    const current = Math.max(Number(pagination.value.current ?? 1), 1);
    const pageSize = Math.max(Number(pagination.value.pageSize ?? 20), 1);
    const start = (current - 1) * pageSize;
    return filteredDefinitionRows.value.slice(start, start + pageSize);
  });

  const totalCount = computed(() => definitionRows.value.length);
  const filteredCount = computed(() => filteredDefinitionRows.value.length);
  const enabledCount = computed(
    () => definitionRows.value.filter((item) => item.enabled !== false).length,
  );
  const platformCount = computed(() =>
    Math.max(platformMetadataList.value.length, PLATFORM_ORDER.length),
  );
  const stageCount = computed(() =>
    definitionRows.value.reduce(
      (total, item) => total + Number(item.stageCount ?? 0),
      0,
    ),
  );

  const platformOptions = computed(() =>
    platformMetadataList.value.length > 0
      ? platformMetadataList.value.map((item) => {
          const platformType = normalizePlatformType(item.platformType);
          return {
            label: item.platformName || PLATFORM_LABELS[platformType],
            value: platformType,
            description: item.description || "",
          };
        })
      : PLATFORM_ORDER.map((platformType) => ({
          label: PLATFORM_LABELS[platformType],
          value: platformType,
          description: "",
        })),
  );

  const currentBoundaryText = computed(
    () =>
      "本页只维护通用 ETL 工作流定义、阶段和平台绑定；同步只负责把任务流和任务分组推送到调度平台，上线/下线是独立动作。",
  );

  const summaryText = computed(
    () =>
      `当前共有 ${totalCount.value} 个工作流定义，启用 ${enabledCount.value} 个，覆盖 ${platformCount.value} 种执行平台，累计 ${stageCount.value} 个阶段。新增和修改会保存完整工作流定义版本，不会直接触发任务实例执行。`,
  );

  const syncPagination = (nextTotal: number) => {
    pagination.value.total = nextTotal;
    const pageSize = Math.max(Number(pagination.value.pageSize ?? 20), 1);
    const maxCurrent = Math.max(Math.ceil(nextTotal / pageSize), 1);
    if (Number(pagination.value.current ?? 1) > maxCurrent) {
      pagination.value.current = maxCurrent;
    }
    if (nextTotal === 0) {
      pagination.value.current = 1;
    }
  };

  const syncFormState = (definition: WorkflowDefinitionDTO | null) => {
    const next = createFormState(definition);
    Object.assign(formState, next);
  };

  const loadDefinition = async (definition: WorkflowDefinitionDTO | null) => {
    if (!definition?.workflowCode || !definition.workflowVersionNo) {
      resetForm();
      return;
    }

    const workflowCode = String(definition.workflowCode).trim();
    const workflowVersionNo = Number(definition.workflowVersionNo);
    try {
      const response = await getEtlWorkflowDefinition(
        workflowCode,
        workflowVersionNo,
      );
      const detail = unwrapSingleResult(response) ?? definition;
      syncFormState(detail);
    } catch {
      syncFormState(definition);
    }
  };

  const getLatestVersionForCode = (workflowCode: string) => {
    const matched = definitionList.value.filter(
      (item) => String(item.workflowCode ?? "").trim() === workflowCode.trim(),
    );
    if (matched.length === 0) {
      return 1;
    }
    return (
      Math.max(
        ...matched.map((item) => Number(item.workflowVersionNo ?? 0) || 0),
      ) + 1
    );
  };

  const resetForm = () => {
    syncFormState(null);
  };

  const applyQuery = () => {
    pagination.value.current = 1;
    syncPagination(filteredDefinitionRows.value.length);
  };

  const resetQuery = () => {
    query.keyword = "";
    query.platformType = "";
    query.enabled = "";
    pagination.value.current = 1;
    syncPagination(filteredDefinitionRows.value.length);
  };

  const createWorkflowDraft = (row?: WorkflowDefinitionDTO | null) => {
    const source = row ?? null;
    const baseWorkflowCode = String(source?.workflowCode ?? "").trim();
    const nextVersion = baseWorkflowCode
      ? getLatestVersionForCode(baseWorkflowCode)
      : 1;
    const nextDefinition: WorkflowDefinitionDTO = {
      workflowCode: baseWorkflowCode,
      workflowName: source?.workflowName ?? "",
      workflowVersionNo: nextVersion,
      platformType: source?.platformType ?? "SPRING_BATCH",
      description: source?.description ?? "",
      enabled: true,
      stages: (source?.stages ?? []).map((stage, index) =>
        createStageRow(stage, index),
      ),
      engineBinding: source?.engineBinding
        ? createBindingState(
            source.engineBinding,
            source.platformType ?? "SPRING_BATCH",
          )
        : createBindingState(null, "SPRING_BATCH"),
    };

    syncFormState(nextDefinition);
  };

  const addStage = () => {
    formState.stages.push(createStageRow({}, formState.stages.length));
  };

  const addStageFromDraft = (
    stage: Partial<EtlWorkflowStageFormState> = {},
  ) => {
    formState.stages.push(createStageRow(stage, formState.stages.length));
  };

  const removeStage = (index: number) => {
    formState.stages.splice(index, 1);
    formState.stages.forEach((stage, stageIndex) => {
      stage.stageOrder = stageIndex + 1;
    });
  };

  const moveStage = (fromIndex: number, toIndex: number) => {
    if (fromIndex === toIndex) {
      return;
    }
    if (
      fromIndex < 0 ||
      toIndex < 0 ||
      fromIndex >= formState.stages.length ||
      toIndex >= formState.stages.length
    ) {
      return;
    }
    const [moved] = formState.stages.splice(fromIndex, 1);
    formState.stages.splice(toIndex, 0, moved);
    formState.stages.forEach((stage, stageIndex) => {
      stage.stageOrder = stageIndex + 1;
    });
  };

  const getStageIndexByKey = (rowKey: string) =>
    formState.stages.findIndex(
      (stage) => (stage as EtlWorkflowStageFormState).__rowKey === rowKey,
    );

  const moveStageByKey = (rowKey: string, direction: -1 | 1) => {
    const index = getStageIndexByKey(rowKey);
    if (index < 0) {
      return;
    }
    moveStage(index, index + direction);
  };

  const removeStageByKey = (rowKey: string) => {
    const index = getStageIndexByKey(rowKey);
    if (index < 0) {
      return;
    }
    removeStage(index);
  };

  const setBindingPlatformType = (value: EtlPlatformType) => {
    formState.platformType = value;
    formState.engineBinding.platformType = value;
  };

  const refreshPlatforms = async () => {
    try {
      const response = await listEtlWorkflowPlatforms();
      platformMetadataList.value = unwrapMultiResult(response);

      if (platformMetadataList.value.length > 0) {
        const current = normalizePlatformType(formState.platformType);
        const hasCurrent = platformMetadataMap.value.has(current);
        if (!hasCurrent) {
          setBindingPlatformType(
            normalizePlatformType(platformMetadataList.value[0].platformType),
          );
        }
      }
    } catch {
      message.error("执行平台元数据加载失败");
    }
  };

  const loadDefinitions = async () => {
    loading.value = true;
    try {
      const response = await listEtlWorkflowDefinitions();
      definitionList.value = unwrapMultiResult(response);

      if (definitionList.value.length === 0) {
        resetForm();
      } else {
        resetForm();
      }

      syncPagination(filteredDefinitionRows.value.length);
    } catch {
      definitionList.value = [];
      resetForm();
      syncPagination(0);
      message.error("工作流定义列表加载失败");
    } finally {
      loading.value = false;
    }
  };

  const runQuery = async () => {
    await Promise.allSettled([loadDefinitions(), refreshPlatforms()]);
  };

  const handlePageChange = ({
    current,
    pageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    pagination.value.current = current;
    pagination.value.pageSize = pageSize;
    syncPagination(filteredDefinitionRows.value.length);
  };

  const saveDefinition = async () => {
    let payload: WorkflowDefinitionDTO;
    try {
      payload = buildDefinitionPayload(formState);
    } catch {
      message.error("执行平台附加属性必须是合法 JSON");
      return false;
    }

    if (!payload.workflowCode) {
      message.warning("请先填写工作流编码");
      return false;
    }
    if (!payload.workflowName) {
      message.warning("请先填写工作流名称");
      return false;
    }
    if (!payload.workflowVersionNo || payload.workflowVersionNo < 1) {
      message.warning("请先填写工作流版本号");
      return false;
    }
    if (!payload.platformType) {
      message.warning("请先选择执行平台");
      return false;
    }

    saving.value = true;
    try {
      await saveEtlWorkflowDefinition(payload);
      message.success("工作流定义已保存");
      await runQuery();
      resetForm();
      return true;
    } catch {
      message.error("工作流定义保存失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const syncDefinition = async (definition?: WorkflowDefinitionDTO | null) => {
    const target = definition ?? null;
    const workflowCode = String(target?.workflowCode ?? "").trim();
    const workflowVersionNo = Number(target?.workflowVersionNo ?? 0);
    if (!workflowCode || !workflowVersionNo) {
      message.warning("请先选择要同步的工作流");
      return false;
    }

    saving.value = true;
    try {
      await syncEtlWorkflowDefinition(workflowCode, workflowVersionNo);
      message.success("工作流已同步到远端平台，未执行上线");
      await runQuery();
      resetForm();
      return true;
    } catch {
      message.error("工作流同步失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const onlineDefinition = async (
    definition?: WorkflowDefinitionDTO | null,
  ) => {
    const target = definition ?? null;
    const workflowCode = String(target?.workflowCode ?? "").trim();
    const workflowVersionNo = Number(target?.workflowVersionNo ?? 0);
    if (!workflowCode || !workflowVersionNo) {
      message.warning("请先选择要上线的工作流");
      return false;
    }

    saving.value = true;
    try {
      await onlineEtlWorkflowDefinition(workflowCode, workflowVersionNo);
      message.success("工作流已上线");
      await runQuery();
      resetForm();
      return true;
    } catch {
      message.error("工作流上线失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const offlineDefinition = async (
    definition?: WorkflowDefinitionDTO | null,
  ) => {
    const target = definition ?? null;
    const workflowCode = String(target?.workflowCode ?? "").trim();
    const workflowVersionNo = Number(target?.workflowVersionNo ?? 0);
    if (!workflowCode || !workflowVersionNo) {
      message.warning("请先选择要下线的工作流");
      return false;
    }

    saving.value = true;
    try {
      await offlineEtlWorkflowDefinition(workflowCode, workflowVersionNo);
      message.success("工作流已下线");
      await runQuery();
      resetForm();
      return true;
    } catch {
      message.error("工作流下线失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const deleteDefinition = async (
    definition?: WorkflowDefinitionDTO | null,
  ) => {
    const target = definition;
    const workflowCode = String(target?.workflowCode ?? "").trim();
    const workflowVersionNo = Number(target?.workflowVersionNo ?? 0);
    if (!workflowCode || !workflowVersionNo) {
      message.warning("请先选择要删除的工作流");
      return false;
    }

    saving.value = true;
    try {
      await deleteEtlWorkflowDefinition(workflowCode, workflowVersionNo);
      message.success("工作流定义已删除");
      await runQuery();
      resetForm();
      return true;
    } catch {
      message.error("工作流定义删除失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const validateDefinition = async () => {
    let payload: WorkflowDefinitionDTO;
    try {
      payload = buildDefinitionPayload(formState);
    } catch {
      message.error("执行平台附加属性必须是合法 JSON");
      return;
    }

    validating.value = true;
    try {
      await validateEtlWorkflowDefinition(payload);
      message.success("工作流定义校验通过");
    } catch {
      message.error("工作流定义校验失败");
    } finally {
      validating.value = false;
    }
  };

  const runDefinition = async (definition?: WorkflowDefinitionDTO | null) => {
    const target = definition ?? null;
    const workflowCode = String(target?.workflowCode ?? "").trim();
    const workflowVersionNo = Number(target?.workflowVersionNo ?? 0);
    if (!workflowCode || !workflowVersionNo) {
      message.warning("请先选择要运行的工作流");
      return false;
    }

    saving.value = true;
    try {
      await runEtlWorkflowDefinition(workflowCode, workflowVersionNo);
      message.success("工作流运行已提交");
      await runQuery();
      return true;
    } catch {
      message.error("工作流运行失败");
      return false;
    } finally {
      saving.value = false;
    }
  };

  const formatPlatformLabel = (value?: EtlPlatformType | string | null) => {
    const platformType = normalizePlatformType(value);
    return PLATFORM_LABELS[platformType] || platformType;
  };

  const getPlatformMetadata = (
    value?: EtlPlatformType | string | null,
  ): WorkflowPlatformMetadataDTO | null => {
    const platformType = normalizePlatformType(value);
    return platformMetadataMap.value.get(platformType) || null;
  };

  onMounted(() => {
    void runQuery();
  });

  watch(
    filteredDefinitionRows,
    (rows) => {
      syncPagination(rows.length);
    },
    { immediate: true },
  );

  return {
    page: {
      loading,
      saving,
      validating,
      definitionRows,
      tableData,
      platformOptions,
      platformMetadataList,
      formState,
      query,
      pagination,
      totalCount,
      filteredCount,
      enabledCount,
      platformCount,
      stageCount,
      summaryText,
      currentBoundaryText,
      runQuery,
      applyQuery,
      resetQuery,
      resetForm,
      loadDefinition,
      createWorkflowDraft,
      saveDefinition,
      syncDefinition,
      onlineDefinition,
      offlineDefinition,
      deleteDefinition,
      addStageFromDraft,
      validateDefinition,
      runDefinition,
      refreshPlatforms,
      handlePageChange,
      addStage,
      removeStage,
      moveStage,
      getStageIndexByKey,
      moveStageByKey,
      removeStageByKey,
      setBindingPlatformType,
      formatPlatformLabel,
      getPlatformMetadata,
    } as unknown as EtlWorkflowConfigPage,
  };
};
