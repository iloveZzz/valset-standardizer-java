export const formatBytes = (value?: number) => {
  if (value === undefined || value === null || Number.isNaN(value)) return "-";

  const units = ["MB", "KB", "MB", "GB", "TB"];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
};

export const formatDateTime = (value?: string) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

export const formatDate = (value?: string) => {
  if (!value) return "-";
  const text = String(value).trim();
  if (!text) return "-";

  const directMatch = text.match(/^(\d{4})[-/]?(\d{2})[-/]?(\d{2})$/);
  if (directMatch) {
    const [, year, month, day] = directMatch;
    return `${year}-${month}-${day}`;
  }

  const chineseMatch = text.match(
    /^(\d{4})年(\d{1,2})月(\d{1,2})日?$/,
  );
  if (chineseMatch) {
    const [, year, month, day] = chineseMatch;
    return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
  }

  const date = new Date(text);
  if (Number.isNaN(date.getTime())) return text;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

export const formatCount = (value?: number) =>
  value === undefined || value === null ? "-" : String(value);
