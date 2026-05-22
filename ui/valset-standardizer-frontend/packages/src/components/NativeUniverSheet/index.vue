<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import {
  FUniver,
  LocaleType,
  LogLevel,
  mergeLocales,
  Univer,
  type Plugin,
  type PluginCtor,
  type IWorkbookData,
} from "@univerjs/presets";
import {
  UniverDocsPlugin,
  UniverDocsUIPlugin,
  UniverFormulaEnginePlugin,
  UniverNetworkPlugin,
  UniverRenderEnginePlugin,
  UniverRPCMainThreadPlugin,
  UniverSheetsFormulaPlugin,
  UniverSheetsFormulaUIPlugin,
  UniverSheetsNumfmtPlugin,
  UniverSheetsNumfmtUIPlugin,
  UniverSheetsPlugin,
  UniverSheetsUIPlugin,
  UniverUIPlugin,
  type IUniverSheetsCorePresetConfig,
} from "@univerjs/preset-sheets-core";
import sheetsCoreZhCN from "@univerjs/preset-sheets-core/locales/zh-CN";
import {
  UniverSheetsFilterPlugin,
  UniverSheetsFilterUIPlugin,
} from "@univerjs/preset-sheets-filter";
import sheetsFilterZhCN from "@univerjs/preset-sheets-filter/locales/zh-CN";
import { UniverSheetsDataValidationPreset } from "@univerjs/preset-sheets-data-validation";
import sheetsDataValidationZhCN from "@univerjs/preset-sheets-data-validation/locales/zh-CN";
import "@univerjs/presets/lib/styles/preset-sheets-core.css";
import "@univerjs/preset-sheets-filter/lib/index.css";
import "@univerjs/preset-sheets-data-validation/lib/index.css";

type NativeWorkbookApi = {
  getId: () => string;
  save: () => IWorkbookData;
};

type NativeFilterRange = {
  startRow: number;
  endRow: number;
  startColumn: number;
  endColumn: number;
};

type NativeUniverApi = {
  createWorkbook: (data: Partial<IWorkbookData>) => NativeWorkbookApi;
  getActiveWorkbook: () => NativeWorkbookApi | null;
  disposeUnit: (unitId: string) => boolean;
  dispose: () => void;
  executeCommand: <P extends object = object, R = boolean>(
    id: string,
    params?: P,
  ) => Promise<R>;
};

type NativeUniverPreset = {
  plugins: unknown[];
};

type NativeUniverPlugin =
  | PluginCtor<Plugin>
  | [PluginCtor<Plugin>, ConstructorParameters<PluginCtor<Plugin>>[0]];

const props = withDefaults(
  defineProps<{
    modelValue?: Partial<IWorkbookData>;
    height?: string | number;
    readonly?: boolean;
    config?: Partial<IUniverSheetsCorePresetConfig>;
    extraPresets?: NativeUniverPreset[];
  }>(),
  {
    height: "65vh",
    readonly: false,
    config: () => ({}),
    extraPresets: () => [],
  },
);

const emit = defineEmits<{
  "workbook-created": [workbook: NativeWorkbookApi | null];
  error: [error: Error];
}>();

const containerRef = ref<HTMLElement | null>(null);
const univerInstance = ref<Univer | null>(null);
const univerAPI = ref<NativeUniverApi | null>(null);
const workbook = ref<NativeWorkbookApi | null>(null);

const containerHeight = computed(() =>
  typeof props.height === "number" ? `${props.height}px` : props.height,
);

const reportError = (error: unknown) => {
  const normalized = error instanceof Error ? error : new Error(String(error));
  emit("error", normalized);
  return normalized;
};

