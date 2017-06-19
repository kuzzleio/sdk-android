[![Build Status](https://api.travis-ci.org/kuzzleio/sdk-android.svg?branch=master)](https://travis-ci.org/kuzzleio/sdk-android) [![codecov.io](http://codecov.io/github/kuzzleio/sdk-android/coverage.svg?branch=master)](http://codecov.io/github/kuzzleio/sdk-android?branch=master)
[ ![Download](https://api.bintray.com/packages/kblondel/maven/kuzzle-sdk-android/images/download.svg) ](https://bintray.com/kblondel/maven/kuzzle-sdk-android/_latestVersion)
[![Join the chat at https://gitter.im/kuzzleio/kuzzle](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kuzzleio/kuzzle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Official Kuzzle Android SDK
======

## About Kuzzle

A backend software, self-hostable and ready to use to power modern apps.

You can access the Kuzzle repository on [Github](https://github.com/kuzzleio/kuzzle)

## SDK Documentation

The complete SDK documentation is available [here](http://docs.kuzzle.io/sdk-reference)

## Report an issue

Use following meta repository to report issues on SDK:

https://github.com/kuzzleio/kuzzle-sdk/issues

## Installation

You can configure your Android project to get Kuzzle's Android SDK from jcenter in your build.gradle:

    buildscript {
        repositories {
            maven {
                url  "http://dl.bintray.com/kblondel/maven"
            }
            jcenter()
        }

    }

    dependencies {
        compile 'io.kuzzle:sdk-android:2.2.0'
    }

## Basic usage

```java
Kuzzle kuzzle = new Kuzzle("host", new ResponseListener<Void>() {
    @Override
    public void onSuccess(Void object) {
        // Handle success
        Document doc = new Document(dataCollection);
        try {
            doc.setContent("foo", "bar").save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(JSONObject error) {
        // Handle error
    }
});
```

## Document

Document is an encapsulation of a JSONObject.

```java
Collection myCollection = new Collection(kuzzle, "myNewCollection", "myNewIndex");
Document myDocument = new Document(myCollection);

// Add properties to the body
myDocument.setContent("foo", "bar");

// Persist the document
myDocument.save();

// Send it on real time (not persistent)
myDocument.publish();
```

## Adding volatile data

As stated [here](http://docs.kuzzle.io/api-documentation/volatile-data/) you can add volatile data to a subscription.

```java
RoomOptions options = new RoomOptions();
JSONObject volatileData = new JSONObject();

volatileData.put("foo", "bar");
options.setVolatile(volatileData);
kuzzle.collection("foo", "test").subscribe(options, new ResponseListener<NotificationResponse>() {
    @Override
    public void onSuccess(NotificationResponse response) {
        
    }

    @Override
    public void onError(JSONObject error) {

    }
});
```

# Login

## Prerequisite

To login using Kuzzle you need at least one authentication plugin. You can refer [here](https://github.com/kuzzleio/kuzzle-plugin-auth-passport-local) for a local authentication plugin
or [here](https://github.com/kuzzleio/kuzzle-plugin-auth-passport-oauth) to refer to our OAuth2 plugin.

To know more about how to log in with a Kuzzle SDK, please refer to our [documentation](http://docs.kuzzle.io/sdk-reference/kuzzle/login/)

If you have the kuzzle-plugin-auth-passport-local installed you can login using either the Kuzzle's constructor or the login method.

### Login with an OAuth strategy

If you have an OAUTH plugin like kuzzle-plugin-auth-passport-oauth, you may use the KuzzleWebViewClient class to handle the second authentication phase:

```java
Handler handler = new Handler();
WebView webView = (WebView) findViewById(R.id.webView);
webView.setWebViewClient(kuzzle.getKuzzleWebViewClient());
kuzzle.login("github", new KuzzleResponseListener<JSONObject>() {
      @Override
      public void onSuccess(final JSONObject object) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            try {
              if (object.has("headers")) {
                webView.loadUrl(object.getJSONObject("headers").getString("Location"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("error", error.toString());
      }
    });
```
