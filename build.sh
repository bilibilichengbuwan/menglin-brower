#!/bin/bash
set -e

# Java 路径：优先使用环境变量 JAVA_HOME，否则使用默认路径
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export PATH=$JAVA_HOME/bin:$PATH

# Android SDK 路径：优先使用环境变量 ANDROID_HOME，否则使用默认路径
export ANDROID_HOME=${ANDROID_HOME:-/workspace/android-sdk}
export BUILD_TOOLS=$ANDROID_HOME/build-tools/35.0.0
export PLATFORM=$ANDROID_HOME/platforms/android-34
export PATH=$BUILD_TOOLS:$PATH

# 项目路径：使用脚本所在目录
PROJ=$(cd "$(dirname "$0")" && pwd)
cd $PROJ

rm -rf build
mkdir -p build/{classes,dex,gen,apk,compiled}

# 1. Compile resources with aapt2
echo "[1/6] 编译资源..."
find res -type f \( -name "*.xml" -o -name "*.png" -o -name "*.jpg" -o -name "*.webp" \) | while read f; do
  aapt2 compile -o build/compiled/ "$f" || true
done

# 2. Link resources into APK with aapt2
echo "[2/6] 链接资源..."
aapt2 link build/compiled/*.flat \
  -I $PLATFORM/android.jar \
  --manifest AndroidManifest.xml \
  -A assets \
  -o build/apk/unaligned.apk \
  --java build/gen \
  --auto-add-overlay

# 3. Compile Java source
echo "[3/6] 编译 Java..."
javac -source 1.8 -target 1.8 -bootclasspath $PLATFORM/android.jar \
  -cp $PLATFORM/android.jar \
  -d build/classes \
  $(find java build/gen -name "*.java")

# 4. Dex classes
echo "[4/6] 转换 Dex..."
CLASSES=$(find build/classes -name "*.class")
d8 --lib $PLATFORM/android.jar --min-api 21 --thread-count 1 --output build/dex $CLASSES

# 5. Add dex to APK
echo "[5/6] 打包 APK..."
cp build/apk/unaligned.apk build/apk/with-dex.apk
# add classes.dex
pushd build/dex > /dev/null
  zip -r ../apk/with-dex.apk classes.dex 2>&1 | tail -2
popd > /dev/null

# 6. Sign and align
echo "[6/6] 签名并对齐..."
VERSION="v1.0"

# Generate debug keystore if not exists
if [ ! -f $PROJ/debug.keystore ]; then
  keytool -genkey -v -keystore $PROJ/debug.keystore \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" 2>&1 | tail -3
fi

# Align first
zipalign -f -p 4 build/apk/with-dex.apk build/apk/aligned.apk

# Sign
APK_NAME="menglin-browser-${VERSION}.apk"
apksigner sign --ks $PROJ/debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out $PROJ/$APK_NAME build/apk/aligned.apk

# Verify
apksigner verify --verbose $PROJ/$APK_NAME 2>&1 | head -10

echo ""
echo "=========================="
echo " 构建完成！APK: $APK_NAME (versionName: $VERSION)"
echo "=========================="
ls -la $PROJ/$APK_NAME
