package org.mastodon.revised.model.mamut.tm2;

import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.EDGE_FEATURE_DECLARATION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.EDGE_SOURCE_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.EDGE_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.EDGE_TARGET_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.FEATURE_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.FEATURE_DECLARATION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.FEATURE_ISINT_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.FEATURE_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.FRAME_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.ID_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.LABEL_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.MODEL_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.RADIUS_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.SPOT_COLLECTION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.SPOT_ELEMENT_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.SPOT_FEATURE_DECLARATION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.SPOT_FRAME_COLLECTION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.TRACK_COLLECTION_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.TRACK_TAG;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.VISIBILITY_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.X_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.Y_ATTRIBUTE_NAME;
import static org.mastodon.revised.model.mamut.tm2.TrackMateXMLKeys.Z_ATTRIBUTE_NAME;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mastodon.collection.IntRefMap;
import org.mastodon.collection.RefCollections;
import org.mastodon.features.DoubleFeature;
import org.mastodon.features.IntFeature;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;

public class TrackMateImporter
{
	public static Model importModel( final File file ) throws JDOMException, IOException
	{
		final Model model = new Model();
		final ModelGraph graph = model.getGraph();

		final SAXBuilder sb = new SAXBuilder();
		final Document document = sb.build( file );
		final Element root = document.getRootElement();
		final Element modelEl = root.getChild( MODEL_TAG );
		if ( null == modelEl ) { return null; }

		/*
		 * Read feature declaration and instantiate mastodon features.
		 */

		final Element featureDeclarationEl = modelEl.getChild( FEATURE_DECLARATION_TAG );

		// Spot features.
		final Element spotFeatureDeclarationEl = featureDeclarationEl.getChild( SPOT_FEATURE_DECLARATION_TAG );
		final List< Element > spotFeatureEls = spotFeatureDeclarationEl.getChildren( FEATURE_TAG );
		final Map< String, DoubleFeature< Spot > > spotDoubleFeatureMap = new HashMap<>();
		final Map< String, IntFeature< Spot > > spotIntFeatureMap = new HashMap<>();
		for ( final Element featureEl : spotFeatureEls )
		{
			final String featureKey = featureEl.getAttributeValue( FEATURE_ATTRIBUTE_NAME );
//			final String featureName = featureEl.getAttributeValue( FEATURE_NAME_ATTRIBUTE_NAME );
//			final String featureShortName = featureEl.getAttributeValue( FEATURE_SHORT_NAME_ATTRIBUTE_NAME );
//			final String featureDimension = featureEl.getAttributeValue( FEATURE_DIMENSION_ATTRIBUTE_NAME );
			final boolean featureIsInt = Boolean.parseBoolean( featureEl.getAttributeValue( FEATURE_ISINT_ATTRIBUTE_NAME ) );
			if ( featureIsInt )
			{
				final IntFeature< Spot > feature = new IntFeature<>( featureKey, Integer.MIN_VALUE );
				spotIntFeatureMap.put( featureKey, feature );
			}
			else
			{
				final DoubleFeature< Spot > feature = new DoubleFeature<>( featureKey, Double.NaN );
				spotDoubleFeatureMap.put( featureKey, feature );
			}
		}

		final Element edgeFeatureDeclarationEl = featureDeclarationEl.getChild( EDGE_FEATURE_DECLARATION_TAG );
		final List< Element > edgeFeatureEls = edgeFeatureDeclarationEl.getChildren( FEATURE_TAG );
		final Map< String, DoubleFeature< Link > > edgeDoubleFeatureMap = new HashMap<>();
		final Map< String, IntFeature< Link > > edgeIntFeatureMap = new HashMap<>();
		for ( final Element featureEl : edgeFeatureEls )
		{
			final String featureKey = featureEl.getAttributeValue( FEATURE_ATTRIBUTE_NAME );
//			final String featureName = featureEl.getAttributeValue( FEATURE_NAME_ATTRIBUTE_NAME );
//			final String featureShortName = featureEl.getAttributeValue( FEATURE_SHORT_NAME_ATTRIBUTE_NAME );
//			final String featureDimension = featureEl.getAttributeValue( FEATURE_DIMENSION_ATTRIBUTE_NAME );
			final boolean featureIsInt = Boolean.parseBoolean( featureEl.getAttributeValue( FEATURE_ISINT_ATTRIBUTE_NAME ) );
			if ( featureIsInt )
			{
				/*
				 * FIXME Fails because there are two feature with the same key:
				 * MANUAL_COLOR, one for spot, one for links. This triggers and
				 * DuplicateKeyException in the FeatureRegistry.
				 */
				final IntFeature< Link > feature = new IntFeature<>( featureKey, Integer.MIN_VALUE );
				edgeIntFeatureMap.put( featureKey, feature );
			}
			else
			{
				final DoubleFeature< Link > feature = new DoubleFeature<>( featureKey, Double.NaN );
				edgeDoubleFeatureMap.put( featureKey, feature );
			}

		}

		/*
		 * Read model and build the graph.
		 */

		final Spot ref = graph.vertexRef();
		final Spot putRef = graph.vertexRef();
		final Spot sourceRef = graph.vertexRef();
		final Spot targetRef = graph.vertexRef();
		final Link edgeRef = graph.edgeRef();

		try
		{

			final double[] pos = new double[ 3 ];

			// Map spot ID -> Vertex
			final IntRefMap< Spot > idToSpotIDmap = RefCollections.createIntRefMap( graph.vertices(), -1 );

			/*
			 * Import spots.
			 */
			final Element allSpotsEl = modelEl.getChild( SPOT_COLLECTION_TAG );
			final List< Element > allFramesEl = allSpotsEl.getChildren( SPOT_FRAME_COLLECTION_TAG );
			for ( final Element frameEl : allFramesEl )
			{
				final List< Element > frameSpotsEl = frameEl.getChildren( SPOT_ELEMENT_TAG );
				for ( final Element spotEl : frameSpotsEl )
				{
					final boolean visible = Integer.parseInt( spotEl.getAttributeValue( VISIBILITY_ATTRIBUTE_NAME ) ) != 0;
					if ( !visible )
						continue;

					// Create spot.
					pos[ 0 ] = Double.parseDouble( spotEl.getAttributeValue( X_ATTRIBUTE_NAME ) );
					pos[ 1 ] = Double.parseDouble( spotEl.getAttributeValue( Y_ATTRIBUTE_NAME ) );
					pos[ 2 ] = Double.parseDouble( spotEl.getAttributeValue( Z_ATTRIBUTE_NAME ) );
					final double radius = Double.parseDouble( spotEl.getAttributeValue( RADIUS_ATTRIBUTE_NAME ) );
					final int frame = Integer.parseInt( spotEl.getAttributeValue( FRAME_ATTRIBUTE_NAME ) );
					final int id = Integer.parseInt( spotEl.getAttributeValue( ID_ATTRIBUTE_NAME ) );
					final String label = spotEl.getAttributeValue( LABEL_ATTRIBUTE_NAME );

					final Spot spot = graph.addVertex( ref ).init( frame, pos, radius );
					spot.setLabel( label );
					idToSpotIDmap.put( id, spot, putRef );

					// Spot features.
					for ( final String featureKey : spotDoubleFeatureMap.keySet() )
					{
						final double val = Double.parseDouble( spotEl.getAttributeValue( featureKey ) );
						final DoubleFeature< Spot > feature = spotDoubleFeatureMap.get( featureKey );
						spot.feature( feature ).set( val );
					}
					for ( final String featureKey : spotIntFeatureMap.keySet() )
					{
						final int val = Integer.parseInt( spotEl.getAttributeValue( featureKey ) );
						final IntFeature< Spot > feature = spotIntFeatureMap.get( featureKey );
						spot.feature( feature ).set( val );
					}
				}
			}

			/*
			 * Import edges.
			 */
			final Element trackCollectionEl = modelEl.getChild( TRACK_COLLECTION_TAG );
			final List< Element > trakEls = trackCollectionEl.getChildren( TRACK_TAG );
			for ( final Element trackEl : trakEls )
			{
				final List< Element > edgeEls = trackEl.getChildren( EDGE_TAG );
				for ( final Element edgeEl : edgeEls )
				{
					// Create links.
					final int sourceID = Integer.parseInt( edgeEl.getAttributeValue( EDGE_SOURCE_ATTRIBUTE_NAME ) );
					final Spot source = idToSpotIDmap.get( sourceID, sourceRef );
					final int targetID = Integer.parseInt( edgeEl.getAttributeValue( EDGE_TARGET_ATTRIBUTE_NAME ) );
					final Spot target = idToSpotIDmap.get( targetID, targetRef );
					final Link link = graph.addEdge( source, target, edgeRef ).init();

					// Edge features.
					for ( final String featureKey : edgeDoubleFeatureMap.keySet() )
					{
						final double val = Double.parseDouble( edgeEl.getAttributeValue( featureKey ) );
						final DoubleFeature< Link > feature = edgeDoubleFeatureMap.get( featureKey );
						link.feature( feature ).set( val );
					}
					for ( final String featureKey : edgeIntFeatureMap.keySet() )
					{
						final int val = Integer.parseInt( edgeEl.getAttributeValue( featureKey ) );
						final IntFeature< Link > feature = edgeIntFeatureMap.get( featureKey );
						link.feature( feature ).set( val );
					}
				}
			}

			return model;
		}
		finally
		{
			graph.releaseRef( ref );
			graph.releaseRef( putRef );
			graph.releaseRef( sourceRef );
			graph.releaseRef( targetRef );
			graph.releaseRef( edgeRef );
		}
	}
}
