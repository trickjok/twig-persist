## Batching ##

New in 2.0 is the ability to "batch" together many datastore operations (puts and deletes) to do in bulk.  Although this is possible to do at the application level, it is easier to manage when handled at the framework level.

```
ObjectDatastore datastore = ObjectDatastoreFactory.getObjectDatastore();

// remember all operations but do not send to datastore
datastore.startBatchMode();

// do many operations that do not call the datastore
datastore.store(aThing);
datastore.storeAll((lotsOfThings);
datastore.update(aChangedThing);
datastore.delete(anOldThing);

// reads go direct to datastore even while batching.
datastore.find(Thing.class, "myThing");

// flush all the changes to the datastore
datastore.flushBatchedOperations();

// stop batching operations
datastore.stopBatchMode();

// will immediately call datastore
datastore.store(oneMoreThing);

```

If you write a lot of items during batching you might start to run out of memory or find it impossible to flush them all to the datastore due to usage limits.  Then it is useful to set an auto flush limit that will automatically flush to the datastore.

```
datastore.setAutoflushThreshold(500);
for (Thing thing : aMillionThings)
{
  // will write to datastore every writes - there may be more than one entity write per Thing
  datastore.update(thing);
}

```
