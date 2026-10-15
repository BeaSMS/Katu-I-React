const { app, BrowserWindow, session, shell } = require('electron')
const path = require('node:path')
const { fileURLToPath } = require('node:url')

const isDevelopment = !app.isPackaged
const developmentUrl = 'http://127.0.0.1:5173/?desktop=1'

function isInternalUrl(value) {
  if (isDevelopment) {
    try {
      return new URL(value).origin === developmentUrl
    } catch {
      return false
    }
  }
  try {
    return fileURLToPath(value) === path.resolve(__dirname, '..', 'dist', 'index.html')
  } catch {
    return false
  }
}

function createWindow() {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 900,
    minHeight: 620,
    show: false,
    backgroundColor: '#f7f9f7',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
      webviewTag: false,
    },
  })

  window.once('ready-to-show', () => window.show())
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://') || url.startsWith('http://')) shell.openExternal(url)
    return { action: 'deny' }
  })
  window.webContents.on('will-navigate', (event, url) => {
    if (!isInternalUrl(url)) event.preventDefault()
  })

  if (isDevelopment) {
    window.loadURL(developmentUrl)
  } else {
    window.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  }
}

const hasSingleInstance = app.requestSingleInstanceLock()
if (!hasSingleInstance) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const [window] = BrowserWindow.getAllWindows()
    if (window) {
      if (window.isMinimized()) window.restore()
      window.focus()
    }
  })

  app.whenReady().then(() => {
    app.setAppUserModelId('com.katui.desktop')
    session.defaultSession.setPermissionRequestHandler((_webContents, permission, callback) => {
      callback(permission === 'notifications')
    })
    createWindow()

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow()
    })
  })

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit()
  })
}
