# Smaller kind names #

In an Engine app the size of your keys is an important factor of your total data size because, unlike traditional RDBMS systems, keys must contain the entire path to the related Entity. They are stored more than once in the Entity itself and for every reference to that Entity.

You can save space that your Keys require in the datastore by giving them shorter kind names.  In JDO you can do this by naming your classes with short names.  In Twig it is simple to specify short names while keeping your classes named nicely:

The kind name is determined by the `FieldStrategy` passed into the constructor of `StrategyObjectDatastore`.  If you are using `AnnotationStrategy` you can override the methods shown here

```
	new AnnotationStrategy()
	{

		@Override
		public String typeToName(Type type)
		{
			if (Hotel.class == type)
			{
				return "H";
			}
			else
			{
				return super.typeToKind(type);
			}
		}
		
		@Override
		protected Type nameToType(String kind)
		{
			if (kind.equals("H"))
			{
				return Hotel.class;
			}
			else
			{
				return super.kindToType(kind);
			}
		}
	}
```