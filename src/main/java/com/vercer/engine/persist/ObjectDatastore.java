package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;

public interface ObjectDatastore extends Activator
{
	// fluent style methods
	StoreCommand store();
	FindCommand find();
	LoadCommand load();

	// convenience store methods
	Key store(Object instance);
	Key store(Object instance, Object parent);
	
	<T> Map<T, Key> storeAll(Collection<? extends T> instances);
	<T> Map<T, Key> storeAll(Collection<? extends T> instances, Object parent);

	// updating
	void update(Object instance);
	void storeOrUpdate(Object instance);
	void storeOrUpdate(Object instance, Object parent);

	// convenience load methods
	<T> T load(Key key);
	<T> T load(Class<T> type, Object key);
	<T> T load(Class<T> type, Object key, Object parent);
	<I, T> Map<I, T> loadAll(Class<? extends T> type, Collection<I> ids);
	
	// convenience find methods
	<T> QueryResultIterator<T> find(Class<T> type);
	<T> QueryResultIterator<T> find(Class<T> type, Object ancestor);

	// convenience delete methods
	void delete(Object instance);
	void deleteAll(Type type);
	void deleteAll(Collection<?> instances);

	// activation
	int getActivationDepth();
	void setActivationDepth(int depth);
	
	/**
	 * Refresh an associated instance with the latest version from the datastore 
	 */
	void refresh(Object instance);
	void refreshAll(Collection<?> instances);
	
	// cache control operations
	void associate(Object instance);
	void associate(Object instance, Key key);
	void disassociate(Object instance);
	void disassociateAll();
	Key associatedKey(Object instance);

	void setConfiguration(DatastoreServiceConfig config);

	// transactions
	Transaction beginTransaction();
	Transaction getTransaction();
	
}
