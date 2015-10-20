# Creating a Data Model #

Building a data model is as easy as writing plain old Java objects.  You may choose to follow the Java Beans pattern using getters and setters for field values but this is not a requirement.  Twig directly reads the field values in your classes and so by-passes these accessor methods.  The examples in this guide use direct field access with no access modifiers (public, private etc) for clarity although in a real world project you would most likely use private fields and control access with methods.

We will begin by creating the basic data model for a music festival application.

```
class MusicFestival
{
  Date held;
  List<Band> bands;
}

class Band
{
  Set<Musician> members;
}

class Musician
{
  enum Hair { LONG_LIKE_A_GIRL, UNKEMPT_FLOPPY, NAVY_SHORT, BALD }; 
  String name;
  Date born;  // many common types converted automatically
  Hair hair;
}
```

The only requirement of your data classes is that they have a no-args or default constructor which can even be private.  Notice that these are pure POJOs with no datastore classes - Twig is the only datastore interface to support this.

<table cellpadding='5' border='0'>
<blockquote><tr><td valign='top'>
<img src='http://wiki.twig-persist.googlecode.com/hg/images/twiggy_promo_small.jpg' />
</td><td cellpadding='5' valign='top'>This model will persist fine with no extra configuration required. However if you want to refine how the model is stored you will need to add configuration information.  If you want absolutely no source dependency on Twig you can configure with Java code or instead use the more convenient annotations.</blockquote>

<code>StrategyObjectDatastore</code> is the central implementation class in Twig which delegates common configuration options to one of several strategy objects which are passed to the constructor.  This pattern allows strategies to be reused and mixed together in a way that inheritance does not permit.<br>
There are four strategy interfaces which divide the configuration into simple related chunks: <code>RelationshipStrategy</code>, <code>FieldStrategy</code>, <code>StorageStraegy</code> and <code>ActivationStrategy</code>.<br>
<br>
A useful implementation of the above strategies, <code>AnnotationStrategy</code> which implements all of the strategy interfaces in one class.  There is also a convenience class <code>AnnotationObjectDatastore</code> which simply constructs a <code>StrategyObjectDatastore</code> using this annotation strategy.<br>
<br>
</td></tr></table>

