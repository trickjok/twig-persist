# Comparison to Other Datastore Interfaces #

Currently Twig is the only interface that uses the underlying non-blocking abilities of app engine to allow you to run commands in parallel.  This is very useful because threads are not supported and many common queries need to be broken into several more basic ones and then combined.

Twig is the only interface that allows you to do OR queries by executing queries in parallel and then merging the results together while maintaining the sort order and removing duplicates.

Twig is the only interface that supports direct unowned relationships.  JDO supports direct _owned_ relationships and the alternative interfaces SimpleDS and Objectify do not support any direct relationships.

Twig is the only interface that allows your data model classes to be plain old Java objects with no dependencies on the low-level datastore API.

## Standard Interfaces JDO and JPA ##

Being designed specifically for the datastore gives many advantages over the standard interfaces.  If you have used JDO or JPA before you will already know that easy things are made hard and hard things are made absolutely impossible.  Core datastore settings like setting a queries parent or chunk size are not a part of the API because those concepts are specific to App Engine.

The standard interfaces must take a lowest common denominator approach to functionality.  In this sense they are too high level to make using the datastore convenient.  Add to this the fact that basic functionality like unowned relationships and polymorphism are not supported and it is clear to see why the mailing lists are full of frustrated users.

The claim that using a standard interface will give you portability benefits are just plain wrong.  It is virtually impossible to write a reasonably complicated application without significantly altering your data model to suit the quirks of JDO-GAE.  The most obvious sign of this is the use of `Key`s throughout data model classes which you must write your own code to dereference.  All your logic that deals with Keys is tied to the App Engine environment.

By contrast Twig data models are plain old Java objects which do not even require annotations.  They can be serialised with no issues and used with GWT on your client. If you need to transfer your app to run in a different environment it will be much easier to write code to persist these plain old Java objects in your new RDBMS or what ever other system you choose than it will be to rewrite you relationship code that depends on `Key`s.

## Low-Level API ##

The underlying low-level Datastore API is extremely clear, succinct and powerful.  It is not aimed at developers to use directly in their applications but rather for alternative interfaces to build on to create more flexible ways to access the datastore.

Unlike the standardized Java persistence API's it targets a specific implementation rather than taking a more general one-size-fits-all approach.  However, the low-level API is effectively untyped except for a handful of "native" types.  It works with bags of properties of these basic types which is extremely flexible but not so nice to program with.

The transaction model is very complex and at times seems inconsistent.  However, it is flexible and does leave open the opportunity to build more simple, consistent transaction management - which is what Twig has done.

So why has Google not created its own user-level datastore interface?  One very important claim that attracts developers to App Engine is that you use standard APIs so you are not "locked in" to the platform.  Google could not be seen to promote a non-standard interface ahead of the standards because they must not appear to encourage lock-in.  However, they have made the powerful low-level API available for third parties to to implement solutions like Twig, SimpleDS and Objectify.

## Objectify and SimpleDS ##

The current alternative interfaces greatly simplify using the [low-level datastore](http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/package-summary.html) by providing nice features like generics and breaking objects into simple values to store and loads of convenience features.  However, they do not go the extra step of abstracting away the use of Keys and dealing with instance identities (i.e. an internal key-instance cache).  This greatly simplifies their implementation but leaves more of the hard work to the developer.

As for JDO, it requires your data models to be written specifically for App Engine which potentially ties every layer of your application to the platform.  You must write your own code to follow `Key`s and read the `Entity`s which makes your code less flexible and harder to experiment with different relationship types.

### Model Definition ###
<table>
<tr>
<td> <b>Twig</b> </td><td> <b>Objectify</b> </td>
</tr>
<tr>
<td>
<pre><code>public class Person<br>
{<br>
    @Key Long id;<br>
    String name;<br>
}<br>
<br>
public class Car<br>
{<br>
    @Parent Person owner;<br>
    String color;<br>
}<br>
</code></pre>

</td>
<td>
<pre><code>public class Person<br>
{<br>
    @Id Long id;<br>
    String name;<br>
}<br>
<br>
public class Car<br>
{<br>
    @Id Long id;<br>
    @Parent Key&lt;Person&gt; owner;<br>
    String color;<br>
}<br>
</code></pre>

</td>
</tr>
</table>
Person is referenced directly. The Car Key is not required due to Twigs ability to map instances to `Key`s. This is one is the reasons Twig can persist any type of object even if it was not designed to be a data model instance.  Both Objectify and SimpleDS require every instance to define a Key.

### Load Car and Person ###
```
// in Twig you do not use keys - they are handled behind the scenes
Person owner = datastore.load(Person.class, personId);
Car someCar = datastore.load(Car.class, someCarId, owner);

// with JDO, Objectify and SimpleDS you must load each referenced instance manually
Person somePerson = objectify.get(personId);
Key<Person> ownerKey = new Key<Person>(Person.class, somePersonId);
Car someCar = objectify.get(new Key<Car>(ownerKey, Car.class, someCarId));
```

Notice how much more readable the code is without dealing with `Key`s.

### Get owner from Car ###
```
// plain Java objects do not need a library to follow references
Person owner = car.owner;

// you will need to follow references in every layer of you app
Person owner = objectify.get(car.owner);
```

If you want to change a relationship with Twig, for example from `@Child` to `@Embed`ded to test performance differences, your code that uses the data remains exactly the same. You still access the Car.owner field in the same way - directly - whether it is embedded or stored as a separate entity.

When Keys or other low-level datastore types have been used in your data models you actually need to change all of you code that uses a Car instance - potentially even the UI code if you use Objectify's support for passing datastore classes to GWT.  If you want your application code to be portable you would need to write an abstraction layer to create DTO's to pass your data to the client.  None of this is necessary with Twig because your models are plain old Java objects anyway.