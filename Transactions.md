# Simplified Transactions #

An ObjectDatastore can have only one current transaction at a time.  Once a transaction is started it is used for every operation on that datastore until it is committed or rolled back.

```
Transaction tx = datastore.beginTransaction();

try
{
  Iterator<Band> polygramBands = datastore.find()
	.type(Band.class)
	.withAncestor(polygram)	 // an ancestor must be used in a tx query
	.addSort("name")
	.returnResultsNow();

  Band latestBand = createLatestBand();
  datastore.store(latestBand, polygram); // store must have same ancestor

  while (polygramBands.hasNext())
  {
    Band polygramBand = polygramBands.next()
    polygramBand.performanceFee = polygramBand.performanceFee * 0.8;
    datastore.update(polygramBand);
  }
  tx.commit();
}
finally
{
  if (tx.isActive())
  {
    tx.rollback();
  }
}
```