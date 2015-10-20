Any class can be persisted as long as it:

  * Has a no-args constructor (can be private)
  * Has no final fields or they are marked as transient
  * Is public

## Annotations ##

The easiest way to define how your types are stored in the datastore is to use annotations.  It is also possible to configure every annotation be creating your own `Configuration` class and passing it to the constructor of your StandardObjectDatastore but this is more complicated and not recommended.

### Class Annotations ###

@Unique
@Entity

### Field Annotations ###

@Type
@Denormalise
@Parent
@Child
@Index