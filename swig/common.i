%rename(User, match="class") kuzzle_user;
%rename(TokenValidity) token_validity;
%rename(AckResponse) ack_response;
%rename(queueTTL) queue_ttl;
%rename(QueryOptions) query_options;
%rename(JsonObject) json_object;
%rename(JsonResult) json_result;
%rename(LoginResult) login_result;
%rename(BoolResult) bool_result;
%rename(Statistics) statistics;
%rename(AllStatisticsResult) all_statistics_result;
%rename(StatisticsResult) statistics_result;
%rename(CollectionsList) collection_entry;
%rename(CollectionsListResult) collection_entry_result;
%rename(StringArrayResult) string_array_result;
%rename(KuzzleResponse) kuzzle_response;
%rename(KuzzleRequest) kuzzle_request;
%rename(ShardsResult) shards_result;
%rename(DateResult) date_result;
%rename(UserData) user_data;
# %rename(User, match="class") user;
%rename(SearchFilters) search_filters;
# %rename(SearchResult) search_result;
%rename(NotificationResult) notification_result;
%rename(NotificationContent) notification_content;
%rename(SubscribeToSelf) subscribe_to_self;
%rename(ValidationResponse) validation_response;
%rename(UserRight) user_right;
%rename(Options, match="class") s_options;
%rename(RoomOptions, match="class") s_room_options;
%rename(QueryOptions, match="class") s_query_options;

// struct options
%rename(queueMaxSize) queue_max_size;
%rename(offlineMode) offline_mode;
%rename(autoQueue) auto_queue;
%rename(autoReconnect) auto_reconnect;
%rename(autoReplay) auto_replay;
%rename(autoReplay) auto_replay;
%rename(autoResubscribe) auto_resubscribe;
%rename(reconnectionDelay) reconnection_delay;
%rename(replayInterval) replay_interval;

// struct query_options
%rename(scrollId) scroll_id;
%rename(ifExist) if_exist;
%rename(retryOnConflict) retry_on_conflict;

// struct kuzzle_request
%rename(membersLength) members_length;
%rename(keysLength) keys_length;
%rename(fieldsLength) fields_length;

// struct role
%rename(profileIds) profile_ids;
%rename(profileIdsLength) profile_ids_length;

// struct notification_result
%rename(nType) n_type;
%rename(roomId) room_id;

// struct statistics
%rename(completedRequests) completed_requests;
%rename(failedRequests) failed_requests;
%rename(ongoingRequests) ongoing_requests;

// struct token_validity
%rename(expiresAt) expires_at;

// struct kuzzle_response
%rename(requestId) request_id;
%rename(roomId) room_id;

%rename(delete) delete_;

%rename(_auth, match="class") auth;
%rename(_kuzzle, match="class") kuzzle;
%rename(_realtime, match="class") realtime;
%rename(_collection, match="class") collection;
%rename(_document, match="class") document;
%rename(_server, match="class") server;

%ignore *::error;
%ignore *::stack;
%ignore *_result::status;
%ignore *_response::status;
%ignore token_validity::status;

%include "std_string.i"

%{
#include "websocket.cpp"
#include "search_result.cpp"
#include "collection.cpp"
#include "auth.cpp"
#include "index.cpp"
#include "server.cpp"
#include "document.cpp"
#include "default_constructors.cpp"
%}

%ignore getListener;
%ignore getListeners;

%include "std_function.i"
%std_function(NotificationListener, void, onMessage, kuzzleio::notification_result*);
%std_function(EventListener, void, trigger, const std::string);

%{
#include "kuzzle.cpp"
#include "realtime.cpp"
%}
