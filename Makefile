VERSION = 4.0.0

ROOT_DIR = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
ifeq ($(OS),Windows_NT)
	STATICLIB = .lib
	DYNLIB = .dll
	GOROOT ?= C:/Go
	GOCC ?= $(GOROOT)bin\go
	SEP = \\
	RM = del /Q /F /S
	RRM = rmdir /S /Q
	MV = rename
	CMDSEP = &
	ROOT_DIR_CLEAN = $(subst /,\,$(ROOT_DIR))
	LIB_PREFIX =
else
	STATICLIB = .a
	DYNLIB = .so
	GOROOT ?= /usr/local/go
	GOCC ?= $(GOROOT)/bin/go
	SEP = /
	RM = rm -f
	RRM = rm -f -r
	MV = mv -f
	CMDSEP = ;
	ROOT_DIR_CLEAN = $(ROOT_DIR)
	LIB_PREFIX = lib
endif

PATHSEP = $(strip $(SEP))
ROOTOUTDIR = $(ROOT_DIR)build
JAVAINCLUDE = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
JAVA_HOME =

CXXFLAGS = -g -fPIC -std=c++11 -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)include -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)src -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)include$(PATHSEP) -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build -L.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build
LDFLAGS = -lkuzzlesdk

OBJS = $(SRCS:.cxx=.o)
SRCS = kcore_wrap.cxx
SWIG = swig

all: android

make_java:
	cd sdk-java && $(MAKE)

kcore_wrap.o: kcore_wrap.cxx
	$(CXX) -c $< -o $@ $(CXXFLAGS) $(LDFLAGS) $(JAVAINCLUDE)

makeabifolder:
	mkdir -p $(OUTDIR)/../../../../jniLibs/$(ARCH)

swig:
	$(SWIG) -Wall -c++ -java -package io.kuzzle.sdk -outdir $(OUTDIR) -o $(SRCS) -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)include -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)include$(PATHSEP) -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)src -I.$(PATHSEP)sdk-java$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build$(PATHSEP) $(JAVAINCLUDE) swig/core.i

make_lib:
	$(CXX) -shared kcore_wrap.o -o $(OUTDIR)/../../../../jniLibs/$(ARCH)/$(LIB_PREFIX)kuzzle-wrapper-android$(DYNLIB) $(LDFLAGS)$(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) $(LIBS) $(INCS) $(JAVAINCLUDE)
	strip $(OUTDIR)/../../../../jniLibs/$(ARCH)/$(LIB_PREFIX)kuzzle-wrapper-android$(DYNLIB)

GOFLAGS = -buildmode=c-shared
export GOFLAGS
android: ARCH ?= x86
android: OUTDIR = $(ROOTOUTDIR)/android/app/src/main/java/io/kuzzle/sdk
android: make_java makeabifolder swig $(OBJS) make_lib
	cd build/android/app/src/main/jni && ndk-build && cd - && cd build/android && cp app/src/main/obj/local/$(ARCH)/*.so app/src/main/jniLibs/$(ARCH)/ && bash ./gradlew assemble

clean:
	cd sdk-java && $(MAKE) clean
	$(RRM) build/android/app/build
	$(RRM) build/android/app/src/main/java
	$(RRM) build/android/app/src/main/jniLibs/
	$(RRM) build/android/build

.PHONY: all makeabifolder swig make_lib android clean