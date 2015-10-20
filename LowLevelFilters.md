## Entity Filters ##

Find commands can take specify predicates to filter both Entities and properties within Entities  They are used to filter out data you are not interested in _before_ it is "re-hydrated" as model instances.  This can save huge amounts of processing time because in GAE it is common to need to return more results than you need due to the single-inequality-property and sort-only-by-inequality rules.

in Twig a search for a hotel under a certain price might look like this:

```
Query query = typesafe.query(Hotel.class).addFilter("price", LESS_THAN, 30);
Iterator<Hotel> hotels = typesafe.find(query);
```

If you want to also sort the results by "popularity" you cannot do this in a simple query because the Datastore does not let you sort by a different property than your inequality.  You have to do your search sorted by popularity and then in your application filter out the results that are too expensive.

Previously you would have needed to iterate through the Hotel objects and ignore the expensive ones.  I believe that with JDO-GAE you would need to do this.  Please correct me if I am wrong.  That wastes CPU time instantiating objects and setting properties that are not even needed.

In Twig you can now filter out both entire Entities from the results at the lowest level by passing a `Predicate<Entity>` like:

```
Predicate<Entity> filter = new Predicate<Entity()
{
    public boolean apply(Entity hotel)
    {
        return hotel.getProperty("price") < 30;        
    }
}

FindOptions options = new FindOptions();
options.setEntityPredicate(filter);
Iterator<Hotel> hotels = typesafe.find(query, options);
```