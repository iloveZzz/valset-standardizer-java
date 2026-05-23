<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import {
  CheckCircleOutlined,
  DeleteOutlined,
  LeftOutlined,
  ReloadOutlined,
  SaveOutlined,
  StepForwardOutlined,
} from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  YssFormily,
  type ISchema,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import { Modal } from "ant-design-vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import type {
  ProductInfoExtractionPageState,
  ProductInfoExtractionOptionRow,
  ProductInfoExtractionPreviewRow,
} from "../types";

const { page, embedded = false } = defineProps<{
  page: ProductInfoExtractionPageState;
  embedded?: boolean;
}>();

const candidateTableAreaRef = ref<HTMLDivElement>();
const previewTableAreaRef = ref<HTMLDivElement>();
const { tableHeight: candidateTableHeight } = useTableHeight(candidateTableAreaRef, {
  withPagination: true,
  withToolbar: true,
});
const { tableHeight: previewTableHeight } = useTableHeight(previewTableAreaRef, {
  withPagination: false,
  withToolbar: true,
});

let formValues = reactive({
  productType: page.query.productType,
  extractionMode: page.query.extractionMode,
  extractionStrategy: page.query.extractionStrategy,
});

watch(
  () => ({ ...formValues }),
  (values) => {
    page.query.productType = values.productType;
    page.query.extractionMode = values.extractionMode;
    page.query.extractionStrategy = values.extractionStrategy;
  },
  { deep: true },
);

const syncFormValues = () => {
  formValues.productType = page.query.productType;
  formValues.extractionMode = page.query.extractionMode;
  formValues.extractionStrategy = page.query.extractionStrategy;
};

const runQuery = () => {
  page.runQuery();
};

const resetQuery = () => {
  page.resetQuery();
  syncFormValues();
};

const goPreview = async () => {
  await page.goPreview();
};

const goCandidate = () => {
  page.goCandidate();
};

const saveRules = async () => {
  await page.saveRules();
};

const startOver = () => {
  page.startOver();
  syncFormValues();
};

const candidateFormSchema: ISchema = {
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": { layout: "horizontal", labelWidth: 90 },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          properties: {
            candidateHeader: {
              type: "void",
              title: "基本信息",
              "x-decorator": "FormItem",
              "x-decorator-props": {
                gridSpan: 3,
                feedbackLayout: "none",
                colon: false,
              },
              "x-component": "GroupHeader",
            },
            productType: {
              type: "string",
              title: "产品类型",
              enum: [
                { label: "委外产品", value: "委外产品" },
                { label: "理财产品", value: "理财产品" },
              ],
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-validator": [{ required: true, message: "请选择产品类型" }],
            },
            extractionMode: {
              type: "string",
              title: "提取模式",
              enum: [{ label: "自动识别", value: "自动识别" }],
              "x-decorator": "FormItem",
              "x-component": "Select",
            },
            extractionStrategy: {
              type: "string",
              title: "提取策略",
              enum: [
                { label: "文件名称", value: "FILE_NAME" },
                { label: "文件内容", value: "FILE_CONTENT" },
              ],
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-validator": [{ required: true, message: "请选择提取策略" }],
            },
          },
        },
      },
    },
  },
};

const previewFormSchema: ISchema = {
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": { layout: "horizontal", labelWidth: 90 },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          properties: {
            previewHeader: {
              type: "void",
              title: "基本信息",
              "x-decorator": "FormItem",
              "x-decorator-props": {
                gridSpan: 3,
                feedbackLayout: "none",
                colon: false,
              },
              "x-component": "GroupHeader",
            },
            productType: {
              type: "string",
              title: "产品类型",
              enum: [
                { label: "委外产品", value: "委外产品" },
                { label: "理财产品", value: "理财产品" },
              ],
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": { disabled: true },
            },
            subjectSystem: {
              type: "string",
              title: "科目体系",
              default: "默认科目体系",
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": { disabled: true },
            },
            managerNameView: {
              type: "string",
              title: "托管人",
              default: "临时机构",
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": { disabled: true },
            },
            holdingStatus: {
              type: "string",
              title: "持仓状态",
              default: "存续",
              enum: [{ label: "存续", value: "存续" }],
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": { disabled: true },
            },
          },
        },
      },
    },
  },
};

