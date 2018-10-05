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

CXXFLAGS = -g -fPIC -std=c++11 -I.$(PATHSEP)sdk-cpp$(PATHSEP)include -I.$(PATHSEP)sdk-cpp$(PATHSEP)src -I.$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)include$(PATHSEP) -I.$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build -L.$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build
LDFLAGS = -lkuzzlesdk

OBJS = $(SRCS:.cxx=.o)
SRCS = kcore_wrap.cxx
SWIG = swig
STRIP ?= strip

all: android

make_cpp:
	cd sdk-cpp && $(MAKE)

kcore_wrap.o: kcore_wrap.cxx
	$(CXX) -c $< -o $@ $(CXXFLAGS) $(LDFLAGS) $(JAVAINCLUDE)

makeabifolder:
	mkdir -p $(OUTDIR)/../../../../jniLibs/$(ARCH)

swig:
	$(SWIG) -Wall -c++ -java -package io.kuzzle.sdk -outdir $(OUTDIR) -o $(SRCS) -I.$(PATHSEP)sdk-cpp$(PATHSEP)include -I.$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)include$(PATHSEP) -I.$(PATHSEP)sdk-cpp$(PATHSEP)src -I.$(PATHSEP)sdk-cpp$(PATHSEP)sdk-c$(PATHSEP)build$(PATHSEP) $(JAVAINCLUDE) swig/core.i

make_lib:
	$(CXX) -shared kcore_wrap.o -o $(OUTDIR)/../../../../jniLibs/$(ARCH)/$(LIB_PREFIX)kuzzle-wrapper-android$(DYNLIB) $(LDFLAGS)$(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) $(LIBS) $(INCS) $(JAVAINCLUDE)
	$(STRIP) $(OUTDIR)/../../../../jniLibs/$(ARCH)/$(LIB_PREFIX)kuzzle-wrapper-android$(DYNLIB)

android: OUTDIR = $(ROOTOUTDIR)/android/app/src/main/java/io/kuzzle/sdk
android: make_cpp makeabifolder swig $(OBJS) make_lib
	cd $(ROOTOUTDIR)/android/app/src/main/jni && ndk-build && cd - && cd $(ROOTOUTDIR)/android && cp app/src/main/obj/local/$(ARCH)/*.so app/src/main/jniLibs/$(ARCH)/

package:
	 cd $(ROOTOUTDIR)/android && gradle assemble

clean:
	cd sdk-cpp && $(MAKE) clean
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP)app$(PATHSEP)build
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP).gradle
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP)app$(PATHSEP)src$(PATHSEP)main$(PATHSEP)java
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP)app$(PATHSEP)src$(PATHSEP)main$(PATHSEP)jniLibs
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP)app$(PATHSEP)src$(PATHSEP)main$(PATHSEP)libs
	$(RRM) $(ROOTOUTDIR)$(PATHSEP)android$(PATHSEP)app$(PATHSEP)src$(PATHSEP)main$(PATHSEP)obj
	$(RRM) $(ROOT_DIR)kcore_wrap.{cxx,h,o}

.PHONY: all makeabifolder swig make_lib android clean
