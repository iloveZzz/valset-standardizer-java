import type { ISchema } from "@yss-ui/components";

const normalizeXKey = (key: string) => {
  if (!key.startsWith("x") || key.startsWith("x-")) {
    return key;
  }

  const body = key.slice(1);
  const kebab = body.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();
  return `x-${kebab}`;
};

export const normalizeFormilySchema = (value: unknown): ISchema | null => {
  if (!value || typeof value !== "object") {
    return null;
  }

  if (Array.isArray(value)) {
    return value.map((item) => normalizeFormilySchema(item)).filter(Boolean) as unknown as ISchema;
  }

  const source = value as Record<string, unknown>;
  const schema: Record<string, unknown> = {};

  Object.entries(source).forEach(([key, item]) => {
    const nextKey = normalizeXKey(key);

    if (nextKey === "properties" && item && typeof item === "object" && !Array.isArray(item)) {
      const properties: Record<string, unknown> = {};
      Object.entries(item as Record<string, unknown>).forEach(([propKey, propValue]) => {
        properties[propKey] = normalizeFormilySchema(propValue) ?? propValue;
      });
      schema[nextKey] = properties;
      return;
    }

    if (nextKey === "enum" && Array.isArray(item)) {
      schema[nextKey] = item.map((enumItem) => {
        if (enumItem && typeof enumItem === "object") {
          return { ...(enumItem as Record<string, unknown>) };
        }
        return enumItem;
      });
      return;
    }

    if (item && typeof item === "object") {
      schema[nextKey] = normalizeFormilySchema(item) ?? item;
      return;
    }

    schema[nextKey] = item;
  });

  return schema as ISchema;
};

export const normalizeFormilyValues = (value: unknown): Record<string, any> => {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }

  return { ...(value as Record<string, any>) };
};

const isPlainObject = (value: unknown): value is Record<string, any> => {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
};

const mergeValidator = (
  schemaNode: Record<string, any>,
  validatorName: string,
  message: string,
) => {
  const existing = Array.isArray(schemaNode["x-validator"])
    ? schemaNode["x-validator"]
    : [];
  const marker = `{{ ${validatorName} }}`;

  if (
    existing.some(
      (item) =>
        isPlainObject(item) &&
        typeof item.validator === "string" &&
        item.validator.trim() === marker,
    )
  ) {
    return;
  }

  schemaNode["x-validator"] = [
    ...existing,
    {
      validator: marker,
      message,
      triggerType: "onBlur",
    },
  ];
};

const walkAndInjectValidator = (
  node: unknown,
  fieldName: string,
  validatorName: string,
  message: string,
) => {
  if (!isPlainObject(node)) {
    return;
  }

  Object.entries(node).forEach(([key, value]) => {
    if (!isPlainObject(value)) {
      if (key === "properties" && isPlainObject(value)) {
        walkAndInjectValidator(value, fieldName, validatorName, message);
      }
      return;
    }

    if (key === fieldName) {
      mergeValidator(value, validatorName, message);
    }

    walkAndInjectValidator(value, fieldName, validatorName, message);
  });
};

export const injectFormilyAsyncValidator = (
  schema: ISchema | null,
  fieldName: string,
  validatorName: string,
  message: string,
): ISchema | null => {
  if (!schema) {
    return null;
  }

  const cloned = JSON.parse(JSON.stringify(schema)) as ISchema;
  walkAndInjectValidator(cloned, fieldName, validatorName, message);
  return cloned;
};
