## Overriding default behaviour ##

It is simple to override only particular strategy methods using `AnnotationStrategy` as a base.  This example adds logging, caching in memcache, defines certain classes as being embedded (without the need to add an annotation every time) and adds a new translator to the translator chain that handles all values.

```
public class MyObjectDatastore extends StrategyObjectDatastore
{
	private static final Logger log = Logger.getLogger(MyObjectDatastore.class.getName());
	private final MemcacheService memcache;

	@Inject
	public MyObjectDatastore(DatastoreService datastore, MemcacheService memcache)
	{
		this.memcache = memcache;
		super(datastore, new AnnotationStrategy(true)
		{
			@Override
			public boolean embed(Field field)
			{
				if (field.getType() == Location.class)
				{
					return true;
				}
				else if (field.getType() == Money.class)
				{
					return true;
				}
				else
				{
					return super.embed(field);
				}
			}
		});
	}

	@Override
	protected ChainedTranslator createValueTranslator()
	{
		ChainedTranslator translator = super.createValueTranslator();
		translator.append(new BlockTranslator());  // a custom translator to handle a specific type
		return translator;
	}

	@Override
	protected Entity keyToEntity(Key key)
	{
		Entity entity = (Entity) memcache.get(KeyFactory.keyToString(key));
		if (entity == null)
		{
			entity = super.keyToEntity(key);
			if (entity != null)
			{
				memcache.put(KeyFactory.keyToString(key), entity);
			}
		}
		return entity;
	}

	@Override
	protected void onAfterUpdate(Object instance, Entity entity)
	{
		memcache.put(KeyFactory.keyToString(entity.getKey()), entity);
	}

	@Override
	protected void onAfterDelete(Key key)
	{
		memcache.delete(KeyFactory.keyToString(key));
	}

	@Override
	protected void onAfterStore(Object instance, Entity entity)
	{
		log.fine("Stored " + entity.getKey());
		memcache.put(KeyFactory.keyToString(entity.getKey()), entity);
		super.onAfterStore(instance, entity);
	}

	@Override
	protected void onAfterRestore(Entity entity, Object instance)
	{
		log.fine("Restored " + entity.getKey());
		super.onAfterRestore(entity, instance);
	}
}

```