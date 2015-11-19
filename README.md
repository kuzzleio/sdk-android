Kuzzle
======

## About Kuzzle

For UI and linked objects developers, Kuzzle is an open-source solution that handles all the data management (CRUD, real-time storage, search, high-level features, etc).

You can access the Kuzzle repository on [Github](https://github.com/kuzzleio/kuzzle)


## SDK Documentation

The complete SDK documentation is available [here](http://kuzzleio.github.io/sdk-documentation)

## Basic usage

    Kuzzle kuzzle = new Kuzzle("http://host.url", new ResponseListener() {
    @Override
    public void onSuccess(JSONObject object) throws Exception {
        // Handle success
        JSONObject doc = new JSONObject();
        doc.put("name", "bar");
        kuzzle.dataCollectionFactory("foo")
              .createDocument(doc);
    }
    
    @Override
    public void onError(JSONObject error) throws Exception {
        // Handle error
    }
    });

## With KuzzleDocument

KuzzleDocument is an encapsulation of a JSONObject.

    KuzzleDataCollection myCollection = new KuzzleDataCollection(kuzzle, "myNewCollection");
    KuzzleDocument myDocument = new KuzzleDocument(myCollection);
    // Add properties to the body
    myDocument.setContent("foo", "bar");
    // Persist the document
    myDocument.save();
    // Send it on real time (non persistent)
    myDocument.publish();
    
## Adding metadata

As stated [here](https://github.com/kuzzleio/kuzzle/blob/master/docs/API.WebSocket.md#sending-metadata) you can add metadata to a subscription.

    KuzzleOptions options = new KuzzleOptions();
    JSONObject metadata = new JSONObject();
    metadata.put("foo", "bar");
    options.setMetadata(metadata);
    myCollection.subscribe(options);
    

## Javadoc

You can find the java doc of the SDK here: [http://kuzzleio.github.io/sdk-android/](http://kuzzleio.github.io/sdk-android/)