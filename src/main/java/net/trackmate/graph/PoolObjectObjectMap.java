package net.trackmate.graph;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.trackmate.graph.mempool.MappedElement;

/**
 * Incomplete!
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class PoolObjectObjectMap< K extends PoolObject< K, T >, T extends MappedElement, V > implements Map< K, V >
{
	private final TIntObjectHashMap< V > indexmap;

	private final Pool< K, T > pool;

	public PoolObjectObjectMap( final Pool< K, T > pool )
	{
		indexmap = new TIntObjectHashMap< V >();
		this.pool = pool;
	}

	@Override
	public void clear()
	{
		indexmap.clear();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean containsKey( final Object key )
	{
		if ( key != null && key instanceof PoolObject )
			return indexmap.containsKey( ( ( K ) key ).getInternalPoolIndex() );
		else
			return false;
	}

	@Override
	public boolean containsValue( final Object value )
	{
		return indexmap.containsValue( value );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public V get( final Object key )
	{
		if ( key != null && key instanceof PoolObject )
			return indexmap.get( ( ( K ) key ).getInternalPoolIndex() );
		else
			return null;
	}

	@Override
	public boolean isEmpty()
	{
		return indexmap.isEmpty();
	}

	@Override
	public V put( final K key, final V value )
	{
		return indexmap.put( key.getInternalPoolIndex(), value );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public V remove( final Object key )
	{
		if ( key != null && key instanceof PoolObject )
			return indexmap.remove( ( ( K ) key ).getInternalPoolIndex() );
		else
			return null;
	}

	@Override
	public int size()
	{
		return indexmap.size();
	}

	@Override
	public Collection< V > values()
	{
		return indexmap.valueCollection();
	}

	@Override
	public PoolObjectSet< K, T > keySet()
	{
		return new PoolObjectSet< K, T >( pool, indexmap.keySet() );
	}

	@Override
	public void putAll( final Map< ? extends K, ? extends V > m )
	{
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public Set< Entry< K, V > > entrySet()
	{
		// TODO
		throw new UnsupportedOperationException();
	}
}
