%include "std_string.i"
%include "std_vector.i"

%include "common.i"
%include "exceptions.i"
%include "typemap.i"
%include "kcore.i"
%include "json_wrap/kuzzle.i"
%include "json_wrap/listeners.i"
%include "json_wrap/document.i"
%include "json_wrap/realtime.i"
%include "json_wrap/server.i"
%include "json_wrap/collection.i"
%include "json_wrap/auth.i"

typedef long long time_t;

%template(StringVector) std::vector<std::string>;
%typemap(out) const StringVector& %{
    return $1;
%}

%template(UserRightVector) std::vector<kuzzleio::user_right*>;
%typemap(out) const UserRightVector& %{
    return $1;
%}

%pragma(java) jniclasscode=%{
  static {
    try {
      System.loadLibrary("kuzzle-wrapper-java");
    } catch (UnsatisfiedLinkError e) {
      try {
        java.io.InputStream inputStream = kuzzlesdk.class.getResourceAsStream("/libkuzzle-wrapper-java.so");
        java.nio.file.Path path = java.nio.file.FileSystems.getDefault().getPath("").toAbsolutePath();
        String sharedObject = path.toString() + "/libs/libkuzzle-wrapper-java.so";

        try {
          java.io.File folder = new java.io.File(path.toString() + "/libs/");
          folder.mkdir();
        } catch(Exception ee) {}

        java.io.OutputStream outputStream = new java.io.FileOutputStream(new java.io.File(sharedObject));

        int read = 0;
        byte[] bytes = new byte[1024];

        while ((read = inputStream.read(bytes)) != -1) {
          outputStream.write(bytes, 0, read);
        }

        System.load(path.toString() + "/libs/libkuzzle-wrapper-java.so");
      } catch (Exception ex) {
        System.err.println("Native code library failed to load. \n");
        ex.printStackTrace();
        System.exit(1);
      }
    }
  }
%}

%extend kuzzleio::kuzzle_response {
    ~kuzzle_response() {
        kuzzle_free_kuzzle_response($self);
    }
}

%include "options.cpp"
%include "websocket.cpp"
%include "kuzzle.cpp"
%include "collection.cpp"
%include "document.cpp"
%include "realtime.cpp"
%include "auth.cpp"
%include "index.cpp"
%include "server.cpp"
%include "search_result.cpp"
%include "default_constructors.cpp"
