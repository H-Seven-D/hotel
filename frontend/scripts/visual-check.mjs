import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright-core'

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const baseUrl = process.env.VISUAL_BASE_URL || 'http://127.0.0.1:5173'
const outputDir = 'artifacts'

await mkdir(outputDir, { recursive: true })

const browser = await chromium.launch({
  headless: true,
  executablePath: edgePath
})

const consoleErrors = []
const pageErrors = []

async function layoutMetrics(page) {
  return page.evaluate(() => {
    const visibleButtons = Array.from(document.querySelectorAll('button')).filter((button) => {
      const rect = button.getBoundingClientRect()
      const style = window.getComputedStyle(button)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden'
    })
    return {
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      horizontalOverflow: Math.max(document.documentElement.scrollWidth - window.innerWidth, 0),
      media640: window.matchMedia('(max-width: 640px)').matches,
      statGridColumns: document.querySelector('.stat-grid')
        ? window.getComputedStyle(document.querySelector('.stat-grid')).gridTemplateColumns
        : null,
      overflowingElements: Array.from(document.querySelectorAll('body *'))
        .map((element) => ({ element, rect: element.getBoundingClientRect() }))
        .filter(({ element, rect }) => {
          const style = window.getComputedStyle(element)
          return rect.width > 0 && rect.right > window.innerWidth + 2 && style.position !== 'fixed'
        })
        .slice(0, 8)
        .map(({ element, rect }) => ({
          tag: element.tagName.toLowerCase(),
          className: String(element.className || '').slice(0, 120),
          width: Math.round(rect.width),
          right: Math.round(rect.right)
        })),
      visibleButtonCount: visibleButtons.length,
      clippedButtons: visibleButtons
        .filter((button) => button.scrollWidth > button.clientWidth + 2 || button.scrollHeight > button.clientHeight + 2)
        .map((button) => button.innerText.trim())
        .filter(Boolean)
    }
  })
}

