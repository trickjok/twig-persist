## Instances are associated with an ObjectDatastore ##

Whenever you store or load an object Twig must ensure that only a single instance is ever kept in memory for the current "session".  This behaviour is a responsibility of StrategyObjectDatastore which keeps maps of Key->Object and Object->Key to maintain this one-one Key-Object relationship.  This rule only holds true within the same ObjectDatastore instance so the same entity loaded from different ObjectDatastores will not be identical.

```
// definition of reference maps in KeyCache uses weak values and no concurrency

private Map<Key, Object> cacheByKey = new MapMaker()
	.weakValues()
	.concurrencyLevel(1)
	.makeMap();

private Map<Object, ObjectReference<Key>> cacheByValue = new MapMaker()
	.weakKeys()
	.concurrencyLevel(1)
	.makeMap();
```

An object can only be `store`d once in a session and must then be `update`d for further changes.  An object loaded in one session cannot be updated in another because it will not exist in the second sessions KeyCache and so Twig cannot be sure that the same Key would be used.

If you do want to explicitly associate an object loaded from a different session with a new ObjectDatastore instance you can use the `ObjectDatastore#associate(Object)` or `ObjectDatastore#associate(Object,Key)` methods.  This instance will then not need to be loaded from the datastore when its Key is found in a loaded entity and the instance will be able to be updated using the new session.

If an instance is stored in more than once in different ObjectDatastore's then Twig will try to store the object again which will result in either an exception (if the key is fully specified) or more than one Entity being stored.

## Memory usage ##

When your application no longer references a model object the garbage collector will remove it and the key will also be removed from the `ObjectDatastore`s KeyCache thanks to the use of weak references.  If you are storing or reading a lot of data then the KeyCache might take up too much memory resulting in an OutOfMemoryError.  To avoid this you can call `ObjectDatastore#disassociate(Object)` but beware that if you disassociate an object and it is referenced again a new instance will be created.  They disassociated objects can not be updated.

## No Automatic Dirty Detection ##

There is currently no state kept to determine if an object has been changed.  You must explicitly call `ObjectDatastore#update(Object)` with every altered instance.  A future release will add this ability.