import { computed } from "vue";
import type { YTableActionConfig } from "@yss-ui/components";

type TableActionButtons = NonNullable<YTableActionConfig["buttons"]>;

type UseTableActionConfigOptions = {
  buttons: TableActionButtons;
  width?: number;
  align?: YTableActionConfig["align"];
  fixed?: YTableActionConfig["fixed"];
  displayLimit?: number;
  moreRenderType?: YTableActionConfig["moreRenderType"];
};

export const useTableActionConfig = (
  options: UseTableActionConfigOptions,
) => {
  return computed<YTableActionConfig>(() => ({
    width: options.width ?? 180,
    align: options.align ?? "left",
    fixed: options.fixed ?? "right",
    displayLimit: options.displayLimit ?? options.buttons.length,
    moreRenderType: options.moreRenderType ?? "moreButton",
    buttons: options.buttons,
  }));
};
