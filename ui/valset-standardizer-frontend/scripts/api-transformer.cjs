/**
 * Orval API 转换器
 *
 * 该脚本在代码生成之前修改 OpenAPI 规范。
 * 它专门针对包含 `application/x-www-form-urlencoded` 请求体的接口操作，
 * 如果这些请求体的 schema 为空（即没有定义任何属性），
 * 则会删除整个 requestBody 部分，从而避免生成无用的函数参数。
 *
 * @param {Object} schema - 解析后的 OpenAPI 规范对象
 * @returns {Object} - 处理后的 OpenAPI 规范对象
 */
/**
 * 提取路径模板中的参数名，例如 `/sources/{sourceId}` -> `["sourceId"]`
 *
 * @param {string} path - OpenAPI 路径模板
 * @returns {string[]} - 路径参数名列表
 */
function extractPathParamNames(path) {
  return Array.from(path.matchAll(/\{([^}]+)\}/g), (match) => match[1]);
}

/**
 * 为缺失的路径参数补齐标准的 path parameter 定义，避免 Orval 生成时报错。
 *
 * @param {Object} operation - OpenAPI operation 对象
 * @param {string} path - OpenAPI 路径模板
 */
function ensurePathParameters(operation, path) {
  const pathParamNames = extractPathParamNames(path);
  if (!pathParamNames.length) {
    return;
  }

  const parameters = Array.isArray(operation.parameters) ? operation.parameters : [];
  const existedPathParamNames = new Set(
    parameters
      .filter((parameter) => parameter && parameter.in === "path" && parameter.name)
      .map((parameter) => parameter.name),
  );

  pathParamNames.forEach((paramName) => {
    if (existedPathParamNames.has(paramName)) {
      return;
    }

    parameters.push({
      name: paramName,
      in: "path",
      required: true,
      description: `自动补齐的路径参数: ${paramName}`,
      schema: {
        type: "string",
      },
    });

    console.log(`[Transformer] 补齐路径参数: ${paramName} -> ${path}`);
  });

  operation.parameters = parameters;
}

module.exports = (schema) => {
  const paths = schema.paths || {};
  const httpMethods = new Set([
    "get",
    "post",
    "put",
    "delete",
    "patch",
    "options",
    "head",
    "trace",
  ]);

  Object.keys(paths).forEach((path) => {
    const methods = paths[path];
    Object.keys(methods).forEach((method) => {
      if (!httpMethods.has(method)) {
        return;
      }

      const operation = methods[method];

      ensurePathParameters(operation, path);

      // 检查操作是否包含 requestBody
      if (operation.requestBody && operation.requestBody.content) {
        const content = operation.requestBody.content;

        // 检查是否为 x-www-form-urlencoded 类型
        if (content["application/x-www-form-urlencoded"]) {
          const mediaType = content["application/x-www-form-urlencoded"];

          // 检查 schema 是否为空（类型为 object，但无任何属性定义）
          if (
            mediaType.schema &&
            mediaType.schema.type === "object" &&
            (!mediaType.schema.properties ||
              Object.keys(mediaType.schema.properties).length === 0)
          ) {
            console.log(`[Transformer] 移除空 requestBody: ${method.toUpperCase()} ${path}`);
            delete operation.requestBody;
          }
        }
      }
    });
  });

  return schema;
};
