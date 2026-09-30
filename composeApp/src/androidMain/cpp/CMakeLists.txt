cmake_minimum_required(VERSION 3.22)
project(ggwave_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source dir compartido entre Android e iOS - mismo ggwave.cpp / .h.
set(GGWAVE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../nativeShared/ggwave)

add_library(ggwave_native SHARED
    ${GGWAVE_DIR}/ggwave.cpp
    ggwave_jni.cpp
)

target_include_directories(ggwave_native PRIVATE
    ${GGWAVE_DIR}
)

# Reed-Solomon usa templates pesados; -O2 acelera el decode bastante.
target_compile_options(ggwave_native PRIVATE
    -O2
    -fexceptions
    -Wno-unused-parameter
    -Wno-deprecated-declarations
)

find_library(LOG_LIB log)
target_link_libraries(ggwave_native PRIVATE ${LOG_LIB})
