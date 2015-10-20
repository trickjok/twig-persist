The `ObjectDatastore` interface is the central interface you will be working with to manage persistent data in your application.  The six basic commands you need to be familiar with are:

  * **Store**: Saves a new instance in the datastore and creates an id if none was defined.
  * **Update**: Changes an existing instance that has previously been stored in the datastore.
  * **Load**: Retrieves one or many specific instances from the datastore by id or key.
  * **Find**: Searches or queries for persistent instances whose ids or keys are not known.
  * **Delete**: Removes one or many persistent instances from the datastore.
  * **Refresh**: Reloads the fields of an instance from the datastore in case it has changed.

The first four of these commands have two forms: a convenient short method that you use most often and a  [fluent style](http://en.wikipedia.org/wiki/Fluent_interface) method that gives you access every available option.  As an example see these two equivalent find commands

```

// convenience method to find all instances with a certain property value
Iterator<RockBand> rockers = datastore.find(RockBand.class, "hair", Hair.LONG_LIKE_A_GIRL);

// the equivalent fluent style command
Iterator<RockBand> rockers = datastore.find()
  .type(RockBand.class)
  .addFilter("hair", EQUAL, Hair.LONG_LIKE_A_GIRL)
  .now();
```

Although the fluent command is longer it gives access to many more options not shown here and perhaps even more importantly it is self documenting.  The convenience method parameters do not make it as obvious which parameter is what.  In fact all the convenience methods are implemented using the fluent style commands under the covers.  They just save you some typing.

## Find Command ##

You use the find command to query for data when you do not know the ids or keys of the instances you need to work with.  If you do have the ids then use the `load()` command instead.

A typical find command could look like this:
```
List<Musican> rockers = datastore.find()
  .type(Musician.class)
  .addFilter("hair", EQUAL, Hair.LONG_LIKE_A_GIRL)
  .ancestor(sonyMusic)
  .addSort("name")
  .startFrom(4)
  .returnAll()
  .now();
```

Below we will walk through what each of these options does and the alternatives.

### Convenience forms ###

`ObjectDatastore` defines some common find methods directly to save you a bit of typing.  These are implemented under the covers as the equivalent full find command as shown above.

```

// equivalent commands
datastore.find(Musician.class);
datastore.find().type(Musician.class).now();

// equivalent commands
datastore.find(Musician.class, "name", "Darwin Deeze");
datastire.find()
  .type(Musician.class)
  .addFilter("name", EQUALS, "Darwin Deeze")
  .now();
```


### Type ###

Every find query is typed with the Class of the returned instances.  This must be a concrete class - not an interface or abstract class - so that Twig can find its no-arg constructor and create results.

### Fetching Data ###

All the methods starting with fetch`*` do not alter which instances are found but only _how_ they are returned for performance tuning.

**.fetchNoFileds()** causes the instances to be created but no field values are loaded thus making the command extremely fast.  Under the covers, the query is run using `Query.setKeysOnly()`.

**.fetchFirst(int)** determines how many results are returned from the datastore as soon as the command is executed. You should tune this value so that most commands only require a single trip to the datastore to read results.  It is only useful when using the default `Iterator<T>` return type.  Its low-level equivalent is `FetchOptions.prefetchSize(int)`

**.fetchNextBy(int)** is similar to `.fetchFirst(int)` but is used to determine how many results to return after the initial trip to the datastore. If you fetch enough results in the first call to the datastore then this value will never be used.  Its low-level equivalent is `FetchOptions.chunkSize(int)`

**fetchMaximum()** limits the number of results returned. Its low-level equivalent is Query.limit(...).

### Restricting results on the client ###

When you add a filter to a find command the datastore server return only the data that matches your query.  Unfortunately, it is common that you cannot create a query that returns only the results that you need.  For example, if you had an e-shop where you needed to sort results by price and show only products with an average review over 8.0... you are stuck because you can only sort by by your inequality filter (average review).

In these cases you are forced to filter results after they are sent to the application server.  So in the example above, you could sort by price and then ignore all results that have a review less than 8.

Twig makes this easier and more efficient by allowing you to restrict which low-level Entity's and properties are "re-hydrated" from the results.

```
.restrictEntities(new Restriction<Entity>()
{
  public boolean allow(Entity candidate)
  {
    return candidate.getProperty("review") >= 8;
  }
}
```

Twig will ignore any results that do not match this restriction which saves memory and CPU time.

You can also choose to restrict properties in each Entity using

```
.restrictEntities(Restriction<Property> candidate)
```

### Return Values ###

The results of your query can be returned in several ways using methods that all start with return`*`:

By default results are returned as a `QueryResultIterator<T>` which is just a normal `Iterator<T>` but with the added ability to get the `Cursor` so you can remember where you got to in the results and continue iterating from the same place later on (more on cursors later).

**.returnAll()** causes every result to be read into memory and returned as a `List<T>` which can be very convenient when you know there are not too many results and you need to work with all of them.  You could run into problems with memory usage if there are a lot of results.

**.returnUnique()** performs the query and throws an exception if more than one result is returned.  The unique result is returned directly.

**.returnCount()** does not actually create any instances but just counts how many results there were and returns the number.  By default the datastore will only count up to 1000 results unless you also set .fetchMaximum(LOTS).


### Terminators: Async or Sync ###

The last method call of every command is one of the terminators **.now()** or **.later()** which determines if the command is run in normal blocking synchronous mode (now) or in non-blocking asynchronous mode (later) which allows your program to continue while it waits for the datastore to return results.