const schema = computed<ISchema>(() =>
  page.currentStep === 1 ? previewFormSchema : candidateFormSchema,
);

const candidateColumns = computed<YTableColumn[]>(() => [
  { type: "checkbox", width: 48, align: "center", fixed: "left" },
  { field: "originalName", title: "文件名称", minWidth: 520, ellipsis: true },
  { field: "receivedAt", title: "收取时间", width: 180 },
  { field: "receiveMode", title: "收取方式", width: 140 },
  { field: "sourceCode", title: "来源编码", width: 160, ellipsis: true },
]);

const previewColumns = computed<YTableColumn[]>(() => [
  { type: "checkbox", width: 48, align: "center", fixed: "left" },
  { field: "originalName", title: "文件名称", width: 260, ellipsis: true },
  { field: "productName", title: "产品名称", width: 260, ellipsis: true },
  { field: "productCode", title: "产品代码", width: 180, ellipsis: true },
  { field: "managerName", title: "机构", width: 160, ellipsis: true },
  { field: "matchRule", title: "匹配规则", width: 260, ellipsis: true },
  { field: "effectiveFrequency", title: "时效频率", width: 110 },
  { field: "delayDays", title: "延迟天数", width: 110 },
  { field: "approvalRequired", title: "是否审批", width: 110 },
  { field: "action", title: "操作", width: 96, fixed: "right" },
]);

const resultColumns = computed<YTableColumn[]>(() => [
  { field: "originalName", title: "文件名称", minWidth: 320, ellipsis: true },
  { field: "productCode", title: "产品代码", width: 180, ellipsis: true },
  { field: "productName", title: "产品名称", width: 240, ellipsis: true },
  { field: "ruleId", title: "规则ID", width: 180, ellipsis: true },
  { field: "status", title: "状态", width: 120 },
  { field: "message", title: "处理信息", width: 240, ellipsis: true },
]);

const handleCandidateTableChange = (params: any) => {
  page.handlePageChange({
    current: params?.current ?? params?.page ?? page.pagination.current,
    pageSize: params?.pageSize ?? page.pagination.pageSize,
  });
};

const updatePreviewValue = (
  row: ProductInfoExtractionPreviewRow,
  field: keyof ProductInfoExtractionPreviewRow,
  value: unknown,
) => {
  page.syncPreviewRowValue(row, field, value);
};

const handleProductOptionSearch = (keyword: string) => {
  void page.loadProductOptions(keyword);
};

const commitProductField = (
  row: ProductInfoExtractionPreviewRow,
  field: "productName" | "productCode" | "managerName",
  value: unknown,
) => {
  page.syncPreviewRowByField(row, field, String(value ?? ""));
};

const resultItems = computed(() => page.result?.items ?? []);

const confirmDeletePreviewRow = (row: ProductInfoExtractionPreviewRow) => {
  Modal.confirm({
    title: "删除行",
    content: `确认删除 ${row.originalName || row.transferId || "当前行"} 吗？`,
    okText: "删除",
    cancelText: "取消",
    okButtonProps: {
      danger: true,
    },
    onOk: () => page.deletePreviewRow(row.transferId),
  });
};
</script>