const applyWorkbookFilters = (data: Partial<IWorkbookData>, createdWorkbook: NativeWorkbookApi) => {
  const filterResource = data.resources?.find((resource) => resource.name === "SHEET_FILTER_PLUGIN");
  if (!filterResource?.data || !univerAPI.value) {
    return;
  }

  try {
    const filters = JSON.parse(filterResource.data) as Record<string, { ref?: NativeFilterRange }>;
    Object.entries(filters).forEach(([sheetId, filter]) => {
      if (!filter.ref || filter.ref.endRow <= filter.ref.startRow) {
        return;
      }
      univerAPI.value?.executeCommand("sheet.command.set-filter-range", {
        unitId: createdWorkbook.getId(),
        subUnitId: sheetId,
        range: filter.ref,
      }).catch((error) => {
        const text = String((error as Error)?.message ?? error ?? "");
        if (text.includes("is not registered")) {
          return;
        }
        reportError(error);
      });
    });
  } catch (error) {
    reportError(error);
  }
};

const disposeWorkbook = () => {
  const activeWorkbook = workbook.value ?? univerAPI.value?.getActiveWorkbook();
  const unitId = activeWorkbook?.getId();
  if (unitId) {
    univerAPI.value?.disposeUnit(unitId);
  }
  workbook.value = null;
};

const dispose = () => {
  try {
    disposeWorkbook();
    univerAPI.value?.dispose();
    univerInstance.value?.dispose();
  } catch (error) {
    reportError(error);
  } finally {
    univerInstance.value = null;
    univerAPI.value = null;
    workbook.value = null;
  }
};

const loadWorkbook = (data?: Partial<IWorkbookData>) => {
  if (!univerAPI.value || !data) {
    return null;
  }

  try {
    const createdWorkbook = univerAPI.value.createWorkbook(data);
    workbook.value = createdWorkbook;
    applyWorkbookFilters(data, createdWorkbook);
    emit("workbook-created", createdWorkbook);
    return createdWorkbook;
  } catch (error) {
    throw reportError(error);
  }
};

const reload = (data?: Partial<IWorkbookData>) => {
  if (!univerAPI.value) {
    return;
  }

  try {
    disposeWorkbook();
    loadWorkbook(data ?? props.modelValue);
  } catch (error) {
    reportError(error);
  }
};

const save = () => {
  try {
    return workbook.value?.save() ?? univerAPI.value?.getActiveWorkbook()?.save() ?? null;
  } catch (error) {
    reportError(error);
    return null;
  }
};

const isReadonlyEditKey = (event: KeyboardEvent) => {
  if (event.metaKey || event.ctrlKey || event.altKey) {
    return false;
  }
  if (event.key.length === 1) {
    return true;
  }
  return [
    "Backspace",
    "Delete",
    "Enter",
    "F2",
  ].includes(event.key);
};

const preventReadonlyEdit = (event: Event) => {
  if (!props.readonly) {
    return;
  }
  event.preventDefault();
  event.stopPropagation();
};

const handleReadonlyKeydown = (event: KeyboardEvent) => {
  if (props.readonly && isReadonlyEditKey(event)) {
    preventReadonlyEdit(event);
  }
};

