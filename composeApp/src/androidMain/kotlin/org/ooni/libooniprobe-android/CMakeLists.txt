cmake_minimum_required(VERSION 3.4.1)
project("OONIProbe")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
add_link_options(LINKER:--build-id=none)
add_compile_options(-Wall -Werror)

set(arch_map_x86    386)
set(arch_map_x86_64 amd64)
set(arch_map_arm    arm)
set(arch_map_arm64  arm64)

add_custom_target(libooniprobe.so
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
        COMMENT "Building libooniprobe"
        VERBATIM COMMAND "${ANDROID_HOST_PREBUILTS}/bin/make" "lib-android"
        ANDROID_PACKAGE_NAME=${ANDROID_PACKAGE_NAME}

        SYSROOT=${CMAKE_SYSROOT}
        TARGET=${CMAKE_C_COMPILER_TARGET}
        CGO_ENABLED=1
        GOOS=android
        GOARCH=${arch_map_${ANDROID_ARCH_NAME}}

        CC=${ANDROID_C_COMPILER}
        CXX=${ANDROID_CXX_COMPILER}
        CFLAGS=${CMAKE_C_FLAGS}
        LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS}

        DESTDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        BUILDDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/../generated-src
)