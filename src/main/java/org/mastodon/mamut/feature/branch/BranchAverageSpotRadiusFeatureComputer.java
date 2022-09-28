package org.mastodon.mamut.feature.branch;

import org.mastodon.mamut.feature.MamutFeatureComputer;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.branch.BranchLink;
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
	private ModelGraph graph;

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
		for ( final BranchSpot bs : branchGraph.vertices() )
		{
			double radius = Double.NaN;
			if( bs.incomingEdges().isEmpty() && bs.outgoingEdges().size() != 1 )
				radius = averageRadiusForThisBranchSpot(bs);
			else if( bs.incomingEdges().size() == 1 )
				radius = averageRadiusForIncomingBranchLink(bs);
			output.map.set( bs, radius );
		}
	}

	private double averageRadiusForIncomingBranchLink( BranchSpot targetBranchSpot )
	{
		Spot ref = graph.vertexRef();
		BranchSpot bvref = branchGraph.vertexRef();
		try {
			BranchLink branchLink = targetBranchSpot.incomingEdges().get(1);
			Spot targetSpot = branchGraph.getLinkedVertex( targetBranchSpot, ref );
			long n = 1;
			double sumRadius = Math.sqrt( targetSpot.getBoundingSphereRadiusSquared() );
			Iterator<Spot> iterator = branchGraph.vertexBranchIterator( branchLink );
			while(iterator.hasNext()) {
				Spot spot = iterator.next();
				sumRadius += Math.sqrt( spot.getBoundingSphereRadiusSquared() );
				n++;
			}
			BranchSpot sourceBranchSpot = branchLink.getSource( bvref );
			if( sourceBranchSpot.incomingEdges().isEmpty() ) {
				Spot sourceSpot = branchGraph.getLinkedVertex( sourceBranchSpot, ref );
				sumRadius += Math.sqrt( sourceSpot.getBoundingSphereRadiusSquared() );
				n++;
			}
			return sumRadius / n;
		}
		finally
		{
			graph.releaseRef( ref );
			branchGraph.releaseRef( bvref );
		}
	}

	private double averageRadiusForThisBranchSpot( BranchSpot bs )
	{
		Spot ref = graph.vertexRef();
		try {
			Spot spot = branchGraph.getLinkedVertex( bs, ref );
			return Math.sqrt( spot.getBoundingSphereRadiusSquared() );
		}
		finally
		{
			graph.releaseRef( ref );
		}
	}
}
