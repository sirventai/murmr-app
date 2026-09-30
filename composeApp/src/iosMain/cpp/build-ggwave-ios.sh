#!/bin/bash
# Compila ggwave.cpp como libreria estatica para iOS device (arm64) + simulator
# (arm64). Salida: libggwave.a en cada subdir, listo para cinterop.
#
# Uso: ./build-ggwave-ios.sh
# Se ejecuta automaticamente como dependencia del task cinterop si se conecta
# desde build.gradle.kts (ver tareas `prepareGgwaveIos`).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${SCRIPT_DIR}/../../nativeShared/ggwave"
SRC="${SRC_DIR}/ggwave.cpp"
HEADER_DIR="${SRC_DIR}"

DEPLOY_TARGET="17.0"
COMMON_FLAGS=(
    -std=c++17
    -O2
    -fexceptions
    -fno-exceptions  # disable exceptions: ggwave usa <stdexcept> minimal, evitamos overhead
)
# Re-enable exceptions: Reed-Solomon hpp puede usarlas; mejor mantenerlas.
COMMON_FLAGS=(
    -std=c++17
    -O2
    -fexceptions
    -fvisibility=hidden
    -DNDEBUG
    -I"${HEADER_DIR}"
)

build_arch() {
    local sdk="$1"           # iphoneos | iphonesimulator
    local arch="$2"          # arm64
    local minflag="$3"       # -mios-version-min | -mios-simulator-version-min
    local outdir="$4"

    local obj="${outdir}/ggwave.o"
    local lib="${outdir}/libggwave.a"
    rm -f "${obj}" "${lib}"

    echo "[ggwave-ios] compilando ${sdk} ${arch}..."
    xcrun --sdk "${sdk}" clang++ \
        -arch "${arch}" \
        "${minflag}=${DEPLOY_TARGET}" \
        "${COMMON_FLAGS[@]}" \
        -c "${SRC}" \
        -o "${obj}"

    echo "[ggwave-ios] empaquetando ${lib}..."
    xcrun --sdk "${sdk}" ar rcs "${lib}" "${obj}"
    rm -f "${obj}"
    echo "[ggwave-ios] OK ${lib}"
}

build_arch iphoneos        arm64 -mios-version-min          "${SCRIPT_DIR}/iphoneos"
build_arch iphonesimulator arm64 -mios-simulator-version-min "${SCRIPT_DIR}/iphonesimulator"

echo "[ggwave-ios] HECHO. Libs en:"
ls -la "${SCRIPT_DIR}/iphoneos" "${SCRIPT_DIR}/iphonesimulator"