try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  await page.addInitScript(() => {
    localStorage.setItem('homestay_token', 'legacy-token')
    localStorage.setItem('homestay_user', JSON.stringify({
      id: 1,
      username: 'legacy',
      realName: '旧登录状态',
      role: 'ADMIN'
    }))
  })
  await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
  await page.waitForURL((url) => url.pathname === '/login')
  const legacySessionCleared = await page.evaluate(() =>
    localStorage.getItem('homestay_token') === null &&
    localStorage.getItem('homestay_user') === null
  )
  await page.getByRole('heading', { name: '进入管理工作台' }).waitFor()
  const loginBrandVisible = await page.getByText('yeu', { exact: true }).first().isVisible()
  const legacyBrandCount = await page.getByText(/栖宿/).count()
  await page.screenshot({ path: `${outputDir}/login-desktop.png`, fullPage: true })

  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123')
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.waitForURL('**/dashboard')
  await page.getByRole('heading', { name: '运营总览', level: 1 }).waitFor()
  await page.reload({ waitUntil: 'networkidle' })
  await page.getByRole('heading', { name: '运营总览', level: 1 }).waitFor()
  const sessionLoginPersists = page.url().endsWith('/dashboard')
  await page.waitForTimeout(1500)
  await page.screenshot({ path: `${outputDir}/dashboard-desktop.png`, fullPage: true })
  const dashboardLayout = await layoutMetrics(page)

  const chart = await page.locator('canvas').first().evaluate((canvas) => {
    const context2d = canvas.getContext('2d')
    const pixels = context2d.getImageData(0, 0, canvas.width, canvas.height).data
    let visibleSamples = 0
    for (let index = 3; index < pixels.length; index += 400) {
      if (pixels[index] > 0) visibleSamples += 1
    }
    return {
      width: canvas.width,
      height: canvas.height,
      visibleSamples
    }
  })

  await page.getByRole('button', { name: '房型房间' }).click()
  await page.waitForURL('**/rooms')
  await page.getByRole('heading', { name: '房型房间', level: 1 }).waitFor()
  await page.waitForTimeout(500)
  await page.screenshot({ path: `${outputDir}/rooms-desktop.png`, fullPage: true })
  const roomsLayout = await layoutMetrics(page)

  await page.getByRole('button', { name: '预订入住' }).click()
  await page.waitForURL('**/bookings')
  await page.getByRole('heading', { name: '预订入住', level: 1 }).waitFor()
  await page.waitForTimeout(500)
  await page.screenshot({ path: `${outputDir}/bookings-desktop.png`, fullPage: true })
  const bookingsLayout = await layoutMetrics(page)

  const adminMenusVisible = await page.getByRole('button', { name: '员工管理' }).isVisible()
  const routeChecks = {}
  for (const route of [
    { path: 'properties', title: '门店管理' },
    { path: 'customers', title: '客户档案' },
    { path: 'schedules', title: '排班调度' },
    { path: 'employees', title: '员工管理' },
    { path: 'audit', title: '审计中心' }
  ]) {
    await page.goto(`${baseUrl}/${route.path}`, { waitUntil: 'networkidle' })
    await page.getByRole('heading', { name: route.title, level: 1 }).waitFor()
    routeChecks[route.path] = 'ok'
  }
  const mobileContext = await browser.newContext({
    viewport: { width: 390, height: 844 }
  })
  const mobilePage = await mobileContext.newPage()
  mobilePage.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text())
  })
  mobilePage.on('pageerror', (error) => pageErrors.push(error.message))
  await mobilePage.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
  await mobilePage.waitForURL((url) => url.pathname === '/login')
  await mobilePage.getByPlaceholder('请输入账号').fill('admin')
  await mobilePage.getByPlaceholder('请输入密码').fill('Admin@123')
  await mobilePage.getByRole('button', { name: '登录系统' }).click()
  await mobilePage.waitForURL('**/dashboard')
  await mobilePage.getByRole('heading', { name: '运营总览', level: 1 }).waitFor()
  await mobilePage.waitForTimeout(800)
  await mobilePage.screenshot({ path: `${outputDir}/dashboard-mobile.png`, fullPage: true })
  const mobileLayout = await layoutMetrics(mobilePage)
  await mobilePage.locator('.mobile-menu-button').click()
  await mobilePage.locator('.mobile-menu').waitFor()
  await mobilePage.screenshot({ path: `${outputDir}/dashboard-mobile-menu.png`, fullPage: true })
  await mobileContext.close()

  const employeeContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  const employeePage = await employeeContext.newPage()
  await employeePage.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  await employeePage.getByPlaceholder('请输入账号').fill('employee')
  await employeePage.getByPlaceholder('请输入密码').fill('Staff@123')
  await employeePage.getByRole('button', { name: '登录系统' }).click()
  await employeePage.waitForURL('**/dashboard')
  const employeeAdminMenuVisible = await employeePage.getByRole('button', { name: '员工管理' }).isVisible()
  await employeePage.goto(`${baseUrl}/employees`, { waitUntil: 'networkidle' })
  await employeePage.waitForURL('**/dashboard')
  const employeeRouteRedirected = employeePage.url().endsWith('/dashboard')
  await employeePage.locator('.user-chip').click()
  await employeePage.getByRole('menuitem', { name: '退出登录' }).click()
  await employeePage.getByRole('button', { name: '退出登录' }).click()
  await employeePage.waitForURL('**/login')
  await employeeContext.close()

  const result = {
    loginPage: 'ok',
    legacySessionCleared,
    sessionLoginPersists,
    loginBrandVisible,
    legacyBrandCount,
    dashboard: 'ok',
    roomsPage: 'ok',
    bookingsPage: 'ok',
    adminMenusVisible,
    employeeAdminMenuVisible,
    employeeRouteRedirected,
    routeChecks,
    chart,
    layouts: {
      dashboardDesktop: dashboardLayout,
      roomsDesktop: roomsLayout,
      bookingsDesktop: bookingsLayout,
      dashboardMobile: mobileLayout
    },
    consoleErrors,
    pageErrors
  }

  console.log(JSON.stringify(result, null, 2))

  const layouts = Object.values(result.layouts)
  const invalidLayout = layouts.some((layout) =>
    layout.horizontalOverflow > 1 || layout.clippedButtons.length > 0
  )
  if (
    !adminMenusVisible ||
    !legacySessionCleared ||
    !sessionLoginPersists ||
    !loginBrandVisible ||
    legacyBrandCount > 0 ||
    employeeAdminMenuVisible ||
    !employeeRouteRedirected ||
    chart.visibleSamples === 0 ||
    invalidLayout ||
    consoleErrors.length ||
    pageErrors.length
  ) {
    process.exitCode = 1
  }
} finally {
  await browser.close()
}