## Parent-Child Relationships ##
Parent-child relationships define _entity groups_ which are used to determine which instances can be used in the same [datastore transaction](http://code.google.com/appengine/docs/java/datastore/transactions.html).

The data model above has direct references from MusicFestival to Band to Musician which by default are stored as _independent entities_ in their own singular entity group.  To set up an entity group configure references as _parnt_ or _child_ entities.

### Configure with Annotations ###

To have a Band and its Albums in the same entity group simply place `@Parent` or `@Child` on a reference like so:

```
class Band
{
  Set<Musician> members;
  @Child Album[] albums;  // include the Bands Key in the Key of each Album 
}

class Album
{
  @Parent Band band;  // reference back to the parent for convenience
  String name;
  int copiesSold;
}
```

All the albums are now children of the Band and can be used in the same transaction.  The Album's low level key contains the Band's Key and so can never be moved to another Band unless the Key is changed.  It is not necessary to have the @Parent reference in this example to form the relationship.  It may just be convenient to have a bi-directional reference in you application.

The low-level `Key` contains a `String` name or `long` id as well as the chain of ancestor Keys to create a system wide unique Key.  If you want to control the key value you can define a field to supply it.

```
class Band
{
  @Key String name;  // the key will contain the band name which must be unique
  Set<Musician> members;
  @Child Album[] albums;
}
```

Defining the Key name is the only reliable way to ensure a band name is unique across multiple entity groups.  See the Storing data section for an example of this.

### Configuration with Code ###

The `RelationshipStrategy` defines which `Field`s represent parents, children or define the key for the `Entity`.  Most strategy methods work with `Field`s which gives them access to the field name and generic type and also the defining class.
```
// example of implementing a configuration strategy
class MyRelationshipStrategy implements RelationshipStrategy
{
  boolean parent(Field field)
  {
    return field.getName().equals("parent");
  }
  boolean child(Field field);
  {
    if (field.getDeclaringClass() == MyClass.class)
    {
      return field.getName().equals("sprogs");
    }
  }
  boolean key(Field field)
  {
    return field.isAnnotationPresent(MyKeyAnnotation.class);
  }
}
```
Key fields can be any type that can be unambiguously encoded and decoded as a String.  If no key field is defined then the Datastore will generate a long key automatically which can be accessed from the Key that is returned or from `ObjectDatastore.associatedKey(Object)`.

A parent reference does not create any new properties in the current Entity because it is encoded into the current Key.

If you define a collection of instances to be a child then the configuration applies to every element.

_Note: Currently only the last instance in the chain of referenced instances to be stored can have no key name defined. This will be fixed in a future version. To work around this you may need to store the referenced instances without a defined @Key field first_

Every annotation has an equivalent strategy implementation in code which some of the examples will not show

## Field Types ##

Twig uses the `ObjectFieldTranslator` to look at the type each field in your classes to decide how to store them.  Sometimes you need to give it more information to choose the right type.  For example, by default `String`s can only be 500 characters long.  If your `String`s may be longer than this you must tell Twig to store the field as `Text`.  You can do this by returning `Text` from `FieldTypeStrategy.typeOf(Field)`.  If you are using the `AnnotationStrategy` you can declare this with a field annotation.

```
class Track
{
  @Type(Text.class) String description;  // longer than 500 chars
  @Type(String.class) Duration length;
  @Type(Blob.class) AudioClip sample;
}
  
// add tracks to the data model
class Album
{
  @Parent Band band;
  String name;
  int copiesSold;
  List<Track> tracks;
}
```

These annotations tell Twig to use the Text type and to convert Duration to a String in the datastore.  Twig comes with TypeConverters to convert many common types such as SerializableToBlob and CurrencyToString but you would need to register a new TypeConverter to handle the Duration conversion.

This can be achieved by overriding an extension point in `StrategyObjectDatastore` (or the subclass `AnnotationObjectDatastore`)

```
class MyObjectDatastore extends StrategyObjectDatastore  
{
  ...

  @Override
  protected TypeConverter createTypeConverter()
  {
    // start with the default converter which we will add to
    CombinedTypeConverter converter = createDefaultTypeConverter();
    
    // register a new converter for storing Durations as Strings starting with "s:"
    converter.register(new SpecificTypeConverter<Duration, String>()
    {
      public String convert(Duration source)
      {
        return "s:" + source.getStandardSeconds();
      }
    });

    // register the reverse converter for reading instances back again
    converter.register(new SpecificTypeConverter<String, Duration>()
    {
      public Duration convert(String source)
      {
        int seconds = Long.parseLong(source.substring(2));
        return Duration.standardSeconds(seconds);
      }
    });
  
    return converter;
  }
}
```


The `AnnotationStrategy.type(Field)` implementation converts all `Collection` implementations and `Array`s to `List` so that they can all be handled by the `ListTranslator`. Twig uses [Gentyref](http://code.google.com/p/gentyref/) to analyse what the type of the collection is and may also need to convert the elements.

If you have a field naming convention that results in ugly datastore property names you may want to override FieldTypeStrategy.name(Field).  By default it handles fields prefixed with "_" so when you use the names in queries you can omit the prefix._

All `Key`s contain the kind which in Twig is a modified fully qualified class name like "com\_example\_project\_Rockstar" (dots are not accepted by the data viewer GQL web interface).  This might result in larger keys than necessary so you might want to override `FieldTypeStrategy.typeToKind(Type)` and `FieldTypeStrategy.kindToType(String)` to remove your package name etc.


## Storage Options ##

It is more efficient to store fewer large entities than many smaller ones so embedding dependent objects in a parent instance can be good for performance.  It also allows you to query on the embedded properties in a single datastore query.

```
class Album
{
  @Parent Band band;
  String name;
  int copiesSold;
  @Embed List<Track> tracks;  // tracks will not be entities but fields in the album
}
```

`StorageStrategy` configures how properties are stored in the datastore.  By default the most inefficient set-up is used which indexes every property and stores every field.  This makes it likely that Twig will work out-of-the-box with no configuration but you should optimise before using in production.  To store a member as _embedded_ return true from `StorageStrategy.embed(Field)`.

When you create your datastore you can configure it to not index properties by default which is a good way to optimise your data model.  You then need explicitly state which fields to index:

```
class Album
{
  @Parent Band band;
  @Index String name;
  int copiesSold;
  @Embed List<Track> tracks;  // tracks will not be entities but fields in the album
}
```

You only need to index fields that you will search or sort on or that will be used in a custom index.

You can set some fields to not be completely ignored with an annotation:

```
class Track
{
  @Type(Text.class) String description;  // longer than 500 chars
  @Type(String.class) Duration length;
  @Store(false) AudioClip sample;
}
```

In code `StorageStrategy.store(Field)` is used to define which fields are stored or ignored by Twig.  The `AnnotationStrategy` stores all non-transient fields by default.  Override `StorageStrategy.indexed(Field)` to decide which fields are needed in your index.


You might be wondering what happens if an item is not declared as an Entity (@Parent, @Child, @Independent) or as `@Embed`ded but left to default.  If no options are defined then the _default translator_ is used which is a chain of translators defined:

```
defaultTranslator = 
    new ChainedTranslator(
        new ListTranslator(valueTranslator), 
        getFallbackTranslator());
```

This means - first try to encode it as a simple value (or list of values) then if that fails use the _fallback translator_ which is declared:

```
protected PropertyTranslator getFallbackTranslator()
{
  return getIndependantTranslator();
}
```

This non-final method is an extension point that you can use to alter the default setup.  The default "fallback translator" encodes values as independent entities (i.e. not parent or child) but you could change this to use e.g. SerializingTranslator to serialize all unconfigured instances or `getEmbedTranslator()`.

Most classes translate fine by storing their internal field values.  But if you have a class the does not have a no-args constructor or there is a problem translating its internal fields you can tell Twig how to store it by adding a PropertyTranslator to the valueTranslator chain:

```
class MyObjectDatastore extends StrategyObjectDatastore
{
  @Override
  protected ChainedTranslator createValueTranslator()
  {
    ChainedTranslator translator = super.createValueTranslator();

    // a custom translator to handle a specific type
    translator.append(new BlockTranslator()); 
    return translator;
  }
}
```

## Activation ##

When instances are deserialized they are _activated_ to set their field values.  In order to stop the possible run-away loading of data you can set an _activation depth_ which stops the loading chain of related instances.  For example, when we load a MusicFestival instance all of the Bands, Musicians and Tracks are also loaded because they are directly referenced.  If you set an activation depth of 1 then the festival is loaded and its fields are set but none of the fields values are loaded.  Each Band will have `null` values in its fields.  An activation depth of 2 will load the Bands fields but not the Musicians fields and so on.

The activation depth setting can be set directly and globally for all classes with `ObjectDatastore.setActivationDepth(int)` but it can also be configured per class or by a particular field giving you complete control of what is loaded.

```
// per field gives great control
class Band
{
  Set<Musician> members;
  @Activate @Child Album[] albums;  // always activate a bands albums  
}

// setting it on the class has the same effect
@Activate
class Album
{
  @Parent Band band;
  String name;
  int copiesSold;
  @Activate(0) List<Track> tracks; // never load tracks unless we ask
}
```

In code this is controlled by the ActivationStrategy.activate(Field):boolean` method.

Setting an activation depth for your model will override the global value set on your datastore.

## Override defaults ##

If you just want to tweak a some default behaviour it will easiest to extend AnnotationStrategy

```
// do not index by default, use version 3 as the default type version
CombinedStrategy combinedStrategy = new AnnotationStrategy(false, 3)
{
  // embed all Money instances without needing to annotate each one
  public boolean embed(Field field)
  {
    if (field.getType() == Money.class)
    {
      return true;
    }
    return super.embed(field);
  }
}
  
// passing in the datastore service allows you to reuse a single instance
DatastoreService service = DatastoreServiceFactory.getDatastoreService();
ObjectDatastore datastore = new StrategyObjectDatastore(service, combinedStrategy);
```