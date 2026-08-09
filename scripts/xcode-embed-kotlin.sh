#!/bin/bash
# Invoked from Xcode "Compile Kotlin" run script phases.
set -euo pipefail

if [ "YES" = "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-}" ]; then
  echo "Skipping Kotlin embed (OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES)"
  exit 0
fi

if [ -z "${JAVA_HOME:-}" ]; then
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  elif /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home)"
  elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  fi
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "error: JDK 17+ not found. Install OpenJDK 17 or set JAVA_HOME for the Xcode build."
  echo "  brew install openjdk@17"
  echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

if [ $# -lt 1 ]; then
  echo "error: Gradle task argument required (e.g. :sak-ios-bridge:embedAndSignAppleFrameworkForXcode)"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

echo "Using JAVA_HOME=$JAVA_HOME"
echo "Running: ./gradlew $*"
./gradlew "$@"
