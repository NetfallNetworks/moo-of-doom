#!/usr/bin/env bash
# sandbox-build.sh — run Gradle through a local proxy relay so that
# Java-based tools can authenticate with the Claude Code Web sandbox proxy.
#
# Usage:
#   scripts/sandbox-build.sh                     # defaults to: build --no-configuration-cache
#   scripts/sandbox-build.sh test                # run tests only
#   scripts/sandbox-build.sh build --info        # any Gradle args
#
# Why this exists:
#   Java's HttpClient doesn't send Proxy-Authorization from env/system
#   properties, so NeoForm's artifact downloads fail with HTTP 407.
#   This script spins up a local relay that injects the auth header,
#   points Gradle at it, then cleans up when done.
#
# Known limitation:
#   Even with auth fixed, the sandbox egress allowlist may not include
#   all Mojang CDN domains (e.g. piston-meta.mojang.com). The build
#   will still fail if required domains are blocked. Track progress at:
#   https://github.com/anthropics/claude-code/issues/11897
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCAL_PROXY_PORT="${LOCAL_PROXY_PORT:-18080}"

# --- Guard: only needed inside the sandbox ---
if [[ -z "${HTTPS_PROXY:-}" ]]; then
    echo "No HTTPS_PROXY detected — running Gradle directly."
    exec "$PROJECT_DIR/gradlew" "${@:-build --no-configuration-cache}"
fi

# --- Defaults ---
if [[ $# -eq 0 ]]; then
    GRADLE_ARGS=(build --no-configuration-cache)
else
    GRADLE_ARGS=("$@")
fi

cleanup() {
    if [[ -n "${RELAY_PID:-}" ]] && kill -0 "$RELAY_PID" 2>/dev/null; then
        kill "$RELAY_PID" 2>/dev/null
        echo "Proxy relay stopped."
    fi
}
trap cleanup EXIT

# --- Start the relay ---
echo "Starting proxy relay on 127.0.0.1:$LOCAL_PROXY_PORT ..."
LOCAL_PROXY_PORT="$LOCAL_PROXY_PORT" python3 "$SCRIPT_DIR/proxy_relay.py" &
RELAY_PID=$!
sleep 1

if ! kill -0 "$RELAY_PID" 2>/dev/null; then
    echo "ERROR: Proxy relay failed to start." >&2
    exit 1
fi

# --- Run Gradle through the relay ---
echo "Running: gradlew ${GRADLE_ARGS[*]}"
HTTPS_PROXY="http://127.0.0.1:$LOCAL_PROXY_PORT" \
HTTP_PROXY="http://127.0.0.1:$LOCAL_PROXY_PORT" \
JAVA_TOOL_OPTIONS="\
-Dhttp.proxyHost=127.0.0.1 \
-Dhttp.proxyPort=$LOCAL_PROXY_PORT \
-Dhttps.proxyHost=127.0.0.1 \
-Dhttps.proxyPort=$LOCAL_PROXY_PORT \
-Dhttp.nonProxyHosts=localhost|127.0.0.1|169.254.169.254 \
-Djdk.http.auth.tunneling.disabledSchemes= \
-Djdk.http.auth.proxying.disabledSchemes=" \
"$PROJECT_DIR/gradlew" "${GRADLE_ARGS[@]}"
