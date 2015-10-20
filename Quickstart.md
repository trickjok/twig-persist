# Get straight to the point #

[Include Twig in your project](IncludingTwig.md)

Most classes will persist with _no configuration_ as long as they have a default or no-args constructor.

Ready... code!

```
class Band
{
	@Key String name;
	@Child List<Musician> members; 
}

// create the datastore instance directly - allows sub-classing
ObjectDatastore datastore = new AnnotationObjectDatastore();
	

// store some bands and every reachable instances such as band members
Collection<Band> bands = ...
Map<Band, Key> bandKeys = datastore.storeAll(bands);

// create a new band - name possibly already exists
Band franz = new Band("Franz Ferdinand");

// for more options use a command - run query async and check unique key
Future<Key> futureKey = datastore.store()  // creates a fluent command
    .instance(franz)
    .ensureUniqueKey()  // don't clobber an existing band with same key
    .returnKeyLater();

// find popular bands
QueryResultIterator<Band> popularBands =  
	datastore.find().type(Band.class)
		.addFilter("albums.sold", GREATER_THAN, 1000000).returnResultsNow();

// set off two commands in parallel 
Future<QueryResultIterator<Musician>> baldMusiciansFuture = 
	datastore.find()
		.type(Musician.class)
		.addFilter("hair", EQUAL, Hair.BALD)
		.returnResultsLater();

// query result iterators are normal iterators with a getCursor() method
Future<QueryResultIterator<Track>> longTracksFuture = 
	datastore.find()
		.type(Track.class)
		.addFilter("length", GREAT_THAN, 300);
		.returnResultsLater();
			
// now wait for results of both commands to return
QueryResultIterator<Musician> baldMusicians = baldMusiciansFuture.get(); 
QueryResultIterator<Track> longTracks = longTracksFuture.get();
	
// delete all instances of a type
datastore.deleteAll(Band.class);
```

Read the rest of the docs when you get stuck or ask a question on the [Twig Discussion Group](http://groups.google.com/group/twig-persist)