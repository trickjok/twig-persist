# Embed Instances and Collections #

When ever you have two dependent classes like a `Book` and `Chapter`s or `Order` and `OrderItem` ask yourself if they really need to be stored in separate Entities. If not you should consider embedding the dependent instances in the container class.

Queries for a Book and its Chapters will be much faster and you can very quickly query for a Book containing a certain chapter name.

One disadvantage is that updates will take longer because the whole Entity must be rewritten for each change.

## Polymorphic collections ##

If your `Chapter`s are again refined in `EndOfAdventureChapter`s and `GoingOnChapter`s in your favorite Book Where You Are The Hero, you have a polymorphic collection of Chapters.

```
class Book {
    List<Chapter> chapters;
}
```

With Twig, you can use the entity as is, with no additional configuration. When storing a new `Book`, a new entity (of the correct type) will be created for each `Chapter`.

However, if you decide you want to store the `Chapter` as an embedded collection (since you decided you won't need to query for chapters on their own), you need to provide an additional configuration on each subclass, for instance:

```
@Entity(polymorphic = true)
class EndOfAdventureChapter extends Chapter {
    ...
}
```

and annotate the collection of chapters:

```
class Book {
	@Embedded(polymorphic = true)
	List<Chapter> chapters;
}
```

You also need (for now, and waiting for the source code to be updated), an **additional configuration** for you `AnnotationObjectDatastore`:

```
public class CustomAnnotationConfiguration extends AnnotationConfiguration {

    public CustomAnnotationConfiguration(final boolean indexPropertiesDefault) {
        super(indexPropertiesDefault);
    }

    public CustomAnnotationConfiguration(final boolean indexPropertiesDefault, final int defaultVersion) {
        super(indexPropertiesDefault, defaultVersion);
    }

    @Override
    public boolean polymorphic(final Class<?> instance) {
        Entity annotation = null;
        try {
            annotation = Class.forName(instance.getCanonicalName()).getAnnotation(Entity.class);
        }
        catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (annotation != null) {
            return annotation.polymorphic();
        }
        return false;
    }

}
```

and use it in your datastore, e.g.

```
public class CustomAnnotationObjectDatastore extends StandardObjectDatastore {

    public CustomAnnotationObjectDatastore() {
        this(true, 0);
    }

    public CustomAnnotationObjectDatastore(final boolean indexed, final int defaultVersion) {
        super(new CustomAnnotationConfiguration(indexed, defaultVersion));
    }

    public CustomAnnotationObjectDatastore(final boolean indexed) {
        super(new CustomAnnotationConfiguration(indexed, 0));
    }
}
```

## Maps ##

You do not need the `@Embedded` annotation on the map because there is a special `MapTranslator` that automatically stores maps as name-value pairs.  `@Embedded` tells Twig to look at the fields inside the instance (in this case a `HashMap`) and store the field values which causes a problem because one of them is final.

## Nesting embedded collections ##

It is not possible to embed collections to more than one level.  That is, a collection of collections cannot be embedded because Twig converts collections into "multi-valued-properties" so that they can be used in queries.

The solution is to serialise the second level of collection like this:

```
public static class ContainsASet {

     @Type(Blob.class)
     private Set<String> set;
}
```