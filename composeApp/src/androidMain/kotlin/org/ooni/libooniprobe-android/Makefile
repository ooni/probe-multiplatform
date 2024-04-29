comma := ,
CLANG_FLAGS := --target=$(TARGET) --sysroot=$(SYSROOT)
export CGO_CFLAGS := $(CLANG_FLAGS) $(subst -mthumb,-marm,$(CFLAGS))
export CGO_LDFLAGS := $(CLANG_FLAGS) $(patsubst -Wl$(comma)--build-id=%,-Wl$(comma)--build-id=none,$(LDFLAGS)) -Wl,-soname=libooniprobe.so

lib-android: $(DESTDIR)/libooniprobe.so

$(DESTDIR)/libooniprobe.so:
	@go build -x -tags linux \
		-v -trimpath \
		-buildvcs=false \
		-o "$(DESTDIR)/libooniprobe.so" -buildmode c-shared

.DELETE_ON_ERROR:
