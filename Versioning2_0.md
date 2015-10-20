When you send data to a client, like a web page, to be edited and updated, it may be possible for more than one user to edit the same data and update stale data to the datastore.  Versioning solves this problem of "long running transactions" by adding a "version" property to your Entity and always checking before you update an instance that you are using fresh data.

```
@Version
class Address
{
...
}

// get the current address and version
Address home = datastore.load(Address.class, "home");
long version = datastore.version(address);

// send the address and its version to the client to edit

// in another process receive the changed address to store
Address changed = ... // get from GWT client etc
long version = ... // also get from client

// associate the external data to our current session with the version
datastore.associate(changed, version, null, null);  // this method will change to be more intuitive

// throws an IllegalStateException if the data was out of date
datastore.update(myUpdatedAddress);

// run in a transaction to be certain that the version check is valid
datastore.transact(new Runnable()
{
  public void run()
  {
    datastore.update(anotherUpdatedAddrdess);
  }
});

// the version gets bumped up after updating
assert version +1 == datastore.version(myUpdatedAddress);

```

The `update` call should be run in a transaction to be sure that the check is valid.  Under the covers, Twig now keeps a version number for every instance in its session cache and will load the existing entity from the datastore when you update it.  This means that two datastore operations are happening instead of one for a non-versioned update.  To ensure the two calls are consistent a transaction is needed.