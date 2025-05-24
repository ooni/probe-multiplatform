# Makefile for NetworkTypeFinder JNI Bridge

# Variables
OBJC_COMPILER = clang
OBJC_FILES = objc/NetworkTypeFinder.m
LIBRARY_NAME = networktypefinder
LIBRARY_FILE_MAC = lib$(LIBRARY_NAME).dylib
LIBRARY_FILE_LINUX = lib$(LIBRARY_NAME).so
MODULE_NAME = NetworkTypeFinder
KOTLIN_CLASS = org.ooni.engine.NetworkTypeFinderJNI
KOTLIN_CLASSES_DIR = ../../build/classes/kotlin/jni/main
JAVA_HOME ?= $(shell java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{print $$3}')

# Default target
all: library

library:
	@echo "Compiling Objective-C code to a dynamic library..."
	@echo "Note: The IDE may show syntax errors for JNI macros, but the code should compile correctly with proper JNI headers"
	@mkdir -p build
	$(OBJC_COMPILER) -dynamiclib -o build/$(LIBRARY_FILE_MAC) $(OBJC_FILES) -framework Foundation -framework SystemConfiguration -framework CoreFoundation -framework Network -framework CFNetwork -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
	@echo "Library created at build/$(LIBRARY_FILE_MAC)"

# Install the library to system path
install: library
	@echo "Installing library to system path..."
ifeq ($(shell uname), Darwin)
	@echo "Installing for macOS..."
	cp build/$(LIBRARY_FILE_MAC) $(HOME)/Library/Java/Extensions
else
	@echo "Installing for Linux..."
	sudo cp build/$(LIBRARY_FILE_LINUX) /usr/lib/
endif
	@echo "Library installed"

clean:
	@echo "Cleaning build artifacts..."
	rm -rf build
	rm -f *.h
	@echo "Clean complete"

# Help target
help:
	@echo "NetworkTypeFinder JNI Bridge Makefile"
	@echo ""
	@echo "Targets:"
	@echo "  all (default): Compile Swift code to a dynamic library"
	@echo "  install: Install the library to system path"
	@echo "  clean: Clean build artifacts"
	@echo "  help: Show this help message"
	@echo ""
	@echo "Example usage:"
	@echo "  make all      # Compile the library"
	@echo "  make install  # Install the library"

.PHONY: all library macos linux header install clean help
