# Makefile for NetworkTypeFinder JNI Bridge

# Variables
OBJC_COMPILER = clang
OBJC_FILES = objc/NetworkTypeFinder.m
LIBRARY_NAME = networktypefinder
LIBRARY_FILE_MAC = lib$(LIBRARY_NAME).dylib
LIBRARY_FILE_LINUX = lib$(LIBRARY_NAME).so
JAVA_HOME ?= $(shell java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $$3}')

# Detect platform
UNAME_S := $(shell uname -s)

ifeq ($(UNAME_S),Darwin)
PLATFORM_TARGET := macos
LIBRARY_FILE := $(LIBRARY_FILE_MAC)
else ifeq ($(UNAME_S),Linux)
PLATFORM_TARGET := linux
LIBRARY_FILE := $(LIBRARY_FILE_LINUX)
else ifeq ($(OS),Windows_NT)
PLATFORM_TARGET := windows
LIBRARY_FILE := $(LIBRARY_NAME).dll
else
PLATFORM_TARGET := unknown
LIBRARY_FILE :=
endif

# Default target: build only for detected platform
all: $(PLATFORM_TARGET)

macos:
	@echo "Compiling Objective-C code to a dynamic library for macOS..."
	@mkdir -p build
	$(OBJC_COMPILER) -dynamiclib -o build/$(LIBRARY_FILE_MAC) $(OBJC_FILES) -framework Foundation -framework SystemConfiguration -framework CoreFoundation -framework Network -framework CFNetwork -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
	@echo "Library created at build/$(LIBRARY_FILE_MAC)"

linux:
	@echo "Compiling Objective-C code to a dynamic library for Linux..."
	@mkdir -p build
	$(OBJC_COMPILER) -shared -fPIC -o build/$(LIBRARY_FILE_LINUX) $(OBJC_FILES) -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
	@echo "Library created at build/$(LIBRARY_FILE_LINUX)"

windows:
	@echo "Compiling Objective-C code to a dynamic library for Windows..."
	@mkdir -p build
	$(OBJC_COMPILER) -shared -m64 -o build\\$(LIBRARY_NAME).dll $(OBJC_FILES) '-I$(JAVA_HOME)include' '-I$(JAVA_HOME)include\\win32' -lws2_32 -liphlpapi
	@echo "Library created at build/$(LIBRARY_NAME).dll"

# Install the library to system path
install: all
	@echo "Installing library to system path..."
ifeq ($(PLATFORM_TARGET),macos)
	cp build/$(LIBRARY_FILE_MAC) $(HOME)/Library/Java/Extensions
else ifeq ($(PLATFORM_TARGET),linux)
	sudo cp build/$(LIBRARY_FILE_LINUX) /usr/lib/
else ifeq ($(PLATFORM_TARGET),windows)
	copy build\\$(LIBRARY_NAME).dll %JAVA_HOME%\\bin
else
	@echo "Unknown platform, not installing."
endif
	@echo "Library installed"

clean:
	@echo "Cleaning build artifacts..."
	rm -rf build
	@echo "Clean complete"

help:
	@echo "NetworkTypeFinder JNI Bridge Makefile"
	@echo ""
	@echo "Targets:"
	@echo "  all (default): Compile Objective-C code to a dynamic library for the current platform"
	@echo "  install: Install the library to system path for the current platform"
	@echo "  clean: Clean build artifacts"
	@echo "  help: Show this help message"
	@echo ""
	@echo "Example usage:"
	@echo "  make all      # Compile the library for your platform"
	@echo "  make install  # Install the library for your platform"

.PHONY: all macos linux windows install clean help
