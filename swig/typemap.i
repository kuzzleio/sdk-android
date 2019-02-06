%rename (_now) kuzzleio::Server::now(query_options*);
%rename (_now) kuzzleio::Server::now();

%javamethodmodifiers kuzzleio::Server::now(query_options* options) "private";
%javamethodmodifiers kuzzleio::Server::now() "private";
%typemap(javacode) kuzzleio::Server %{
  /**
   * {@link #now(QueryOptions)}
   * @return java.util.Date
   */
  public java.util.Date now() {
    long res = _now();

    return new java.util.Date(res);
  }

  /**
   * Returns the current Kuzzle UTC timestamp
   *
   * @param options - Request options
   * @return a DateResult
   */
  public java.util.Date now(QueryOptions options) {
    long res = _now(options);

    return new java.util.Date(res);
  }
%}

%typemap(jni) std::string* "jobject"
%typemap(jtype) std::string* "java.lang.String"
%typemap(jstype) std::string* "java.lang.String"
%typemap(javain) std::string* "$javainput"
%typemap(in) std::string * (std::string strTemp) {
  jobject oInput = $input;
  if (oInput != NULL) {
    jstring sInput = static_cast<jstring>( oInput );

    const char * $1_pstr = (const char *)jenv->GetStringUTFChars(sInput, 0);
    if (!$1_pstr) return $null;
    strTemp.assign( $1_pstr );
    jenv->ReleaseStringUTFChars( sInput, $1_pstr);
    $1 = &strTemp;
  } else {
    $1 = NULL;
  }
}
%apply std::string * { std::string* };


// const char** to String[]

%typemap(jni) const char**, const char * const * "jobjectArray";
%typemap(jtype) const char**, const char * const * "String[]";
%typemap(jstype) const char**, const char * const * "String[]";

%typemap(in) const char**, const char * const * %{
  jobject fu = (jobject) jenv->GetObjectArrayElement($input, 0);

  size_t size = jenv->GetArrayLength($input);
  int i = 0;
  char **res = (char**)malloc(sizeof(*res) * size + 1);
  while(i < size) {
    jobject fu = (jobject) jenv->GetObjectArrayElement($input, i);
    res[i] = (char *)jenv->GetStringUTFChars(static_cast<jstring>(fu), 0);
    i++;
  }
  res[i] = NULL;
  $1 = res;
%}

%typemap(out) const char**, const char * const *  {
  size_t count = 0;
  const char **pos = const_cast<const char**>($1);
  while(pos[++count]);

  $result = JCALL3(NewObjectArray, jenv, count, JCALL1(FindClass, jenv, "java/lang/String"), NULL);
  size_t idx = 0;
  while (*pos) {
    jobject str = JCALL1(NewStringUTF, jenv, *pos);
    assert(idx < count);
    JCALL3(SetObjectArrayElement, jenv, $result, idx++, str);
    *pos++;
  }
}

%typemap(javain) const char**, const char * const * "$javainput"
%typemap(javaout) const char**, const char * const * {
  return $jnicall;
}

// std::vector<user_right*> to UserRight[]

%typemap(jni) std::vector<kuzzleio::user_right*> "jobjectArray";
%typemap(jtype) std::vector<kuzzleio::user_right*> "UserRight[]";
%typemap(jstype) std::vector<kuzzleio::user_right*> "UserRight[]";

%typemap(in) std::vector<kuzzleio::user_right*> %{
  size_t size = jenv->GetArrayLength($input);
  int i = 0;
  std::vector<kuzzleio::user_right*> user_rights;

  while(i < size) {
    jobject user_right = (jobject) jenv->GetObjectArrayElement($input, i);
    user_rights.push_back((user_right*) user_right);
    i++;
  }
  res[i] = NULL;
  $1 = res;
%}

%typemap(out) std::vector<kuzzleio::user_right*> {
  std::vector<kuzzleio::user_right*> user_rights = $1;

  $result = JCALL3(NewObjectArray, jenv, user_rights.size(), JCALL1(FindClass, jenv, "io/kuzzle/sdk/UserRight"), NULL);

  jclass user_right_class = jenv->FindClass("io/kuzzle/sdk/UserRight");
  jmethodID constructor = jenv->GetMethodID(user_right_class, "<init>", "()V");
  jobject user_right = jenv->NewObject(user_right_class, constructor);

  for (size_t i = 0; i < user_rights.size(); ++i) {
    // Set controller
    jmethodID setCtrl = jenv->GetMethodID(user_right_class, "setController", "(Ljava/lang/String;)V");
    jstring ctrl_string = JCALL1(NewStringUTF, jenv, user_rights[i]->controller);
    jenv->CallVoidMethod(user_right, setCtrl, ctrl_string);

    // Set action
    jmethodID setAction = jenv->GetMethodID(user_right_class, "setAction", "(Ljava/lang/String;)V");
    jstring action_string = JCALL1(NewStringUTF, jenv, user_rights[i]->action);
    jenv->CallVoidMethod(user_right, setAction, action_string);

    // Set index
    jmethodID setIndex = jenv->GetMethodID(user_right_class, "setIndex", "(Ljava/lang/String;)V");
    jstring index_string = JCALL1(NewStringUTF, jenv, user_rights[i]->index);
    jenv->CallVoidMethod(user_right, setIndex, index_string);

    // Set collection
    jmethodID setCollection = jenv->GetMethodID(user_right_class, "setCollection", "(Ljava/lang/String;)V");
    jstring collection_string = JCALL1(NewStringUTF, jenv, user_rights[i]->collection);
    jenv->CallVoidMethod(user_right, setCollection, collection_string);

    // Set value
    jmethodID setValue = jenv->GetMethodID(user_right_class, "setValue", "(Ljava/lang/String;)V");
    jstring value_string = JCALL1(NewStringUTF, jenv, user_rights[i]->value);
    jenv->CallVoidMethod(user_right, setValue, value_string);

    JCALL3(SetObjectArrayElement, jenv, $result, i, user_right);
  }
}

%typemap(javain) std::vector<kuzzleio::user_right*> "$javainput"
%typemap(javaout) std::vector<kuzzleio::user_right*> {
  return $jnicall;
}
