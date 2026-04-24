import type { OptionItem } from "@yss-ui/components";
import {
  mailAttachmentTypeOptions,
  mailConditionFieldOptions,
} from "../constants/mailCondition";

export const useMailConditionBasic = () => {
  const loadFields = async (q: string) => {
    const keyword = (q || "").toLowerCase();
    return mailConditionFieldOptions.filter(
      (option) =>
        option.label.toLowerCase().includes(keyword) ||
        option.value.toLowerCase().includes(keyword),
    );
  };

  const loadValues = async (args: { q: string; field: unknown }) => {
    const fieldKey = String(args.field || "");
    if (fieldKey !== "attachmentFileType") {
      return [];
    }

    const keyword = (args.q || "").toLowerCase();
    const list: OptionItem[] = mailAttachmentTypeOptions;
    return list.filter(
      (option) =>
        option.label.toLowerCase().includes(keyword) ||
        option.value.toLowerCase().includes(keyword),
    );
  };

  return { loadFields, loadValues };
};
