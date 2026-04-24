#!/usr/bin/env node
// Minimal organization CLI to scaffold a new microapp from the current repo template
// Usage example:
// node scripts/create-microapp.mjs \
//   --name microapp-demo \
//   --target-dir /Users/wangyancong/Documents/Vue3 \
//   --port 8082 \
//   --active-rule /micro \
//   --api-base /api \
//   [--openapi-url http://host/openapi.json]

import fs from 'fs/promises'
import path from 'path'
import { fileURLToPath } from 'url'
import readline from 'node:readline/promises'
import { stdin as input, stdout as output } from 'node:process'
import { exec as execCb } from 'node:child_process'
import { promisify } from 'node:util'

const exec = promisify(execCb)

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

function parseArgs(argv) {
  const args = {}
  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i]
    if (arg.startsWith('--')) {
      const key = arg.slice(2)
      const next = argv[i + 1]
      if (!next || next.startsWith('--')) {
        args[key] = true
      } else {
        args[key] = next
        i++
      }
    }
  }
  return args
}

async function pathExists(p) {
  try {
    await fs.access(p)
    return true
  } catch {
    return false
  }
}

function printHelp() {
  const help = `
用法: node scripts/create-microapp.mjs [选项]

选项:
  --name <string>                微应用名称 (默认: microapp-<timestamp>)
  --target-dir <path>            目标目录 (默认: /Users/wangyancong/Documents/Vue3)
  --port <number>                本地开发端口 (默认: 8081)
  --active-rule <path>           微应用激活路由前缀 (默认: /micro)
  --api-base <path>              接口 Base 地址 (默认: /api)
  --openapi-url <url>            OpenAPI 地址 (可选)
  --interactive                  进入交互式模式 (如不带任何参数也会进入)
  --help, -h                     显示帮助

示例(参数化):
  node scripts/create-microapp.mjs \\
    --name your-app \\
    --target-dir /Users/wangyancong/Documents/Vue3 \\
    --port 8083 \\
    --active-rule /micro \\
    --api-base /api \\
    --openapi-url http://your-host/v3/api-docs

示例(交互式):
  node scripts/create-microapp.mjs --interactive
`
  console.log(help)
}

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true })
}

/**
 * Recursively copy directory with ignore rules
 */
async function copyDir(src, dest, options = {}) {
  const { ignore = [] } = options
  await ensureDir(dest)
  const entries = await fs.readdir(src, { withFileTypes: true })
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name)
    const destPath = path.join(dest, entry.name)
    const relPath = path.relative(src, srcPath)
    if (ignore.some((rule) => matchIgnore(rule, relPath))) {
      continue
    }
    if (entry.isDirectory()) {
      await copyDir(srcPath, destPath, options)
    } else if (entry.isFile()) {
      await fs.copyFile(srcPath, destPath)
    }
  }
}

function matchIgnore(rule, relPath) {
  // very small glob-like support
  if (rule.endsWith('/**')) {
    const base = rule.slice(0, -3)
    return relPath.startsWith(base)
  }
  if (rule.endsWith('/*')) {
    const base = rule.slice(0, -2)
    return relPath.startsWith(base + path.sep)
  }
  return relPath === rule
}

async function replaceInFile(filePath, replacements) {
  const exists = await pathExists(filePath)
  if (!exists) return
  const content = await fs.readFile(filePath, 'utf8')
  const next = replacements.reduce((acc, [search, replace]) => acc.replace(search, replace), content)
  if (next !== content) {
    await fs.writeFile(filePath, next, 'utf8')
  }
}

async function deletePath(targetPath) {
  try {
    await fs.rm(targetPath, { recursive: true, force: true })
  } catch {}
}

async function updateJson(filePath, updater) {
  const exists = await pathExists(filePath)
  if (!exists) return
  const text = await fs.readFile(filePath, 'utf8')
  const json = JSON.parse(text)
  const updated = await updater(json)
  await fs.writeFile(filePath, JSON.stringify(updated, null, 2) + '\n', 'utf8')
}

