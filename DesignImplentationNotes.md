# Central Classes #

The central interface which you will work with is ObjectDatastore which contains the methods requied to store, find, update, refresh and delete objects from the datastore.  This interface is implemented by TranslatorObjectDatastore which introduces the concept of using a PropertyTranslator to encode and decode a Java object to and from a low-level Entity which is essentially a Map of name-value pairs.  The PropertyTranslator is responsible for breaking down an object into the [native types supported by the datastore](http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/DataTypeUtils.html).  It deals with Property objects, which encapsulate the name, value and indexed flag, and Path objects which make complex property names like 'store.location.latitude' easier to work with.

PropertyTranslators are composed using Chain of Responsibility and Decorator patterns to make complex behaviours out of simple building blocks.  For example, ChainedTranslator looks for a child translator to handle the task - the first to return a non null value is used.  Hence, null is a special value that can only be returned to signal that a translator could not handle the call.  A null member is simply not encoded into the Entity at all and is signaled by returning PropertyTranslator.NULL\_VALUE.  EnumTranslator simply encodes the value into a String and uses Enum.valueOf() to decode it.  A useful base class to create your own translators is SpecificTypeTranslator which helps convert a single class.
<table cellpadding='5' border='0'>
<blockquote><tr><td valign='top'>
<img src='http://wiki.twig-persist.googlecode.com/hg/images/twiggy_street.jpg' />
</td><td cellpadding='5' valign='top'></blockquote>

StrategyObjectDatastore is a concrete subclass of TranslatorObjectDatastore that composes a standard PropertyTranslator and tries to allow all its interesting behaviour to be configurable using plugable strategy interfaces that must be passed into its constructor.  Using strategy objects (or Delegates if you are familiar with Cocoa programming) makes the extension points easier to understand and allows more flexible reuse than plain vanilla subclassing and overriding methods.  Most methods are declared final unless they are intended to be used as extension points.<br>
<br>
StrategyObjectDatastore uses an anonymous subclass of ObjectFieldTranslator to examine each field of an instance and decide how to convert it into Property's.  It uses the Field object to decide which translator to use by calling ObjectFieldTranslator.translator(Field).  The various strategies are used here to decide which translator to return.  For example, if the RelationshipStrategy decides that a certain field is used to hold a reference to the entities parent then a ParentEntityTranslator is used to encode / decode the field value.  Using Fields makes a lot of information available to the strategy i.e. name, declaring class, generic type, modifier flags.<br>
<br>
</td></tr></table>


When storing an instance ObjectFieldTranslator examines each field and using RelationshipStrategy decides if any of the fields holds a key value or a parent instance.  However, when low-level Entities are created they need to have all the parts of their Key passed into the constructor so they cannot be created until all the fields are examined and encoded.  Therefore, KeySpecification is needed to hold all the parts of the key until the Entity is created.

# Delayed Key creation with ObjectReferences #

The Entity key needs to be created after all the instance fields are encoded because one of the fields may be the Key's unique name.  While converting a child member the child may reference back to the parent creating a circular reference.  This would cause a problem because the parent key has not yet been created but it is needed to store in the child entity as a property.  The solution to this problem is to delay the creation of the key to the last possible moment by instead giving the child a reference to a ObjectReference which promises to point to the actual Key when needed.

## From type-safe to datastore entities ##

Keys are referenced through ObjectReference which allows them to be lazily initialized when they are actually needed.  When the key is needed the instance fields will be examined for a configured key value and possible parent entity reference.  The entity may need to be put in the datastore to generate an automatic key or if we have enough info about the key already we could just return the key without storing the instance yet.  This flexible store order allows any possible combination of child or ancestor to have undefined automatically generated keys.

Three types of ObjectReference are used to reference a key during writing.  Firstly, depending on the entity's relationship (parent, child or unrelated) an ObjectReference is created that holds the logic to set up the relationship state and lazily call a translator to create the key.  This allows all entity key properties to be lazily loaded only after all other properties in the object have been processed.  Therefore, by the time the key is needed a key name and parent will have been discovered if defined.

These key references are created in StrategyTypesafeSession by the translators parentTranslator, childTranslator and independantTranslator.

Next, once the key is asked for the IOU reference is replaced with a reference to the KeySpecification that can only be used by referenced entities if the key is complete without put()'ing to the datastore (i.e. it does not need a generated long id).  For example, while storing a band instance, before the band is stored, there will exist in the key cache its KeySpecification.  If the band object defined a key name field then its key would be complete and so Album instances could back-reference it before it has finished being stored with no problems.

Once any instance has finished being stored in the datastore by calling DatastoreService.put(Entity) then the actual key is placed in the key cache.

## From datastore entities to type-safe ##

Currently, instances are restored from the datastore in top-down order.  The first instance retrieved from the datastore is instantiated, added to the cache and then its children are processed.  This means that if any of the child instances contains a key that references back to the parent, the correct instance will be pulled from the cache even if it is not complete.  This approach, while simple, requires the instance to be instantiated before its child field values.  Therefore classes that contain final fields or constructors that require valid parameters cannot be used because they require their parameters upon construction.  This will be addressed in a future release with Instantiators and Activators that use the decoded field values to call constructors or factory methods as required.