const resolveCorePluginEntries = (): NativeUniverPlugin[] => {
  const {
    container = containerRef.value,
    workerURL,
    header,
    footer,
    toolbar,
    ribbonType,
    formulaBar,
    menu,
    contextMenu,
    disableAutoFocus,
    customFontFamily,
    docs,
    sheets,
    formula,
    disableTextFormatAlert,
    disableTextFormatMark,
  } = props.config;
  const useFormulaWorker = Boolean(workerURL);

  return [
    UniverNetworkPlugin,
    [UniverDocsPlugin, { hasScroll: docs?.hasScroll }],
    UniverRenderEnginePlugin,
    [
      UniverUIPlugin,
      {
        container,
        header,
        toolbar,
        ribbonType,
        menu,
        contextMenu,
        disableAutoFocus,
        customFontFamily,
      },
    ],
    UniverDocsUIPlugin,
    ...(workerURL ? [[UniverRPCMainThreadPlugin, { workerURL }] as NativeUniverPlugin] : []),
    [
      UniverFormulaEnginePlugin,
      {
        notExecuteFormula: useFormulaWorker,
        function: formula?.function,
      },
    ],
    [
      UniverSheetsPlugin,
      {
        notExecuteFormula: useFormulaWorker,
        onlyRegisterFormulaRelatedMutations: false,
        isRowStylePrecedeColumnStyle: sheets?.isRowStylePrecedeColumnStyle,
        autoHeightForMergedCells: sheets?.autoHeightForMergedCells,
        freezeSync: sheets?.freezeSync,
      },
    ],
    [
      UniverSheetsUIPlugin,
      {
        formulaBar,
        footer,
        maxAutoHeightCount: sheets?.maxAutoHeightCount,
        clipboardConfig: sheets?.clipboardConfig,
        scrollConfig: sheets?.scrollConfig,
        protectedRangeShadow: sheets?.protectedRangeShadow ?? true,
        protectedRangeUserSelector: sheets?.protectedRangeUserSelector,
        disableForceStringAlert: sheets?.disableForceStringAlert,
        disableForceStringMark: sheets?.disableForceStringMark,
      },
    ],
    [
      UniverSheetsNumfmtPlugin,
      {
        disableTextFormatAlert,
        disableTextFormatMark,
      },
    ],
    UniverSheetsNumfmtUIPlugin,
    [
      UniverSheetsFormulaPlugin,
      {
        notExecuteFormula: useFormulaWorker,
        description: formula?.description,
        initialFormulaComputing: formula?.initialFormulaComputing,
      },
    ],
    [
      UniverSheetsFormulaUIPlugin,
      {
        functionScreenTips: formula?.functionScreenTips,
      },
    ],
  ];
};

const registerPluginEntry = (univer: Univer, entry: unknown) => {
  if (!entry) {
    return;
  }

  const [plugin, options] = Array.isArray(entry)
    ? [entry[0], entry[1]]
    : [entry, undefined];
  if (!plugin) {
    return;
  }
  univer.registerPlugin(plugin as PluginCtor<Plugin>, options);
};

const registerNativePlugins = (univer: Univer) => {
  [
    ...resolveCorePluginEntries(),
    [UniverSheetsFilterPlugin, {}],
    UniverSheetsFilterUIPlugin,
    ...UniverSheetsDataValidationPreset({
      showEditOnDropdown: false,
      showSearchOnDropdown: true,
    }).plugins,
    ...props.extraPresets.flatMap((preset) => preset.plugins ?? []),
  ].forEach((entry) => registerPluginEntry(univer, entry));
};

const initUniver = () => {
  if (!containerRef.value || univerAPI.value) {
    return;
  }

  try {
    const univer = new Univer({
      logLevel: LogLevel.WARN,
      locale: LocaleType.ZH_CN,
      locales: {
        [LocaleType.ZH_CN]: mergeLocales(
          sheetsCoreZhCN,
          sheetsFilterZhCN,
          sheetsDataValidationZhCN,
        ),
      },
    });
    registerNativePlugins(univer);

    const api = FUniver.newAPI(univer) as unknown as NativeUniverApi;
    univerInstance.value = univer;
    univerAPI.value = api;
    loadWorkbook(props.modelValue);
  } catch (error) {
    throw reportError(error);
  }
};

watch(
  containerRef,
  (container) => {
    if (container) {
      initUniver();
    }
  },
  { immediate: true },
);

watch(
  () => props.modelValue,
  (data) => {
    if (data && univerAPI.value) {
      reload(data);
    }
  },
);

onBeforeUnmount(() => {
  dispose();
});

defineExpose({
  getUniverAPI: () => univerAPI.value,
  getWorkbook: () => workbook.value ?? univerAPI.value?.getActiveWorkbook() ?? null,
  save,
  reload,
  dispose,
});
</script>

<template>
  <div
    ref="containerRef"
    class="native-univer-sheet"
    :style="{ height: containerHeight }"
    @keydown.capture="handleReadonlyKeydown"
    @beforeinput.capture="preventReadonlyEdit"
    @paste.capture="preventReadonlyEdit"
    @drop.capture="preventReadonlyEdit"
    @dblclick.capture="preventReadonlyEdit"
    @contextmenu.capture="preventReadonlyEdit"
  />
</template>

<style scoped>
.native-univer-sheet {
  width: 100%;
  min-height: 560px;
}
</style>