<template>
  <div class="product-info-page" :class="{ 'product-info-page--embedded': embedded }">
    <component
      :is="embedded ? 'div' : YCard"
      class="product-info-card"
      style="width: 100%"
      :bordered="embedded ? undefined : false"
    >
      <div class="product-info-titlebar">
        <h2>提取估值表产品信息</h2>
      </div>

      <a-steps
        class="product-info-steps"
        :current="page.currentStep"
        :items="[
          { title: '未匹配文件信息', description: '选择文件信息' },
          { title: '文件提取产品信息', description: '确认识别结果' },
          { title: '保存产品信息', description: '完成配置' },
        ]"
      />

      <div v-if="page.currentStep !== 2" class="product-info-form-panel">
        <YssFormily
          :key="page.currentStep"
          size="small"
          v-model="formValues"
          :schema="schema"
          :scope="{ runQuery }"
          :grid-defaults="{ maxColumns: 3, columnGap: 24, rowGap: 10 }"
        />
      </div>

      <section v-if="page.currentStep === 0" class="product-info-section product-info-section--table">
        <div class="product-info-section-header">
          <div>
            <h3>文件列表</h3>
            <p>未匹配产品信息的估值表文件</p>
          </div>
          <div class="product-info-form-actions">
            <YButton type="primary" size="small" @click="runQuery">
              查询
            </YButton>
            <YButton size="small" @click="resetQuery">
              重置
            </YButton>
          </div>
        </div>

        <div ref="candidateTableAreaRef" class="product-info-table">
          <YTable
            :columns="candidateColumns"
            :data="page.rows"
            :loading="page.loading"
            :max-height="candidateTableHeight"
            :row-config="{ keyField: 'transferId' }"
            :checkbox-config="{ highlight: true }"
            :pageable="true"
            :autoFlexColumn="true"
            :border="false"
            v-model:pagination="page.pagination"
            :toolbar-config="{ custom: false }"
            @page-change="handleCandidateTableChange"
            @checkbox-change="page.handleCandidateSelectionChange"
            @checkbox-all="page.handleCandidateSelectionChange"
          >
            <template #toolbar-left>
              <WorkspaceTableToolbar
                title="未匹配文件信息"
                :description="`总数 ${page.pagination.total} 条，已选 ${page.selectedCandidateRows.length} 条`"
                :meta="`当前页 ${page.rows.length} 条`"
              />
            </template>
          </YTable>
        </div>
      </section>

      <section v-if="page.currentStep === 1" class="product-info-section product-info-section--table">
        <div class="product-info-section-header">
          <div>
            <h3>文件识别产品信息</h3>
            <p>产品名称、代码、机构和匹配规则可在表格中调整</p>
          </div>
        </div>

        <div ref="previewTableAreaRef" class="product-info-table">
          <YTable
            :columns="previewColumns"
            :data="page.previewRows"
            :loading="page.previewLoading"
            :max-height="previewTableHeight"
            :row-config="{ keyField: 'transferId' }"
            :checkbox-config="{ highlight: true }"
            :pageable="false"
            :autoFlexColumn="true"
            :border="false"
            :toolbar-config="{ custom: false }"
            @checkbox-change="page.handlePreviewSelectionChange"
            @checkbox-all="page.handlePreviewSelectionChange"
          >
            <template #toolbar-left>
              <WorkspaceTableToolbar
                title="文件识别产品信息"
                :description="`待保存 ${page.previewRows.length} 条，已选 ${page.selectedPreviewRows.length} 条`"
                meta="可编辑产品名称、代码、机构和匹配规则"
              />
            </template>
            <template #productName="{ row }">
              <a-auto-complete
                :value="row.productName"
                size="small"
                :options="page.productOptions.map((option: ProductInfoExtractionOptionRow) => ({ value: option.productName }))"
                @search="handleProductOptionSearch"
                @change="(value: unknown) => commitProductField(row, 'productName', value)"
                @select="(value: unknown) => commitProductField(row, 'productName', value)"
              >
                <a-input size="small" />
              </a-auto-complete>
            </template>
            <template #productCode="{ row }">
              <a-auto-complete
                :value="row.productCode"
                size="small"
                :options="page.productOptions.map((option: ProductInfoExtractionOptionRow) => ({ value: option.productCode }))"
                @search="handleProductOptionSearch"
                @change="(value: unknown) => commitProductField(row, 'productCode', value)"
                @select="(value: unknown) => commitProductField(row, 'productCode', value)"
              >
                <a-input size="small" />
              </a-auto-complete>
            </template>
            <template #managerName="{ row }">
              <a-auto-complete
                :value="row.managerName"
                size="small"
                :options="page.productOptions.map((option: ProductInfoExtractionOptionRow) => ({ value: option.managerName }))"
                @search="handleProductOptionSearch"
                @change="(value: unknown) => commitProductField(row, 'managerName', value)"
                @select="(value: unknown) => commitProductField(row, 'managerName', value)"
              >
                <a-input size="small" />
              </a-auto-complete>
            </template>
            <template #matchRule="{ row }">
              <a-input
                :value="row.matchRule"
                size="small"
                @change="updatePreviewValue(row, 'matchRule', $event.target.value)"
              />
            </template>
            <template #effectiveFrequency="{ row }">
              <a-select
                :value="row.effectiveFrequency"
                size="small"
                class="product-info-cell-control"
                @change="(value: unknown) => updatePreviewValue(row, 'effectiveFrequency', value)"
              >
                <a-select-option value="日">日</a-select-option>
                <a-select-option value="周">周</a-select-option>
                <a-select-option value="月">月</a-select-option>
              </a-select>
            </template>
            <template #delayDays="{ row }">
              <a-input-number
                :value="row.delayDays"
                size="small"
                class="product-info-cell-control"
                :min="0"
                @change="(value: unknown) => updatePreviewValue(row, 'delayDays', Number(value ?? 0))"
              />
            </template>
            <template #approvalRequired="{ row }">
              <a-checkbox
                :checked="row.approvalRequired"
                @change="updatePreviewValue(row, 'approvalRequired', $event.target.checked)"
              />
            </template>
            <template #action="{ row }">
              <a-button
                type="link"
                danger
                size="small"
                @click="confirmDeletePreviewRow(row)"
              >
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </template>
          </YTable>
        </div>
      </section>

      <section v-if="page.currentStep === 2" class="product-info-result product-info-section">
        <div class="product-info-result__summary">
          <CheckCircleOutlined />
          <div>
            <h3>保存产品信息完成</h3>
            <p>
              成功 {{ page.result?.successCount ?? 0 }} 条，
              跳过 {{ page.result?.skippedCount ?? 0 }} 条，
              失败 {{ page.result?.failedCount ?? 0 }} 条
            </p>
          </div>
          <YButton type="primary" size="small" @click="startOver">
            <template #icon><ReloadOutlined /></template>
            继续提取
          </YButton>
        </div>
        <YTable
          :columns="resultColumns"
          :data="resultItems"
          :row-config="{ keyField: 'transferId' }"
          :pageable="false"
          :autoFlexColumn="false"
          :border="false"
        >
          <template #status="{ row }">
            <a-tag :color="row.status === 'SUCCESS' ? 'green' : row.status === 'SKIPPED' ? 'orange' : 'red'">
              {{ row.status }}
            </a-tag>
          </template>
        </YTable>
      </section>

      <div class="product-info-footer">
        <div class="product-info-footer__meta">
          已选文件 {{ page.selectedCandidateRows.length }} 条，待保存产品信息 {{ page.selectedPreviewRows.length }} 条
        </div>
        <div class="product-info-footer__actions">
          <YButton
            v-if="page.currentStep === 1"
            size="small"
            @click="goCandidate"
          >
            <template #icon><LeftOutlined /></template>
            上一步
          </YButton>
          <YButton
            v-if="page.currentStep === 0"
            type="primary"
            size="small"
            :disabled="!page.canGoPreview"
            :loading="page.previewLoading"
            @click="goPreview"
          >
            <template #icon><StepForwardOutlined /></template>
            下一步
          </YButton>
          <YButton
            v-if="page.currentStep === 1"
            type="primary"
            size="small"
            :disabled="!page.canSave"
            :loading="page.saving"
            @click="saveRules"
          >
            <template #icon><SaveOutlined /></template>
            完成配置
          </YButton>
        </div>
      </div>
    </component>
  </div>
</template>
