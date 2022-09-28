package org.mastodon.mamut.feature.branch;

import org.mastodon.feature.Dimension;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureProjections;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.IntFeatureProjection;
import org.mastodon.feature.Multiplicity;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.properties.IntPropertyMap;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

import static org.mastodon.feature.FeatureProjectionKey.key;

public class BranchAverageSpotRadiusFeature implements Feature< BranchSpot >
{
	public static final String KEY = "Branch average spot radius";

	private static final String INFO_STRING = "Returns the average of the spots radius in a branch.";

	private static final FeatureProjectionSpec PROJECTION_SPEC = new FeatureProjectionSpec( KEY );

	public static final Spec SPEC = new Spec();

	final DoublePropertyMap< BranchSpot > map;

	private final FeatureProjection< BranchSpot > projection;

	@Plugin( type = FeatureSpec.class )
	public static class Spec extends FeatureSpec<BranchAverageSpotRadiusFeature, BranchSpot >
	{
		public Spec()
		{
			super(
					KEY,
					INFO_STRING,
					BranchAverageSpotRadiusFeature.class,
					BranchSpot.class,
					Multiplicity.SINGLE,
					PROJECTION_SPEC );
		}
	}

	BranchAverageSpotRadiusFeature( final DoublePropertyMap< BranchSpot > map )
	{
		this.map = map;
		this.projection = FeatureProjections.project( key( PROJECTION_SPEC ), map, Dimension.NONE_UNITS );
	}

	@Override
	public FeatureProjection< BranchSpot > project( final FeatureProjectionKey key )
	{
		return projection.getKey().equals( key ) ? projection : null;
	}

	@Override
	public Set< FeatureProjection< BranchSpot > > projections()
	{
		return Collections.singleton( projection );
	}

	@Override
	public Spec getSpec()
	{
		return SPEC;
	}

	@Override
	public void invalidate( final BranchSpot spot )
	{
		map.remove( spot );
	}
}
