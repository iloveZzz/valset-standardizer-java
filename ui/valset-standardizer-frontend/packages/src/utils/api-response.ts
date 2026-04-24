export const unwrapSingleResult = <T>(
  value: { data?: T } | T | undefined,
): T | undefined => {
  if (value !== null && value !== undefined && typeof value === "object") {
    if ("data" in value) {
      return (value as { data?: T }).data;
    }
  }
  return value as T | undefined;
};

export const unwrapMultiResult = <T>(
  value: { data?: T[] } | T[] | undefined,
): T[] => {
  if (Array.isArray(value)) {
    return value;
  }
  return value?.data ?? [];
};
