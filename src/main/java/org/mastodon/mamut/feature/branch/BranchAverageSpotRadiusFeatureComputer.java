package org.mastodon.mamut.feature.branch;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.feature.MamutFeatureComputer;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.mamut.model.branch.ModelBranchGraph;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Iterator;

@Plugin( type = MamutFeatureComputer.class )
public class BranchAverageSpotRadiusFeatureComputer implements MamutFeatureComputer
{

	@Parameter
	private MamutAppModel model;

	@Parameter
	private ModelBranchGraph branchGraph;

	@Parameter( type = ItemIO.OUTPUT )
	private BranchAverageSpotRadiusFeature output;

	@Override
	public void createOutput()
	{
		if ( null == output )
			output = new BranchAverageSpotRadiusFeature( new DoublePropertyMap<>( branchGraph.vertices().getRefPool(), -1 ) );
	}

	@Override
	public void run()
	{
		for ( final BranchSpot bv : branchGraph.vertices() )
		{
			final Iterator< Spot > it = branchGraph.vertexBranchIterator( bv );
			double sumRadius = 0;
			long n = 0;
			while ( it.hasNext() )
			{
				Spot spot = it.next();
				sumRadius = Math.sqrt( spot.getBoundingSphereRadiusSquared() );
				n++;
			}
			branchGraph.releaseIterator( it );
			output.map.set( bv, sumRadius / n );
		}
	}

}
