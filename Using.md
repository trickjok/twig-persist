# Using an Object Datastore #

There are six basic data commands which you use to manage persistent state in your application: store, load, find, update, refresh and delete.  Store and Find have [fluent interfaces](http://en.wikipedia.org/wiki/Fluent_interface) which take no parameters and offer a more flexible way to define a command.

## Creating an ObjectDatastore ##

You directly instantiate an `ObjectDatastore` instance with the `new` operator which gives you the ability to choose your implementation or quickly override extension points with an anonymous subclass.  Normally your implementation would be created by a dependency injection library like Guice.

It is recommended you start with the convenient `AnnotationObjectDatastore` implementation which sets up an `AnnotationStrategy` to easily configure your data model.

```
// this should happen in a factory method or Guice `Provider` 
ObjectDatastore datastore = new AnnotationObjectDatastore();

// convenience constructor which sets all fields to un-indexed by default - a good default
ObjectDatastore datastore = new AnnotationObjectDatastore(false);

// as above but sets the default data version to 2 -  see Versioned Types for details
ObjectDatastore datastore = new AnnotationObjectDatastore(false, 2);
```

Remember that these are just convenience constructors and you can always use you own [customised Strategy](Configuration#Override_defaults.md)

ObjectDatastores are stateful non-thread-safe and cheap to create so you should not share them between requests.  If you are using Guice you could declare a binding using the default scope like this:

```
// simple un-customised object datastore
binder.bind(ObjectDatastore.class).to(AnnotationObjectDatastore.class);
```

## Parallel Asynchronous Commands ##
<table cellpadding='5' border='0'>
<blockquote><tr><td valign='top'>
<img src='http://wiki.twig-persist.googlecode.com/hg/images/twiggy_sad.jpg' />
</td><td cellpadding='5' valign='top'>
The underlying App Engine has the ability to run every RPC call either synchronously (blocking) or asynchronously (non-blocking). The standard datastore interfaces, JDO and JPA, were not designed with the concept of non-blocking operations so this has not yet been exposed in the public API.  Twig taps in to this potential to enable <code>store</code> and <code>find</code> commands to start executing in the background and return control immediately to your code.  The same request can then continue executing more parallel commands or any other work while it waits for the results.</blockquote>

This ability is very useful in a cloud environment in which Threads are not allowed and processing time is restricted to 30 seconds.<br>
<br>
The examples in this section use the standard blocking <code>returnResultsNow()</code> type methods for clarity but keep in mind that both command interfaces support <code>returnResultsLater()</code> type methods that return a <code>Future&lt;T&gt;</code>.<br>
<br>
The Future object is like a handle to get the results of you command when you are ready.  Just call <code>get()</code> and the results will be returned immediately if the command is finished otherwise it will block.  If any problems occurred during execution they will be thrown when you call get() wrapped in an <code>ExecutionException</code>.<br>
</td></tr></table>

## Storing Instances ##

You should already have configured you data model, now it is time to persist some instances.  You can store data in parallel or serially, in bulk or item by item.

```
// storing a single instance 
Band ledzep = createClassicRockBand();

// convenience store method
Key key = datastore.store(ledzep);

// more flexible fluent method
Key key = datastore.store().instance(ledzep).returnKeyNow();
```

`Key`s are not often used within Twig code but are useful to send to a web-page if you have not defined your own `@Key` field.

```
// bulk store
Collection<Band> bands = createAllBands();

// convenience method
Map<Band, Key> bandKeys = datastore.storeAll(bands);
  
// fluent method
Map<Band, Key> bandKeys = datastore.store().instances(bands).returnKeysNow();
```

Both these methods will store all referenced instances including all band members and albums. However, if band member was already stored in the datastore (i.e. it is persistent) it will not be updated.  Every persistent instance must be explicitly updated with a call to update(). This is because Twig is not able to automatically detect when an instance has been changed - this would require byte-code enhancement.  But it is easy for Twig to know which instances are not already persistent - they are not present in the internal Key->Instance cache.

So, in effect, `store` is cascaded to referenced instances but `update` is not.

All of the directly stored instances are `put` in a single operation however the referenced Musicians and Albums are stored separately and their Keys are not included in the returned Map.  To also include all the referenced instances in the same bulk datastore `put` and also the returned key map set "batchRelated"

```
// do a single bulk put and return all keys created
Map<Band, Key> bandKeys = datastore.store()
  .instances(bands)
  .batchRelated()
  .returnKeysNow();
```

Any instances that are already stored will not be stored again and will not return a Key

The store methods can also run non-blocking by calling returnKeysLater() which is covered in [Parallel Asynchronous Commands](Parallel.md)

The datastore has no "unique constraints" to ensure you do not save two instances with the same value.  Using auto-generated long keys by setting no key yourself guarantees you will not accidentally overwrite an existing entity but if you create your own keys you can make Twig check that the key does not exist before you set it.  This should be done within a transaction and will work for storing single or multiple instances.

```
Key bandKey = datastore.store()
  .instance(aNewBand)
  .ensureUniqueKey()
  .returnKeyNow();
```


## Loading By Key ##

The term _key_ can refer to either a low-level `Key` which contains the entire ancestor chain, kind and app id in an object that can be encoded as a String - or it can refer to you data models defined @Key field which might be a long or

```
Band beasties = datastore.load(Band.class, "Beastie boys");
```

### Activation Depth ###

By default every referenced instance is loaded which could end up loading far more data than you intend. To control which fields are loaded use `ObjectDatastore.setActivationDepth(int)`

```
// just load the first instances and their fields  
datastore.setActivationDepth(2);
Band underground = datastore.load(Band.class, "Velvet Underground");

// the Musician instance is created but not activated - its fields are not loaded
Musician lewis = underground.members.get(0);
assertNull(lewis.birthday);

// now activate the instance which loads all its fields to the activation depth 
datastore.refresh(lewis);
  
assertNotNull(lewis.birdthday);  
```

Activation depth also applies to instances retrieved with `find` commands.  You can `refresh` an instance at any time to read a fresh copy from the datastore.  If you use `load` or `find` commands to retrieve the same entity the identical _instance_ will be returned to you with any modifications you might have made since it was loaded.

You can control activation depth on a field by field basis or per class using annotations or in code using the `ActivationStrategy`.

## Finding Instances ##

Unless you know the key of the item you want to load you need to use a find command to search the datastore. Like `store` there is a fluent style 'find()` command which allows you finer control over the command and there are several convenience find methods for common cases.

### Single Query Commands ###

```
// convenience method to load all instances of a type
Iterator<Band> bands = datastore.find(Band.class);  

// the equivalent with a fluent find command
Iterator<Band> bands = datastore.find().type(Band.class).returnResultsNow();
  
// find all long hair musicians working for Sony but skip the first 4 sorted by name now!
Iterator<Musican> glamRockers = datastore.find().type(Musician.class)
  .addSort("name")
  .addFilter("hair", EQUAL, Hair.LONG_LIKE_A_GIRL)
  .withAncestor(sonyMusic)
  .startFrom(4)
  .returnResultsNow();
```

Note that all commands can also be run asynchronously using getResultsLater() as described in [Parallel Asynchronous Commands](Parallel.md)

### Multiple Query Commands ###

Datastore queries can only return contiguous results from an index which forces many useful operators such as OR, IN and NOT\_EQUAL to be broken down into multiple queries.  The low level datastore breaks down IN and NOT\_EQUAL for you but for OR you are left to your own devices.  Twig introduces support for OR queries using _multiple query commands_ which execute in parallel (using async commands) and then the results are merged together into a single iterator.  If you have a sort order on your query that is also respected across all the child queries and duplicate results are removed automatically!  This saves you some serious coding for what is a very common search requirement.

Add child queries using `.addChildQuery()` which creates a new query that inherits all of the parent queries filters and other options.

```
// find musicians with floppy hair born before 1950 OR with a name that begins with M
Iterator<Musican> glamRockers = datastore.find().type(Musician.class)
  .addFilter("hair", EQUAL, Hair.UNKEMPT_FLOPPY)
  .addChildQuery()
    .addFilter("born", LESS_THAN, 1950)
  .addChildQuery()
    .addFilter("name", GREATER_THAN, "M")
    .addFilter("name", LESS_THAN, "O")  // same property so ok
    .returnResultsNow();  
```

Note that the above command executes _two_ queries which both contain the inherited hair filter.  It is only possible to apply a sort to the `RootFindCommand` and not to the child `BranchFindCommand`s

Merged AND queries are not currently supported but will be in a future release.  For example to find Mick Jagger you would need to run the above query but AND merge the results.

### Relation Index Entities ###

Using multi-valued property lists is a great way to store relationships directly in an Entity but can make your Entities bloated and large.  Every time the Entity is read the entire list of related Keys is also read which could be large.  This problem is described in Bret Slatkin's talk [Building Scalable, Complex Apps on App Engine](http://code.google.com/events/io/2009/sessions/BuildingScalableComplexApps.html) and he suggests a work around called a "Relation Entity Index" which breaks the original Entity up into a parent which just has the data you want and a child that contains the list of Keys (or other values) that never needs to be read.

His message fan-out example would be coded in Twig like this

```
class Message
{
  String sender;
  @Type(Text.class) String body;  // needs to hold over 500 chars
}

class MessageIndex
{
  List<String> receivers;
}

// create a message
Message message = new Message()
message.sender = "me";
message.body = "very long text...";
  
// create the index
MessageIndex index = new MessageIndex()
index.receivers = Arrays.asList("janet", "jimmmy", "keith");
  
// store both and set relationship
datastore.store(message);
datastore.store(index, message);  // index is stored with *message as parent*
  
// finding a message by receiver      
Iterator<Message> = datastore.find()
  .type(MessageIndex.class)   // search for the index
  .addFilter("receiver", EQUAL, "jimmy")  
  .<Message>returnParentsNow();  // query for parents keys only and fetch instances
```

Searching this way means that the potentially long list of receivers is never loaded.  This is a great technique for geospatial or full-text searches that need to associate a lot of indexed values for each record.

### Why return Iterator and not List or Iterable? ###

Find commands return an Iterator which can be easily converted into any Collection type if required e.g. using Google Collections it is as simple as `List<Band> list = Lists.newArrayList(bands)`. The low-level datastore can return an Iterable which is convenient to use in `for-each` loops but actually _executes the same query again_ every time it is used.  This is not always obvious but could slow you application significantly.  Another option is returning a List which will contain all items - even if there are thousands.  Often you have a lot of data but only want to display the first page for example.  Using an Iterator gives you the best of both worlds - access to all the results if required but only loading as many as you are expecting.  If you definitely want to load all results in one call then set `.fetchResultsBy(Integer.MAX_VALUE)`.  If you explicitly convert the Iterator to a Collection type then the query will only be run once.

### Continuing from a cursor ###

All of the `Iterator`s shown above are `QueryResultIterator`s which have a getCursor() method.  You can store this cursor and use it to continue your query using `.continueFrom(Cursor)`

```
QueryResultIterator<Band> bands = datastore.find(Band.class);

// work with bands until we are almost out of time
  
Cursor cursor = bands.getCursor();
  
String encoded = cursor.toWebsafeString();
  
// later - perhaps in another task - continue what we were doing
Cursor cursor = Cursor.fromWebsafeString(encoded);
  
QueryResultIterator<Band> bands =   
    datastore.find()
        .type(Band.class)
        .continueFrom(cursor)
        .returnResultsNow();
```

You cannot user cursors if you have merged multiple commands or if your query contains NOT\_EQUAL or IN (these are converted to multiple commands by the low-level datastore)

## Update Existing Instance ##

After loading an instance and modifying its fields you need to call update(Object) to store the changes.  Unlike JDO there is no automatic dirty detection so it is up to your code to manage this. For every modified instance, you must explicitly call `update(Object)` or `updateAll(collection<?>)`

```
ledzep.albums..copiesSold(housesOfTheHoly);
datastore.update(ledzep);
```

The new album will be stored automatically because `store` operations cascade to referenced instances.  If, however, we modified an existing album:

```
ledzep.albums.get(0).copiesSold = 500000;

// changed album not updated!
datastore.update(ledzep);
```

the change will not be persisted because Twig is not able to detect that the Album was changed.  You must explicitly update the album:

```
ledzep.albums.get(0).copiesSold = 500000;
datastore.update(ledzep.albums.get(0));
```

Note that if you send an instance to the client (e.g. by GWT RPC) then modify it and send it back to the server to update, you will need to associate the new instance with the new `ObjectDatastore` like this:

```
Band altered = getBandFromGwtCommand();
datastore.associate(altered);
datastore.update(altered);
```

The call to `associate()` has the effect of reading the key field of the altered instance and putting it in the `ObjectDatastore`s `KeyCache` with the instance.  It is just as if you had previously loaded the instance from the new datastore so now it is safe to call update().

Having distinct `update` and `store` methods makes it harder to accidentally create a new instance when you intend to modify an existing instance.  It is good to make that intention clear in your code to catch errors early.  Store always creates a new key and may require the parent instance whereas update may need to look up an existing key when no key field is defined on your model.  An exception will be thrown if you try to update an instance that was not loaded from the datastore and store will complain if the opposite is true.

The store command can be configured to check for existing keys to ensure you don't clobber an existing entity which does not make sense for update.  For these reasons `store` and `update` should be different methods as they are in SQL.

## Deleting ##

```
datastore.delete(prodigy);  // they keep on lighting fires
  
datastore.deleteAll(Band.class); // you don't have to go home but you can't stay here
```

The second method automatically partitions the instances into groups of 100 to avoid datastore timeout errors