## Twig - Making the datastore more manageable ##

Twig is an object persistence interface built on Google App Engine's low-level datastore which overcomes many of JDO-GAEs limitations including improved support for inheritance, polymorphism and generic types.  You can easily configure, modify or extend Twigs behaviour by implementing your own strategies or overriding extension points in pure Java code.

Twig is the only datastore interface to support:
  * [Parallel Asynchronous Commands](Using#Parallel_Asynchronous_Commands.md)
  * [Plain old Java object data models with no datastore dependencies](Configuration.md)
  * [Merged OR queries on multiple properties](Using#Multiple_Query_Commands.md)

[Read the documentation](http://code.google.com/p/twig-persist/wiki/Contents) to get started

Visit the [Twig Discussion Group](http://groups.google.com/group/twig-persist) to ask a question

## Show me some code ##

The central interface you will use is `ObjectDatastore` which has an implementation `AnnotationObjectDatastore`

```
// create a new light-weight stateful datastore for every request
ObjectDatastore datastore  = new AnnotationObjectDatastore(); 

// create a complex object graph
Band ledzep = createClassicRockband();

// store the instance and all other reachable instances
Key key = datastore.store(ledzep);

// converted into a query by kind with a key name
Band result = datastore.load(Band.class, "Led Zeppelin");

// the identical instance is always returned from same datastore
assert result == ledzep; 

// modernize the classic rock band
ledzep.name = "White Stripes"; 

// call update on changed instances - no dirty detection
datastore.update(ledzep);

// no need to deal with Keys  
datastore.delete(ledzep);

// run a find command
Iterator<Band> punkBands = datastore.find()
    .type(Band.class)
    .addFilter("genre", EQUAL, Genre.PUNK)
    .returnResultsNow();
```

<table cellpadding='5' border='0'>
<blockquote><tr><td valign='top'>
<img src='http://wiki.twig-persist.googlecode.com/hg/images/twiggy_sitting.jpg' />
</td><td cellpadding='5' valign='top'>
<h2>A common sentiment</h2></blockquote>

A <a href='http://groups.google.com/group/google-appengine-java/browse_thread/thread/f5e1e6d211ddf689/ac8ef1c084fd0d62?lnk=gst&q=jdo+generics#ac8ef1c084fd0d62'>message</a> from the App Engine Java <a href='http://groups.google.com/group/google-appengine-java'>group</a>:<br>
<br>
"It seems alarming to me that these basic relations are difficult to code.  Lets face it, applications are full of these relations...it seems to me that too much developer time is required in the Persistence layer of GAE apps.  The Persistence and Presentation layers needs to be a "no brainer", so more focus can be where it needs to be - the business layer.  Does anyone feel the same?" - Diana Cruise (not pictured)<br>
</td></tr>
</table>

Twig is named after the super thin sixties model Twiggy - or it could be something like Typesafe Wrapper in GAE