async function writeEnvFiles(targetRoot, _appName, apiBase, activeRule, proxyTarget) {

  // 去掉首斜杠
  const subAppName = activeRule.replace(/^\//, '')
  const devEnv = `VITE_SUB_APP_NAME=${subAppName}\nVITE_API_BASE_URL=${apiBase}\nVITE_ACTIVE_RULE=/subApp${activeRule}\nVITE_PROXY_TARGET=${proxyTarget}\n`
  const prodEnv = `VITE_SUB_APP_NAME=${subAppName}\nVITE_API_BASE_URL=${apiBase}\nVITE_ACTIVE_RULE=/subApp${activeRule}\nVITE_PROXY_TARGET=${proxyTarget}\n`
  // jsp 环境下 VITE_API_BASE_URL 固定为 mdm
  const prodJspEnv = `VITE_SUB_APP_NAME=${subAppName}\nVITE_API_BASE_URL=mdm\nVITE_ACTIVE_RULE=/subApp${activeRule}\nVITE_PROXY_TARGET=${proxyTarget}\nVITE_IS_JSP=true\n`
  const pkgRoot = path.join(targetRoot, 'packages')
  await ensureDir(pkgRoot)
  await fs.writeFile(path.join(pkgRoot, '.env.development'), devEnv, 'utf8')
  await fs.writeFile(path.join(pkgRoot, '.env.production'), prodEnv, 'utf8')
  await fs.writeFile(path.join(pkgRoot, '.env.production.jsp'), prodJspEnv, 'utf8')
}

async function writeNpmrc(targetRoot) {
  const sourceNpmrc = path.resolve(path.join(__dirname, '..', '.npmrc'))
  if (await pathExists(sourceNpmrc)) {
    const content = await fs.readFile(sourceNpmrc, 'utf8')
    await fs.writeFile(path.join(targetRoot, '.npmrc'), content, 'utf8')
  }
}

async function downloadOpenapi(openapiUrl, destFile) {
  const res = await fetch(openapiUrl)
  if (!res.ok) {
    throw new Error(`Failed to download OpenAPI: ${res.status} ${res.statusText}`)
  }
  const body = await res.text()
  await ensureDir(path.dirname(destFile))
  await fs.writeFile(destFile, body, 'utf8')
}

async function askQuestions(defaults) {
  const rl = readline.createInterface({ input, output })
  const answers = {}
  try {
    answers.appName = (await rl.question(`请输入微应用名称 [${defaults.appName}]: `)) || defaults.appName
    answers.targetDir = (await rl.question(`请输入目标目录 [${defaults.targetDir}]: `)) || defaults.targetDir
    const portStr = (await rl.question(`请输入本地端口 [${defaults.port}]: `)) || String(defaults.port)
    answers.port = Number(portStr)
    answers.activeRule = (await rl.question(`请输入激活路由前缀(如 /micro) [${defaults.activeRule}]: `)) || defaults.activeRule
    answers.apiBase = (await rl.question(`请输入接口 Base(如 /api) [${defaults.apiBase}]: `)) || defaults.apiBase
    answers.proxyTarget = (await rl.question(`请输入代理目标(如 http://127.0.0.1:3000) [${defaults.proxyTarget}]: `)) || defaults.proxyTarget
    answers.openapiUrl = (await rl.question(`OpenAPI 地址(可留空): `)) || ''

    const gitInit = (await rl.question(`是否初始化 Git 并创建/关联远程仓库? (y/N): `)).trim().toLowerCase() === 'y'
    answers.git = { enable: gitInit }
    if (gitInit) {
      const createRemote = (await rl.question(`需要自动在 GitLab 创建远程仓库吗? (Y/n): `)).trim().toLowerCase()
      answers.git.createRemote = createRemote !== 'n'
      if (answers.git.createRemote) {
        answers.git.host = (await rl.question(`GitLab 地址(如 http://gitlab.company.com): `)).trim()
        answers.git.token = (await rl.question(`GitLab Token(仅用于本次创建，不会写入仓库): `)).trim()
        answers.git.namespaceId = (await rl.question(`GitLab 组ID(namespace_id，可留空): `)).trim()
        answers.git.visibility = (await rl.question(`仓库可见性(private/internal/public) [private]: `)).trim() || 'private'
      } else {
        answers.git.remoteUrl = (await rl.question(`请输入已有远程仓库 URL: `)).trim()
      }
    }
    console.log('')
    return answers
  } finally {
    await rl.close()
  }
}

async function createGitlabProject({ host, token, name, pathName, namespaceId, visibility = 'private' }) {
  const base = host.replace(/\/$/, '')
  const url = `${base}/api/v4/projects`
  const payload = {
    name,
    path: pathName || name,
    visibility,
  }
  if (namespaceId) {
    payload.namespace_id = Number(namespaceId)
  }
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'PRIVATE-TOKEN': token },
    body: JSON.stringify(payload)
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`GitLab 创建项目失败: ${res.status} ${res.statusText} ${text}`)
  }
  const data = await res.json()
  return {
    httpUrl: data.http_url_to_repo,
    sshUrl: data.ssh_url_to_repo,
    webUrl: data.web_url,
    id: data.id,
  }
}

