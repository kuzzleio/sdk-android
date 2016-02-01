[![Build Status](https://api.travis-ci.org/kuzzleio/sdk-android.svg?branch=master)](https://travis-ci.org/kuzzleio/sdk-android) [![codecov.io](http://codecov.io/github/kuzzleio/sdk-android/coverage.svg?branch=master)](http://codecov.io/github/kuzzleio/sdk-android?branch=master)
[ ![Download](https://api.bintray.com/packages/kblondel/maven/kuzzle-sdk-android/images/download.svg) ](https://bintray.com/kblondel/maven/kuzzle-sdk-android/_latestVersion)
[![Join the chat at https://gitter.im/kuzzleio/kuzzle](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kuzzleio/kuzzle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Official Kuzzle Android SDK 
======

This SDK version requires Kuzzle v0.12 or higher.

## About Kuzzle

For UI and linked objects developers, Kuzzle is an open-source solution that handles all the data management (CRUD, real-time storage, search, high-level features, etc).

You can access the Kuzzle repository on [Github](https://github.com/kuzzleio/kuzzle)


## SDK Documentation

The complete SDK documentation is available [here](http://kuzzleio.github.io/sdk-documentation)

## Installation

You can configure your android project to get the Kuzzle's android SDK from jcenter in your build.gradle:

    buildscript {
        repositories {
            jcenter()
        }
    }
    compile 'io.kuzzle:sdk-android:1.3.0'

## Basic usage

```java
Kuzzle kuzzle = new Kuzzle("http://host.url", "index", new ResponseListener<Void>() {
@Override
public void onSuccess(Void object) {
    // Handle success
    KuzzleDocument doc = new KuzzleDocument(dataCollection);
    doc.setContent("foo", "bar").save();
}

@Override
public void onError(JSONObject error) {
    // Handle error
}
});
```

## KuzzleDocument

KuzzleDocument is an encapsulation of a JSONObject.

```java
KuzzleDataCollection myCollection = new KuzzleDataCollection(kuzzle, "myNewCollection");
KuzzleDocument myDocument = new KuzzleDocument(myCollection);
// Add properties to the body
myDocument.setContent("foo", "bar");
// Persist the document
myDocument.save();
// Send it on real time (not persistent)
myDocument.publish();
```

## Adding metadata

As stated [here](http://kuzzleio.github.io/kuzzle-api-documentation/#sending-metadata) you can add metadata to a subscription.

```java
KuzzleOptions options = new KuzzleOptions();
JSONObject metadata = new JSONObject();
metadata.put("foo", "bar");
options.setMetadata(metadata);
myCollection.subscribe(options);
```

# Login

## Prerequisite

To login using kuzzle you need at least one authentication plugin. You can refer [here](https://github.com/kuzzleio/kuzzle-plugin-auth-passport-local) for a local authentication plugin
or [here](https://github.com/kuzzleio/kuzzle-plugin-auth-github) to refer to an OAuth plugin with github.

## Login with local strategy

If you have the kuzzle-plugin-auth-passport-local installed you can login using either the Kuzzle's constructor or the login method.

```java
KuzzleOptions options = new KuzzleOptions();
kuzzle = new Kuzzle("http://localhost:7512", "index", options);
```

## Login with an OAuth strategy

## Javadoc

You can find the java doc of the SDK here: [http://kuzzleio.github.io/sdk-android/](http://kuzzleio.github.io/sdk-android/)
