#!/bin/bash
set -e

echo "========================================================"
echo "  Dota2 数据分析平台 - macOS 桌面安装包 (.dmg) 打包脚本"
echo "========================================================"
echo ""

# 1. 检查 Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到 Java 环境，请先安装 JDK 11+"
    exit 1
fi

# 2. 编译后端 Jar
echo "[1/4] 编译 Spring Boot 后端 Jar 包..."
mvn clean package -DskipTests
echo "[成功] 后端编译完成！"
echo ""

# 3. 准备打包资源目录
echo "[2/4] 准备 Electron 资源文件..."
mkdir -p dota2-frontend/resources/backend
cp dota2-api/target/dota2-api-1.0-SNAPSHOT.jar dota2-frontend/resources/backend/dota2-api.jar

# 检查/复制 JRE 环境 (macOS)
if [ ! -d "dota2-frontend/resources/jre" ]; then
    if [ -n "$JAVA_HOME" ]; then
        echo "复制 macOS JRE 环境自 $JAVA_HOME ..."
        cp -R "$JAVA_HOME" dota2-frontend/resources/jre
    else
        echo "[提示] 未设置 JAVA_HOME，打包将尝试依赖目标 Mac 系统的 Java 环境。"
    fi
fi
echo ""

# 4. 编译前端与打包 macOS dmg
echo "[3/4] 编译前端 Vue 静态资源..."
cd dota2-frontend
yarn build

echo ""
echo "[4/4] 使用 Electron Builder 构建 macOS DMG 安装包..."
yarn electron:build --mac

cd ..
echo ""
echo "========================================================"
echo "===== macOS 打包完成！====="
echo "安装包位置: dota2-frontend/dist_electron/"
echo "文件包含: Dota2DataAnalyze-1.0.0.dmg"
echo "========================================================"
