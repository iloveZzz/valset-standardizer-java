#!/usr/bin/env node

import { spawn, exec } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const APP = {
  name: 'microapp',
  path: 'packages',
  port: 8081,
  description: '数据中台微应用',
}

class MicroManager {
  constructor() {
    this.processes = new Map()
  }

  // 启动应用
  async startApp(mode = 'dev') {
    console.log(`🚀 启动 ${APP.description}...`)

    const command = mode === 'dev' ? 'vite' : 'vite preview'
    const args = mode === 'dev' ? ['--port', APP.port] : []

    const child = spawn('npx', [command, ...args], {
      cwd: path.resolve(APP.path),
      stdio: 'inherit',
      shell: true,
    })

    this.processes.set('app', child)

    child.on('error', err => {
      console.error(`❌ ${APP.name} 启动失败:`, err)
    })

    child.on('exit', code => {
      console.log(`📦 ${APP.name} 进程退出，代码: ${code}`)
      this.processes.delete('app')
    })
  }

  // 停止应用
  stop() {
    console.log('🛑 停止应用...')
    this.processes.forEach((process) => {
      console.log(`📦 停止 ${APP.name}...`)
      process.kill('SIGTERM')
    })
    this.processes.clear()
  }

  // 构建应用
  async build() {
    console.log(`🔨 构建 ${APP.description}...`)

    return new Promise((resolve, reject) => {
      const child = spawn('npm', ['run', 'build'], {
        cwd: path.resolve(APP.path),
        stdio: 'inherit',
        shell: true,
      })

      child.on('exit', code => {
        if (code === 0) {
          console.log(`✅ ${APP.name} 构建成功`)
          resolve()
        } else {
          console.error(`❌ ${APP.name} 构建失败`)
          reject(new Error(`Build failed with code ${code}`))
        }
      })
    })
  }

  // 检查应用状态
  async checkStatus() {
    console.log('📊 微应用状态检查:')
    console.log('='.repeat(50))

    const packagePath = path.resolve(APP.path, 'package.json')
    const distPath = path.resolve(APP.path, 'dist')

    console.log(`📦 ${APP.description}`)
    console.log(`   路径: ${APP.path}`)
    console.log(`   端口: ${APP.port}`)
    console.log(`   配置: ${fs.existsSync(packagePath) ? '✅' : '❌'}`)
    console.log(`   构建: ${fs.existsSync(distPath) ? '✅' : '❌'}`)
    console.log(`   运行: ${this.processes.has('app') ? '🟢' : '🔴'}`)
    console.log('')
  }

  // 清理构建产物
  async clean() {
    console.log('🧹 清理构建产物...')

    const distPath = path.resolve(APP.path, 'dist')
    const nodeModulesPath = path.resolve(APP.path, 'node_modules')

    if (fs.existsSync(distPath)) {
      await this.execCommand(`rm -rf ${distPath}`, APP.path)
      console.log(`🗑️  清理 ${APP.name}/dist`)
    }

    if (fs.existsSync(nodeModulesPath)) {
      await this.execCommand(`rm -rf ${nodeModulesPath}`, APP.path)
      console.log(`🗑️  清理 ${APP.name}/node_modules`)
    }

    console.log('✅ 清理完成')
  }

  // 执行命令
  execCommand(command, cwd = process.cwd()) {
    return new Promise((resolve, reject) => {
      exec(command, { cwd }, (error, stdout, stderr) => {
        if (error) {
          reject(error)
        } else {
          resolve(stdout)
        }
      })
    })
  }

  // 显示帮助信息
  showHelp() {
    console.log(`
🎯 数据中台微应用管理工具
`)
    console.log('使用方法:')
    console.log('  node scripts/micro-manager.js <command>\n')
    console.log('命令:')
    console.log('  dev           启动开发服务器')
    console.log('  build         构建应用')
    console.log('  preview       预览构建结果')
    console.log('  status        检查应用状态')
    console.log('  clean         清理构建产物')
    console.log('  stop          停止运行中的应用')
    console.log('  help          显示帮助信息\n')
    console.log('示例:')
    console.log('  node scripts/micro-manager.js dev')
    console.log('  node scripts/micro-manager.js build')
    console.log('  node scripts/micro-manager.js status')
  }
}

// 主程序
async function main() {
  const manager = new MicroManager()
  const [, , command] = process.argv

  // 优雅退出处理
  process.on('SIGINT', () => {
    console.log('\n🛑 接收到退出信号，正在停止应用...')
    manager.stop()
    process.exit(0)
  })

  switch (command) {
    case 'dev':
      await manager.startApp('dev')
      break

    case 'build':
      await manager.build()
      break

    case 'preview':
      await manager.startApp('preview')
      break

    case 'status':
      await manager.checkStatus()
      break

    case 'clean':
      await manager.clean()
      break

    case 'stop':
      manager.stop()
      break

    case 'help':
    default:
      manager.showHelp()
      break
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main().catch(console.error)
}

export default MicroManager
