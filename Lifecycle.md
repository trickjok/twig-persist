## Instances are associated with a single `ObjectDatastore` ##

Whenever you use an `ObjectDatastore` to work with data model instances, Twig must ensure that there is a one-to-one mapping between the instance in memory and an `Entity` in the datastore.  If you run a find command that returns some instances and then later load one of the instances again, Twig will not create a new object.  It will always return the exact same instance it loaded first.

This means that you can safely use the == operator to check that results are the same "entity".

```

// find the white stripes by name
Band stripes = datastore.find()
  .type(Band.class)
  .addFilter("name", EQUALS, "White Stripes")
  .returnUnique()
  .now();

// get jack white
Musician jack = stripes.getMusicians().get(0);

// now try to load the same musician by key name
Musician alsoJack = datastore.load(Musician.class, "Jack White");

// will always be identical because same datastore was used
assert jack == alsoJack;

```

Even if you modify the original instance and then run a new load command the same instance will be returned with your modifications. The fields of the instance will not be refreshed with current data from the datastore (use `refresh(Object)` for this instead).

This guarantee only holds true if you use the same `ObjectDatastore`!  If you load an instance in one `ObjectDatastore` then keep hold of the instance and later use another `ObjectDatastore` to load the same "entity", a **different instance will be returned.**

How does Twig keep this guarantee?  Basically it keeps a Map<Object, Key> in each `ObjectDatastore` so it can always get the same Key every time you do something with a data model instance.

An object can only be `store`d once in a session and must then be `update`d for further changes.  An object loaded in one `ObjectDatastore` cannot be updated in another because it will not know the Key that is associated with that instance.

## Associating external instances with an `ObjectDatastore` ##

If you have a data model instance that was previously used with a different `ObjectDatastore` you will need to associate it with the current `ObjectDatastore`.

```

Musician meg = getMusicianSentWithGwt();

// this will only work because Musican has an @Id defined
datastore.associate(meg);

// this will work even if there is no @Id
Key megsKey = getTheKeyOfTheMusicianSentWithGwt()
datastore.associate(meg, megsKey);
```

The `associate` method examines the instance and tries to create a Key using the field values it finds.  It uses any @Id, @GaeKey or @Parent field values it finds to do this - exactly as it does when it stores an instance.  The created `Key` is then added to the internal Map<Object Key> and from this point on the `ObjectDatastore` behaves exactly as if the instance was loaded or stored with it.

Associating an instance is also common if you send data to a client with RPC (for example using GWT) and then receive data back from the client.  You will need to associate the data instance with the current datastore before you can use it.

If you did not associate the external instance then Twig would not know that the instance is already saved in the datastore.  Normally, Twig will stop you accidentally using `store()` on an instance that is already persistent and will not let you call `update()` on an instance that it thinks is not persistent.  These restrictions help catch errors early by forcing your code to be explicit.

### Associating parent-child relationships ###

When you have parent-child references, associating external data can be a little more tricky.  You first need to associate the parent and then the child.

```
// first associate the parent so we know the Key
datastore.associate(parent);

// the child contains a @Parent field containing the parent
datastore.associate(child);

// now actually update the child
datastore.update(child);
```

But notice that this requires that we have the parent instance as well as the child instance which may not be the case.

Because you are not actually updating the parent - only using it to help create the correct Key for the child - you can get away with only having the parent id.

```
// associate a dummy parent instance that only contains the id
Parent parent = new Parent(parentId);
datastore.associate(parent);

Child child = createChildFromRequest();
child.setParent(parent);

// now the correct Key will be created including the parent Key
datastore.associate(child);
datastore.update(child);

```


## Memory usage ##

When your application no longer references a model object the garbage collector will remove it and the key will also be removed from the `ObjectDatastore`s KeyCache thanks to the use of weak references.  If you are storing or reading a lot of data then the KeyCache might take up too much memory resulting in an OutOfMemoryError.  To avoid this you can call `ObjectDatastore#disassociate(Object)` but beware that if you disassociate an object and it is referenced again a new instance will be created.  They disassociated objects can not be updated.

## No Automatic Dirty Detection ##

There is currently no state kept to determine if an object has been changed.  You must explicitly call `ObjectDatastore#update(Object)` with every altered instance.  A future release will add this ability.