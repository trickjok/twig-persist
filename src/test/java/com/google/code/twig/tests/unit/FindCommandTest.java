package com.google.code.twig.tests.unit;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.tests.unit.FindCommandTest.Spaceship.Planet;

public class FindCommandTest extends LocalDatastoreTestCase
{
	private AnnotationObjectDatastore datastore;

	@Before
	public void setup()
	{
		datastore = new AnnotationObjectDatastore();
	}
	
	static class Spaceship
	{
		enum Planet { MARS, VENUS, MERCURY };
		Planet planet;
		
		public Spaceship()
		{
		}
		public Spaceship(Planet planet)
		{
			this.planet = planet;
		}
	}
	
	@Test
	public void filterWithEnumValue()
	{
		datastore.store(new Spaceship(Planet.MARS));
		datastore.store(new Spaceship(Planet.VENUS));
		datastore.store(new Spaceship(Planet.MERCURY));

		Spaceship result = datastore.find()
			.type(Spaceship.class)
			.addFilter("planet", FilterOperator.EQUAL, Planet.MARS)
			.returnUnique()
			.now();
		
		assertNotNull(result);
	}
	
	static class Pilot
	{
		String name;
		Spaceship spaceship;
		
		public Pilot()
		{
		}
		
		public Pilot(String name, Spaceship spaceship)
		{
			this.name = name;
			this.spaceship = spaceship;
		}
	}

	@Test
	public void filterOnRelatedInstanceDirectly()
	{
		Spaceship urisSpaceship = new Spaceship(Planet.MARS);
		Pilot uriGaragrin = new Pilot("Uri Gagagrin", urisSpaceship);
		datastore.store(uriGaragrin);
		
		Pilot shouldBeUri = datastore.find()
			.type(Pilot.class)
			.addFilter("spaceship", FilterOperator.EQUAL, urisSpaceship)
			.returnUnique()
			.now();
		
		assertSame(uriGaragrin, shouldBeUri);
	}
	
	@Test
	public void filterOnRelatedInstanceByKey()
	{
		Spaceship laikasSpaceship = new Spaceship(Planet.VENUS);
		Pilot laikaSpaceDog = new Pilot("Laika", laikasSpaceship);
		datastore.store(laikaSpaceDog);
		
		Key laikasShipKey = datastore.associatedKey(laikasSpaceship);
		
		Pilot shouldBeLaika = datastore.find()
			.type(Pilot.class)
			.addFilter("spaceship", FilterOperator.EQUAL, laikasShipKey)
			.returnUnique()
			.now();
		
		assertSame(laikaSpaceDog, shouldBeLaika);
	}
}