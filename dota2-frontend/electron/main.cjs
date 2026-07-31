const { app, BrowserWindow, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const { spawn } = require('child_process');

let mainWindow;
let javaProcess;

// 检测后端端口 (9601) 是否就绪
function checkBackendReady(callback, retries = 30) {
  if (retries <= 0) {
    callback(false);
    return;
  }
  const req = http.get('http://127.0.0.1:9601/swagger-ui.html', (res) => {
    if (res.statusCode >= 200 && res.statusCode < 400) {
      callback(true);
    } else {
      setTimeout(() => checkBackendReady(callback, retries - 1), 1000);
    }
  });
  req.on('error', () => {
    setTimeout(() => checkBackendReady(callback, retries - 1), 1000);
  });
  req.end();
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
  console.log('[Electron] Java 路径:', javaBin);
  console.log('[Electron] Jar 包路径:', jarPath);

  const env = Object.assign({}, process.env, {
    DUCKDB_PATH: duckdbFile
  });

  try {
    javaProcess = spawn(javaBin, ['-jar', jarPath, '--spring.profiles.active=dev'], {
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

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1366,
    height: 868,
    minWidth: 1024,
    minHeight: 700,
    title: 'Dota 2 数据分析平台',
    autoHideMenuBar: true,
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

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.on('ready', () => {
  startBackend();
  // 等待后端连接就绪后创建主窗口
  checkBackendReady((ready) => {
    if (!ready) {
      console.warn('[Electron] 后端响应超时，仍然尝试打开界面');
    }
    createWindow();
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
