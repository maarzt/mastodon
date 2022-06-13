/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.views.trackscheme.display;

import javax.swing.SwingUtilities;

import org.mastodon.adapter.SelectionModelAdapter;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.mamut.model.branch.BranchLink;
import org.mastodon.mamut.model.branch.BranchSpot;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.trackscheme.DepthFirstIteration;
import org.mastodon.views.trackscheme.LexicographicalVertexOrder;
import org.mastodon.views.trackscheme.LongEdgesLineageTreeLayout;
import org.mastodon.views.trackscheme.TrackSchemeEdge;
import org.mastodon.views.trackscheme.TrackSchemeGraph;
import org.mastodon.views.trackscheme.TrackSchemeVertex;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

/**
 * These actions allow to limit the view of the
 * {@link org.mastodon.mamut.MamutBranchViewTrackScheme}
 * and {@link org.mastodon.mamut.MamutBranchViewTrackSchemeHierarchy}
 * to only show selected tracks or downward tracks.
 */
public class ShowSelectedTracksActions
{

	public static final String SHOW_TRACK_DOWNWARD = "ts show track downward";
	public static final String SHOW_SELECTED_TRACKS = "ts show selected tracks";
	public static final String SHOW_ALL_TRACKS = "ts show all tracks";

	public static final String[] SHOW_TRACK_DOWNWARD_KEYS = {"ctrl PAGE_DOWN"};
	public static final String[] SHOW_SELECTED_TRACKS_KEYS = {"ctrl SPACE"};
	public static final String[] SHOW_ALL_TRACKS_KEYS = {"ctrl DELETE"};

