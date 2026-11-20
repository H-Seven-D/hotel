import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'

const frontendDir = path.resolve(import.meta.dirname, '..')
const projectDir = path.dirname(frontendDir)
const backendDir = path.join(projectDir, 'backend')
const javaHome = 'C:\\Users\\MR\\.jdks\\ms-17.0.19'
const mavenCommand = 'D:\\apache-maven-3.8.4\\bin\\mvn.cmd'
const backendJar = path.join(backendDir, 'build', 'homestay-booking-1.0.0-exec.jar')
const viteEntry = path.join(frontendDir, 'node_modules', 'vite', 'bin', 'vite.js')
const healthUrl = 'http://127.0.0.1:8080/api/health'
const requiredBackendVersion = '1.0.1'

let backendProcess
let frontendProcess
let backendStartedByThisScript = false
let shuttingDown = false

function childEnvironment() {
  return {
    ...process.env,
    JAVA_HOME: javaHome,
    Path: `${javaHome}\\bin;${process.env.Path || process.env.PATH || ''}`,
    AUTO_START_FRONTEND: 'false'
  }
}

function pipeWithPrefix(stream, prefix) {
  let buffer = ''
  stream.on('data', (chunk) => {
    buffer += chunk.toString()
    const lines = buffer.split(/\r?\n/)
    buffer = lines.pop() || ''
    for (const line of lines) {
      if (line.trim()) console.log(`${prefix} ${line}`)
    }
  })
}

async function backendHealth() {
  try {
    const response = await fetch(healthUrl, { signal: AbortSignal.timeout(1200) })
    if (!response.ok) return null
    const body = await response.json()
    return body?.data || null
  } catch {
    return null
  }
}

function startBackend() {
  if (!existsSync(mavenCommand)) {
    throw new Error(`未找到 Maven：${mavenCommand}`)
  }
  if (!existsSync(path.join(javaHome, 'bin', 'java.exe'))) {
    throw new Error(`未找到 JDK 17：${javaHome}`)
  }

  const commandShell = process.env.ComSpec || 'cmd.exe'
  console.log('[启动器] 正在编译后端...')
  const buildResult = spawnSync(
    commandShell,
    [
      '/d',
      '/s',
      '/c',
      `${mavenCommand} -Dmaven.repo.local=D:\\hotel\\.m2\\repository -DskipTests package`
    ],
    {
      cwd: backendDir,
      env: childEnvironment(),
      windowsHide: true,
      stdio: 'inherit'
    }
  )
  if (buildResult.status !== 0) {
    throw new Error(`后端打包失败，退出码：${buildResult.status ?? 'unknown'}`)
  }
  if (!existsSync(backendJar)) {
    throw new Error(`未找到后端可执行 JAR：${backendJar}`)
  }

  backendProcess = spawn(
    path.join(javaHome, 'bin', 'java.exe'),
    ['-jar', backendJar],
    {
      cwd: backendDir,
      env: childEnvironment(),
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe']
    }
  )
  backendStartedByThisScript = true
  pipeWithPrefix(backendProcess.stdout, '[后端]')
  pipeWithPrefix(backendProcess.stderr, '[后端]')

  backendProcess.on('exit', (code) => {
    if (!shuttingDown) {
      console.error(`[后端] 进程已退出，退出码：${code ?? 'unknown'}`)
      shutdown(code || 1)
    }
  })
}

function startFrontend() {
  if (!existsSync(viteEntry)) {
    throw new Error('前端依赖尚未安装，请先在 frontend 目录执行 npm install')
  }
  frontendProcess = spawn(process.execPath, [viteEntry, '--open'], {
    cwd: frontendDir,
    env: process.env,
    windowsHide: true,
    stdio: ['ignore', 'pipe', 'pipe']
  })
  pipeWithPrefix(frontendProcess.stdout, '[前端]')
  pipeWithPrefix(frontendProcess.stderr, '[前端]')

  frontendProcess.on('exit', (code) => {
    if (!shuttingDown) {
      console.log(`[前端] 开发服务器已停止，退出码：${code ?? 0}`)
      shutdown(code || 0)
    }
  })
}

async function waitForBackend() {
  const deadline = Date.now() + 120_000
  while (Date.now() < deadline) {
    const health = await backendHealth()
    if (health?.status === 'UP' && health?.version === requiredBackendVersion) return
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
  throw new Error('后端在 120 秒内未完成启动，请查看 [后端] 日志')
}

function stopProcessTree(child) {
  if (!child || child.exitCode !== null) return
  if (process.platform === 'win32' && child.pid) {
    spawnSync('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], {
      windowsHide: true,
      stdio: 'ignore'
    })
    return
  }
  child.kill('SIGTERM')
}

function shutdown(exitCode = 0) {
  if (shuttingDown) return
  shuttingDown = true
  stopProcessTree(frontendProcess)
  if (backendStartedByThisScript) {
    stopProcessTree(backendProcess)
  }
  setTimeout(() => process.exit(exitCode), 300)
}

process.on('SIGINT', () => shutdown(0))
process.on('SIGTERM', () => shutdown(0))

try {
  const existingBackend = await backendHealth()
  if (existingBackend && existingBackend.version !== requiredBackendVersion) {
    throw new Error(
      `检测到旧版后端仍在运行（版本 ${existingBackend.version || 'unknown'}），` +
      '请先点击 IDEA 红色停止按钮关闭旧后端，再重新运行“一键启动”'
    )
  }
  if (existingBackend) {
    console.log('[启动器] 检测到后端已运行，将直接启动前端。')
  } else {
    console.log('[启动器] 正在启动 Spring Boot 后端...')
    startBackend()
    await waitForBackend()
    console.log('[启动器] 后端健康检查通过。')
  }
  console.log('[启动器] 正在启动 Vue 前端并打开浏览器...')
  startFrontend()
} catch (error) {
  console.error(`[启动器] 启动失败：${error.message}`)
  shutdown(1)
}
