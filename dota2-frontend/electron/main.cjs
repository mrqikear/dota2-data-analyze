const { app, BrowserWindow, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const { spawn } = require('child_process');

let splashWindow;
let mainWindow;
let javaProcess;

// 高频检测后端端口 (9601) 是否就绪 (200ms 轮询)
function checkBackendReady(callback, retries = 100) {
  if (retries <= 0) {
    callback(false);
    return;
  }
  const req = http.get('http://127.0.0.1:9601/swagger-ui.html', (res) => {
    if (res.statusCode >= 200 && res.statusCode < 400) {
      callback(true);
    } else {
      setTimeout(() => checkBackendReady(callback, retries - 1), 200);
    }
  });
  req.on('error', () => {
    setTimeout(() => checkBackendReady(callback, retries - 1), 200);
  });
  req.end();
}

function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 480,
    height: 320,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    center: true,
    resizable: false,
    show: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  const splashHtml = `
  <!DOCTYPE html>
  <html>
  <head>
    <meta charset="UTF-8">
    <style>
      body {
        margin: 0;
        padding: 0;
        background: #12141d;
        color: #ffffff;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100vh;
        border-radius: 12px;
        box-shadow: 0 16px 32px rgba(0,0,0,0.6);
        border: 1px solid #2a2e3d;
        user-select: none;
      }
      .logo {
        font-size: 26px;
        font-weight: 700;
        background: linear-gradient(135deg, #ff4e50, #f9d423);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin-bottom: 8px;
      }
      .sub {
        font-size: 13px;
        color: #8a8f9d;
        margin-bottom: 24px;
      }
      .spinner {
        width: 32px;
        height: 32px;
        border: 3px solid rgba(255, 255, 255, 0.1);
        border-radius: 50%;
        border-top-color: #ff4e50;
        animation: spin 0.8s ease-in-out infinite;
      }
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
      .tip {
        margin-top: 16px;
        font-size: 12px;
        color: #616675;
      }
    </style>
  </head>
  <body>
    <div class="logo">Dota 2 数据分析平台</div>
    <div class="sub">正在极速加载引擎与本地数据库...</div>
    <div class="spinner"></div>
    <div class="tip">首次启动准备中，请稍候</div>
  </body>
  </html>
  `;

  splashWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(splashHtml)}`);
}

function startBackend() {
  const isPackaged = app.isPackaged;
  
  // Java 路径
  let javaBin;
  if (isPackaged) {
    const isWin = process.platform === 'win32';
    const javaExecName = isWin ? 'java.exe' : 'java';
    javaBin = path.join(process.resourcesPath, 'jre', 'bin', javaExecName);
  } else {
    javaBin = 'java';
  }

  // Jar 包路径
  let jarPath;
  if (isPackaged) {
    jarPath = path.join(process.resourcesPath, 'backend', 'dota2-api.jar');
  } else {
    jarPath = path.join(__dirname, '../../dota2-api/target/dota2-api-1.0-SNAPSHOT.jar');
  }

  // 数据存储目录
  const userDataPath = isPackaged 
    ? path.join(app.getPath('userData'), 'data')
    : path.join(__dirname, '../../data');

  if (!fs.existsSync(userDataPath)) {
    fs.mkdirSync(userDataPath, { recursive: true });
  }

  const duckdbFile = path.join(userDataPath, 'dota2_analyze.duckdb');
  console.log('[Electron] DuckDB 文件路径:', duckdbFile);

  const env = Object.assign({}, process.env, {
    DUCKDB_PATH: duckdbFile
  });

  // 🚀 JVM 桌面端极速启动优化参数
  const jvmArgs = [
    '-Xms128m',
    '-Xmx512m',
    '-XX:+TieredCompilation',
    '-XX:TieredStopAtLevel=1',
    '-Dspring.jmx.enabled=false',
    '-Dspring.main.lazy-initialization=true',
    '-jar', jarPath,
    '--spring.profiles.active=dev'
  ];

  try {
    javaProcess = spawn(javaBin, jvmArgs, {
      windowsHide: true,
      cwd: isPackaged ? app.getPath('userData') : path.join(__dirname, '../..'),
      env: env
    });

    javaProcess.stdout.on('data', (data) => {
      console.log(`[Jar Output]: ${data}`);
    });

    javaProcess.stderr.on('data', (data) => {
      console.error(`[Jar Error]: ${data}`);
    });

    javaProcess.on('error', (err) => {
      console.error('[Electron] 无法启动 Java 进程:', err);
    });
  } catch (e) {
    console.error('[Electron] 启动子进程异常:', e);
  }
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 868,
    minWidth: 1024,
    minHeight: 700,
    title: 'Dota 2 数据分析平台',
    autoHideMenuBar: true,
    show: false, // 准备好后再显示
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  const isPackaged = app.isPackaged;
  if (isPackaged) {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  } else {
    mainWindow.loadURL('http://localhost:5200');
  }

  mainWindow.once('ready-to-show', () => {
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.destroy();
    }
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.on('ready', () => {
  // 1. 0.1s 极速弹出 Splash 加载动画屏
  createSplashWindow();

  // 2. 启动 JVM 后端
  startBackend();

  // 3. 高频监听后端就绪
  checkBackendReady((ready) => {
    if (!ready) {
      console.warn('[Electron] 后端响应超时，仍然尝试打开界面');
    }
    createMainWindow();
  });
});

app.on('will-quit', () => {
  if (javaProcess) {
    console.log('[Electron] 正在关闭后台 Java 进程...');
    javaProcess.kill();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