	/**
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( SHOW_TRACK_DOWNWARD, SHOW_TRACK_DOWNWARD_KEYS, "Show only the downward tracks of selected spots and in the branch / hierarchy TrackScheme." );
			descriptions.add( SHOW_SELECTED_TRACKS, SHOW_SELECTED_TRACKS_KEYS, "Show only the tracks with selected spots in the branch / hierarchy TrackScheme." );
			descriptions.add( SHOW_ALL_TRACKS, SHOW_ALL_TRACKS_KEYS, "Show all tracks in the branch / hierarchy TrackScheme." );
		}
	}

	private final TrackSchemeGraph<BranchSpot, BranchLink> viewGraph;

	private final LongEdgesLineageTreeLayout layout;

	private final SelectionModel<TrackSchemeVertex, TrackSchemeEdge> selectionModel;

	private final TrackSchemePanel panel;

	private final RunnableAction showTrackDownwardAction = new RunnableAction( SHOW_TRACK_DOWNWARD, this::showTrackDownward );

	private final RunnableAction showSelectedTracksAction = new RunnableAction( SHOW_SELECTED_TRACKS, this::showSelectedTracks );

	private final RunnableAction showAllTracksAction = new RunnableAction( SHOW_ALL_TRACKS, this::showAllTracks );

	public static void install( Actions actions, TrackSchemeGraph<BranchSpot, BranchLink> viewGraph, SelectionModelAdapter<BranchSpot, BranchLink, TrackSchemeVertex, TrackSchemeEdge> selectionModel, TrackSchemePanel trackschemePanel )
	{
		new ShowSelectedTracksActions( viewGraph, selectionModel, trackschemePanel).install(actions);
	}

	private ShowSelectedTracksActions( TrackSchemeGraph<BranchSpot, BranchLink> viewGraph, SelectionModel<TrackSchemeVertex, TrackSchemeEdge> selectionModel, TrackSchemePanel panel )
	{
		this.viewGraph = viewGraph;
		this.layout = ( LongEdgesLineageTreeLayout ) panel.getLineageTreeLayout();
		this.selectionModel = selectionModel;
		this.panel = panel;
		selectionModelChanged();
		selectionModel.listeners().add( this::selectionModelChanged );
	}

	private void selectionModelChanged()
	{
		SwingUtilities.invokeLater( () -> {
			showTrackDownwardAction.setEnabled( ! this.selectionModel.isEmpty() );
			showSelectedTracksAction.setEnabled( ! this.selectionModel.isEmpty() );
		} );
	}

	private void install( Actions actions )
	{
		actions.namedAction( showTrackDownwardAction, SHOW_TRACK_DOWNWARD_KEYS );
		actions.namedAction( showSelectedTracksAction, SHOW_SELECTED_TRACKS_KEYS );
		actions.namedAction( showAllTracksAction, SHOW_ALL_TRACKS_KEYS );
	}

	public void showTrackDownward()
	{
		layout.setRoots( getSelectedSubtreeRoots() );
		panel.graphChanged();
		panel.getTransformEventHandler().zoomOutFully();
	}

	private void showSelectedTracks()
	{
		layout.setRoots( getSelectedWholeTrackRoots() );
		panel.graphChanged();
		panel.getTransformEventHandler().zoomOutFully();
	}

	public void showAllTracks()
	{
		layout.setRoots( new RefArrayList<>( viewGraph.getVertexPool() ) );
		panel.graphChanged();
		panel.getTransformEventHandler().zoomOutFully();
	}

	private RefList<TrackSchemeVertex> getSelectedSubtreeRoots()
	{
		RefSet<TrackSchemeVertex> selectedNodes = new RefSetImp<>( viewGraph.getVertexPool() );
		selectedNodes.addAll( selectionModel.getSelectedVertices() );
		addEdgeTargets( selectedNodes, selectionModel.getSelectedEdges() );
		return filterRootNodes( selectedNodes );
	}

	private RefList<TrackSchemeVertex> getSelectedWholeTrackRoots()
	{
		RefSet<TrackSchemeVertex> selectedNodes = new RefSetImp<>( viewGraph.getVertexPool() );
		selectedNodes.addAll( selectionModel.getSelectedVertices() );
		addEdgeTargets( selectedNodes, selectionModel.getSelectedEdges() );
		return getRealRoots( selectedNodes );
	}

	private void addEdgeTargets( RefSet<TrackSchemeVertex> selected, RefSet<TrackSchemeEdge> selectedEdges )
	{
		TrackSchemeVertex targetRef = viewGraph.vertexRef();
		for(TrackSchemeEdge edge : selectedEdges )
			selected.add(edge.getTarget(targetRef));
		viewGraph.releaseRef( targetRef );
	}

	private RefList<TrackSchemeVertex> filterRootNodes( RefSet<TrackSchemeVertex> selectedVertices )
	{
		RefList<TrackSchemeVertex> roots = new RefArrayList<>( viewGraph.getVertexPool() );
		DepthFirstIteration<TrackSchemeVertex> df = new DepthFirstIteration<>( viewGraph );
		df.setExcludeNodeAction( node -> {
			boolean isSelected = selectedVertices.contains( node );
			if(isSelected)
				roots.add(node);
			return isSelected;
		} );
		for( TrackSchemeVertex realRoot : LexicographicalVertexOrder.sort( viewGraph, viewGraph.getRoots() ) )
			df.runForRoot( realRoot );
		return roots;
	}

	private RefList<TrackSchemeVertex> getRealRoots( RefSet<TrackSchemeVertex> selectedNodes )
	{
		TrackSchemeVertex parent = viewGraph.vertexRef();
		RefSet<TrackSchemeVertex> roots = new RefSetImp<>( viewGraph.getVertexPool() );
		A: for(TrackSchemeVertex vertex : selectedNodes ) {
			parent.refTo( vertex );
			while ( ! parent.incomingEdges().isEmpty() )
			{
				parent.incomingEdges().iterator().next().getSource( parent );
				if ( selectedNodes.contains( parent ) )
					continue A;
			}
			roots.add( parent );
		}
		viewGraph.releaseRef( parent );
		return LexicographicalVertexOrder.sort( viewGraph, roots );
	}

}
