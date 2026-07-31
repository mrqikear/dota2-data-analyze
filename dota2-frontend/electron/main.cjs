const { app, BrowserWindow, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const { spawn } = require('child_process');

let mainWindow;
let javaProcess;

// 高频检测后端端口 (9601) 是否就绪
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

function startBackend() {
  const isPackaged = app.isPackaged;
  
  let javaBin = 'java';
  if (isPackaged) {
    const isWin = process.platform === 'win32';
    const javaExecName = isWin ? 'java.exe' : 'java';
    const bundledJava = path.join(process.resourcesPath, 'jre', 'bin', javaExecName);
    if (fs.existsSync(bundledJava)) {
      javaBin = bundledJava;
    }
  }

  let jarPath;
  if (isPackaged) {
    jarPath = path.join(process.resourcesPath, 'backend', 'dota2-api.jar');
  } else {
    jarPath = path.join(__dirname, '../../dota2-api/target/dota2-api-1.0-SNAPSHOT.jar');
  }

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

    javaProcess.stdout.on('data', (data) => console.log(`[Jar Output]: ${data}`));
    javaProcess.stderr.on('data', (data) => console.error(`[Jar Error]: ${data}`));
  } catch (e) {
    console.error('[Electron] 启动子进程异常:', e);
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 868,
    minWidth: 1024,
    minHeight: 700,
    title: 'Dota 2 数据分析平台',
    autoHideMenuBar: true,
    show: false, // 端口 Ready 后再 show，杜绝 Network Error！
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
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.on('ready', () => {
  startBackend();
  createWindow();
  
  // 确保后端端口在 2 秒内就绪后再优雅显示，避免 Network Error！
  checkBackendReady((ready) => {
    if (!ready) {
      console.warn('[Electron] 后端响应稍有延迟，界面已就绪');
    }
    if (mainWindow && !mainWindow.isVisible()) {
      mainWindow.show();
    }
  });
});

app.on('will-quit', () => {
  if (javaProcess) {
    javaProcess.kill();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
