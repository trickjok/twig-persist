# Versioned Types #

It is expensive to delete instances from the datastore and can take a long time to update a type if you change the format.  Maybe you need to update a lot of data from an external source but your application cannot deal with a partially updated datastore.  While processing changes to your datastore your application may need to be off-line.  Twig introduces a simple solution to this problem with _versioned types_ which allow the same type to be stored in the datastore with multiple versions

```
@Version(3)
class Band
{
  @Key name;
  Set<Musician> members;
  @Child Album[] albums;
}
```

You can also set a default version for your whole application using the constructor

```
// default version of 3 and properties un-indexed by default
ObjectDatastore datastore = new AnnotationObjectDatastore(false, 3);
```

You will then see in the datastore that your entities are named something like "v3\_com\_domain\_appname\_Thing"

This simple naming strategy keeps the versions completely separated so you could even have different versions of your app using different versions of a persistent class.  When you change the version number it will appear to your app as if there are no instances - the datastore is fresh.  This is great if you don't have time to wait days for your old data to delete.

You can also configure this version in code with by overriding `AnnotationStrategy.version(Type):int` for precise control.