async function gitInitAndPush(cwd, remoteUrl) {
  try {
    await exec('git --version')
  } catch {
    console.warn('[WARN] 未检测到 git，已跳过仓库初始化。')
    return
  }
  try {
    await exec('git init', { cwd })
    await exec('git add .', { cwd })
    await exec('git commit -m "feat: init microapp"', { cwd })
    await exec('git branch -M main', { cwd })
    if (remoteUrl) {
      await exec(`git remote add origin ${remoteUrl}`, { cwd })
      try {
        await exec('git push -u origin main', { cwd })
      } catch (e) {
        console.warn(`[WARN] 首次推送失败: ${e instanceof Error ? e.message : String(e)}`)
      }
    }
  } catch (e) {
    console.warn(`[WARN] Git 初始化失败: ${e instanceof Error ? e.message : String(e)}`)
  }
}

async function main() {
  const args = parseArgs(process.argv)
  if (args.help || args.h) {
    printHelp()
    return
  }

  const defaults = {
    appName: 'microapp-' + Date.now(),
    targetDir: '/Users/wangyancong/Documents/Vue3',
    port: 8081,
    activeRule: '/micro',
    apiBase: '/api',
    proxyTarget: 'http://localhost:3000',
  }

  const interactive = !!args.interactive || (!args.name && !args['target-dir'] && !args.port && !args['active-rule'] && !args['api-base'] && !args['openapi-url'])

  let appName, targetDir, port, activeRule, apiBase, proxyTarget, openapiUrl, gitAnswers
  if (interactive) {
    const a = await askQuestions(defaults)
    appName = String(a.appName)
    targetDir = path.resolve(String(a.targetDir))
    port = Number(a.port)
    activeRule = String(a.activeRule)
    apiBase = String(a.apiBase)
    proxyTarget = String(a.proxyTarget)
    openapiUrl = a.openapiUrl ? String(a.openapiUrl) : ''
    gitAnswers = a.git
  } else {
    appName = String(args.name || defaults.appName)
    targetDir = path.resolve(String(args['target-dir'] || defaults.targetDir))
    port = Number(args.port || defaults.port)
    activeRule = String(args['active-rule'] || defaults.activeRule)
    apiBase = String(args['api-base'] || defaults.apiBase)
    proxyTarget = String(args['proxy-target'] || defaults.proxyTarget)
    openapiUrl = args['openapi-url'] ? String(args['openapi-url']) : ''
    gitAnswers = { enable: false }
  }

  const repoRoot = path.resolve(path.join(__dirname, '..'))
  const targetRoot = path.join(targetDir, appName)

  if (await pathExists(targetRoot)) {
    throw new Error(`Target directory already exists: ${targetRoot}`)
  }

  // Copy template (current repo) to target, excluding non-template dirs
  await copyDir(repoRoot, targetRoot, {
    ignore: [
      '.git',
      '.git/**',
      '.cursor/**',
      '.DS_Store',
      'node_modules/**',
      'packages/node_modules/**',
      'packages/dist/**',
      'dist/**'
    ]
  })

  // Ensure .gitlab-ci.yml exists in target (some environments may skip hidden files)
  const srcCi = path.join(repoRoot, '.gitlab-ci.yml')
  const dstCi = path.join(targetRoot, '.gitlab-ci.yml')
  if (!(await pathExists(dstCi)) && (await pathExists(srcCi))) {
    await fs.copyFile(srcCi, dstCi)
  }

  // Ensure pnpm lockfiles exist for CI with --frozen-lockfile
  const srcRootLock = path.join(repoRoot, 'pnpm-lock.yaml')
  const dstRootLock = path.join(targetRoot, 'pnpm-lock.yaml')
  if (!(await pathExists(dstRootLock)) && (await pathExists(srcRootLock))) {
    await fs.copyFile(srcRootLock, dstRootLock)
  }
  const srcPkgLock = path.join(repoRoot, 'packages', 'pnpm-lock.yaml')
  const dstPkgLock = path.join(targetRoot, 'packages', 'pnpm-lock.yaml')
  if (!(await pathExists(dstPkgLock)) && (await pathExists(srcPkgLock))) {
    await ensureDir(path.join(targetRoot, 'packages'))
    await fs.copyFile(srcPkgLock, dstPkgLock)
  }

  // Replace index.html container id with appName
  await replaceInFile(
    path.join(targetRoot, 'packages/index.html'),
    [
      [/%VITE_SUB_APP_NAME%/g, appName]
    ]
  )

  // Update vite server port and origin
  const viteConfigPath = path.join(targetRoot, 'packages/vite.config.ts')
  await replaceInFile(
    viteConfigPath,
    [
      [/port:\s*\d+/g, `port: ${port}`],
      [/origin:\s*'http:\/\/localhost:\d+'/g, `origin: 'http://localhost:${port}'`]
    ]
  )

  // Update micro-config.json
  await updateJson(path.join(targetRoot, 'micro-config.json'), (json) => {
    json.app = json.app || {}
    json.app.name = appName
    json.app.port = port
    json.app.entry = `http://localhost:${port}`
    json.app.activeRule = activeRule
    return json
  })

  // Write env files
  await writeEnvFiles(targetRoot, appName, apiBase, activeRule, proxyTarget)

  // Write .npmrc from current repo if exists
  await writeNpmrc(targetRoot)

  // Optionally download OpenAPI
  if (openapiUrl) {
    const openapiDest = path.join(targetRoot, 'openapi/openapi.json')
    try {
      await downloadOpenapi(openapiUrl, openapiDest)
    } catch (e) {
      console.warn(`[WARN] OpenAPI 下载失败，已跳过。原因: ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  // Remove scaffold UI & api in generated project
  await deletePath(path.join(targetRoot, 'packages/src/views/Scaffold'))
  await deletePath(path.join(targetRoot, 'packages/src/api/scaffold.ts'))
  // Remove scaffold route from router
  const routerPath = path.join(targetRoot, 'packages/src/router/index.ts')
  await replaceInFile(routerPath, [[/\s*,\s*\{\s*path:\s*['\"]\/scaffold['\"][\s\S]*?\}\s*,?\s*/m, '']])
  // 兜底：若未匹配到前置逗号的场景，尝试移除对象及其后逗号
  await replaceInFile(routerPath, [[/\{\s*path:\s*['\"]\/scaffold['\"][\s\S]*?\}\s*,?/m, '']])
  // 清理可能残留的 "}},]" 之类语法
  await replaceInFile(routerPath, [
    [/\}\},\s*\]/m, '}\]'],
    [/\},\s*\]/m, '}\]']
  ])
  // Remove scaffold proxy from vite config if present
  await replaceInFile(
    path.join(targetRoot, 'packages/vite.config.ts'),
    [[/,\s*['\"]\/scaffold-api['\"]:\s*\{[\s\S]*?\}\s*/m, '']]
  )

  // Update .gitlab-ci.yml image name and local deploy dir if present
  const ciFile = path.join(targetRoot, '.gitlab-ci.yml')
  const kebabApp = String(appName)
  const camelFromActive = String(activeRule || '').replace(/^\//, '') || kebabApp.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase())
  const imageName = `yss-frontend-${kebabApp}`
  const deployDir = `/opt/html/vue3/${camelFromActive}`
  await replaceInFile(ciFile, [[/IMAGE_NAME:\s*"[^"]+"/g, `IMAGE_NAME: "${imageName}"`]])
  await replaceInFile(ciFile, [[/LOCAL_DEPLOY_DIR:\s*"[^"]+"/g, `LOCAL_DEPLOY_DIR: "${deployDir}"`]])

  // Update package.json names
  await updateJson(path.join(targetRoot, 'package.json'), (json) => {
    if (json && typeof json === 'object') {
      json.name = `${appName}-workspace`
    }
    return json
  })
  await updateJson(path.join(targetRoot, 'packages/package.json'), (json) => {
    if (json && typeof json === 'object') {
      json.name = appName
    }
    return json
  })

  // Git init & remote
  if (gitAnswers && gitAnswers.enable) {
    let remoteUrl = gitAnswers.remoteUrl || ''
    if (gitAnswers.createRemote) {
      try {
        const created = await createGitlabProject({
          host: gitAnswers.host,
          token: gitAnswers.token,
          name: appName,
          pathName: appName,
          namespaceId: gitAnswers.namespaceId,
          visibility: gitAnswers.visibility || 'private',
        })
        remoteUrl = created.httpUrl
        console.log(`GitLab 项目已创建: ${created.webUrl}`)
      } catch (e) {
        console.warn(`[WARN] 远程创建失败，继续进行本地初始化。原因: ${e instanceof Error ? e.message : String(e)}`)
      }
    }
    await gitInitAndPush(targetRoot, remoteUrl)
  }

  // Done
  // Print next steps
  const steps = [
    `cd ${targetRoot}`,
    'pnpm install',
    openapiUrl ? 'pnpm generate:api' : '(optional) pnpm generate:api',
    'pnpm dev'
  ].join('\n')
  console.log(`\nCreated microapp at: ${targetRoot}\n\nNext steps:\n${steps}\n`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
