#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const GENERATED_DIR = "./packages/src/api/generated/valset";
const SCHEMAS_DIR = path.join(GENERATED_DIR, "schemas");
const INDEX_FILE = path.join(GENERATED_DIR, "index.ts");

function cleanupSchemas() {
  if (!fs.existsSync(SCHEMAS_DIR)) {
    console.log("Schemas directory not found:", SCHEMAS_DIR);
    return;
  }

  const files = fs.readdirSync(SCHEMAS_DIR);
  let cleanedCount = 0;

  files.forEach((file) => {
    if (!file.endsWith(".ts")) return;

    const filePath = path.join(SCHEMAS_DIR, file);
    let content = fs.readFileSync(filePath, "utf8");

    const genericPattern = /export type (\w+) = \{ \[key: string\]: unknown \};/g;

    if (genericPattern.test(content)) {
      content = content.replace(
        /export type (\w+) = \{ \[key: string\]: unknown \};/g,
        "export type $1 = Record<string, never>;"
      );

      fs.writeFileSync(filePath, content, "utf8");
      cleanedCount++;
      console.log(`Cleaned: ${file}`);
    }
  });

  console.log(`\nCleaned ${cleanedCount} schema files.`);

  if (cleanedCount > 0) {
    console.log("\nNote: These types represent empty request bodies or responses.");
    console.log("Consider updating the OpenAPI specification to include proper schema definitions.");
  }
}

function patchDownloadResponseTypes() {
  if (!fs.existsSync(INDEX_FILE)) {
    console.log("Generated API file not found:", INDEX_FILE);
    return;
  }

  let content = fs.readFileSync(INDEX_FILE, "utf8");
  let changed = false;

  const functionNamePattern = /const\s+(\w+)\s*=\s*\(/g;
  const functionNames = new Set();
  let match;

  while ((match = functionNamePattern.exec(content)) !== null) {
    const functionName = match[1];
    if (functionName.includes("download") && functionName !== "initDownload") {
      functionNames.add(functionName);
    }
  }

  functionNames.forEach((functionName) => {
    if (functionName === "downloadChunk") {
      // 下载分片也是文件下载链路，保留 blob。
    }

    const functionPattern = new RegExp(
      `(const\\s+${functionName}\\s*=\\s*\\([\\s\\S]*?return\\s+customInstance<[^>]+>\\(\\{)([\\s\\S]*?)(\\n\\s*\\}\\))`
    );

    if (!functionPattern.test(content)) {
      return;
    }

    content = content.replace(functionPattern, (fullMatch, head, body, tail) => {
      if (/responseType:\s*['"]blob['"]/.test(body)) {
        return fullMatch;
      }

      const patchedBody = body.replace(
        /(method:\s*['"][A-Z]+['"],?\n)/,
        "$1        responseType: 'blob',\n"
      );

      if (patchedBody === body) {
        return fullMatch;
      }

      changed = true;
      console.log(`[Transformer] 添加 blob 响应: ${functionName}`);
      return `${head}${patchedBody}${tail}`;
    });
  });

  if (changed) {
    fs.writeFileSync(INDEX_FILE, content, "utf8");
  }
}

cleanupSchemas();
patchDownloadResponseTypes();
