# Performance enhancing features #

## Low-Level Filters ##

Converting low-level Entities into Instances is fast when compared to the time spent making the remote call to the datastore.  Datastore queries are very limited because you can only have one inequality filter property and that must be on the same property as your sort order so often you need to return a lot more results from a query than you would like and then filter out those you want in memory.  In these cases it is a waste of resources to translate all `Entity`s and properties into data model instances.  You can specify `Predicate`s that operate at the low-level to exclude whole Entities from the results and individual properties from each instance.

Because these `Predicate`s are defined using [Google Collections `Predicate`](http://google-collections.googlecode.com/svn/trunk/javadoc/index.html?http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/collect/package-summary.html) class you can combine them into complex rules using AND, OR, NOT.

```
Predicate<Entity> hotelInRegionPredicate = new Predicate<Entity>()
{ 
	public boolean apply(Entity hotel)
	{
		Block block = new Block((Long) hotel.getProperty("location"));
		boolean contains = region.contains(block.toRegion().getCentre());
		if (hotelCount.get() == null)
		{
			hotelCount.set(new MutableInt());
		}
		hotelCount.get().increment();
		return contains;
	}
}
```

## Embed Instances and Collections ##

When ever you have two dependent classes like a `Book` and `Chapter`s or `Order` and `OrderItem` ask yourself if they really need to be stored in separate Entities. If not you should consider embedding the dependent instances in the container class.

Queries for a Book and its Chapters will be much faster and you can very quickly query for a Book containing a certain chapter name.

One disadvantage is that updates will take longer because the whole Entity must be rewritten for each change.

## Find and Store in Parallel ##

If you have multiple commands to execute run them in parallel and then wait for the results.  This will not reduce the total CPU time used but will reduce the clock time it takes to respond to that of the single slowest query.

```
	Future<QueryResultIterator<Pony>> myLittlePoniesAreComing = 
		datastore.find()
			.type(Pony.class)
			.addFilter("size", EQUAL, Size.Little)
			.returnResultsLater();
			
	Future<QueryResultIterator<BackToThe>> backToTheFuture =
		datastore.find()
			.type(BackToThe.class)
			.returnResultsLater();
			
	Future<Key> junkKey = datastore.store().instance(junk).returnKeyLater();
	
	// do some other stuff - make a cup of virtual tea
	
	// now wait for all results
	Iterator<Pony> myLittlePonies = myLittlePoniesAreComing.get();
	BackToThe doc = backToTheFutre.get().next();
	junkKey.get();
	 
```

## Set Properties Un-Indexed By Default ##

Unnecessary indexes take time to modify when you store or update instances.  Setting properties to un-indexed by default makes you examine each property as required.  You will get an exception thrown when an index is missing so there is no risk of accidentally slowing down your application.

## Shorten Kind Names ##

By default the full package and class name are used as the Kind which is stored multiple times per Entity and relationship.  Shortening this to a one or two letter abbreviation will save datastore space.  [See details](Configuration.md)