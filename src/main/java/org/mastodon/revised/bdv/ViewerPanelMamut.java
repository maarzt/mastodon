package org.mastodon.revised.bdv;

import java.util.List;

import bdv.cache.CacheControl;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;

public class ViewerPanelMamut extends ViewerPanel
{
	private static final long serialVersionUID = 1L;

	private final BehaviourTransformEventHandlerMamut transformEventHandler;

	public ViewerPanelMamut( final List< SourceAndConverter< ? > > sources, final int numTimepoints, final CacheControl cacheControl, final ViewerOptions optional )
	{
		super( sources, numTimepoints, cacheControl,
				is2D( sources )
						? optional.transformEventHandlerFactory( BehaviourTransformEventHandler3DMamut::new )
						: optional.transformEventHandlerFactory( BehaviourTransformEventHandler2DMamut::new ) );
		transformEventHandler = ( BehaviourTransformEventHandlerMamut ) display.getTransformEventHandler();
	}

	private static boolean is2D( final List< SourceAndConverter< ? > > sources )
	{
		return true;
	}

	public BehaviourTransformEventHandlerMamut getTransformEventHandler()
	{
		return transformEventHandler;
	}

	@Override
	public synchronized void align( final AlignPlane plane )
	{
		super.align( plane );
	}
}
