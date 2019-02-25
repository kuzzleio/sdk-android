%module(directors="1") kuzzlesdk
%{
#include "internal/options.hpp"
#include "protocol.hpp"
#include "websocket.hpp"
#include "internal/exceptions.hpp"
#include "kuzzle.hpp"
#include "internal/search_result.hpp"
#include "internal/collection.hpp"
#include "internal/index.hpp"
#include "internal/server.hpp"
#include "internal/document.hpp"
#include "internal/realtime.hpp"
#include "internal/auth.hpp"
#include <assert.h>
%}

%define _Complex
%enddef

%include "internal/options.hpp"
%include "internal/event_emitter.hpp"
%include "protocol.hpp"
%include "websocket.hpp"
%include "internal/kuzzle_structs.h"
%include "kuzzle.h"
%include "internal/exceptions.hpp"
%include "kuzzle.hpp"
%include "internal/search_result.hpp"
%include "internal/collection.hpp"
%include "internal/index.hpp"
%include "internal/server.hpp"
%include "internal/document.hpp"
%include "internal/realtime.hpp"
%include "internal/auth.hpp"