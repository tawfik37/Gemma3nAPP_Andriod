cmake_minimum_required(VERSION 3.18.1)

project("androidllmapp")

# whisper.cpp must already be cloned at root
add_subdirectory(${CMAKE_SOURCE_DIR}/../whisper.cpp whisper-build)

add_library(
        whisper-jni SHARED
        ${CMAKE_SOURCE_DIR}/src/main/cpp/whisper-jni.cpp
)

find_library(log-lib log)

target_link_libraries(
        whisper-jni
        whisper
        ${log-lib}
)
