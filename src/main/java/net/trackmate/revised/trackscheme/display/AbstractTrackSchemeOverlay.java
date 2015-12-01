package net.trackmate.revised.trackscheme.display;

import java.awt.Graphics;
import java.awt.Graphics2D;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.ui.OverlayRenderer;
import net.trackmate.graph.collection.RefList;
import net.trackmate.revised.trackscheme.ScreenEdge;
import net.trackmate.revised.trackscheme.ScreenEntities;
import net.trackmate.revised.trackscheme.ScreenVertex;
import net.trackmate.revised.trackscheme.ScreenVertexRange;
import net.trackmate.revised.trackscheme.TrackSchemeGraph;
import net.trackmate.revised.trackscheme.TrackSchemeHighlight;

/**
 * An {@link OverlayRenderer} that paints {@link ScreenEntities} of a
 * TrackScheme graph. Comprises methods to paint vertices, edges, and dense
 * vertex ranges. It has no layout capabilities of its own; it just paints
 * laid-out screen objects.
 * <p>
 * It takes the laid-out {@link ScreenEntities} that it receives with the method
 * {@link #setScreenEntities(ScreenEntities)}, and can deal separately with
 * {@link ScreenVertex} and {@link ScreenVertexRange}.
 * <p>
 * This class is abstract and the details of how to paint vertices, edges and
 * background are delegated to concrete implementations. When the
 * {@link #drawOverlays(Graphics)} method is called, the following sequence of
 * abstract methods is executed:
 * <ol>
 * <li>{@link #paintBackground(Graphics2D, ScreenEntities)} to paint background
 * decorations.
 * <li> {@link #beforeDrawEdge(Graphics2D)} to configure the Graphics2D object
 * prior to painting edges.
 * <li> {@link #drawEdge(Graphics2D, ScreenEdge, ScreenVertex, ScreenVertex)} for
 * each edge.
 * <li>{@link #beforeDrawVertex(Graphics2D)} to configure the Graphics2D object
 * prior to painting vertices.
 * <li> {@link #drawVertex(Graphics2D, ScreenVertex)} for each vertex.
 * <li> {@link #beforeDrawVertexRange(Graphics2D)} to configure the Graphics2D
 * object prior to painting vertex ranges.
 * <li>{@link #drawVertexRange(Graphics2D, ScreenVertexRange)} for each vertex
 * range.
 * </ol>
 * <p>
 * It also offers facilities to interrogate what has been painted where, to
 * facilitate writing user interfaces. For instance, it can return the
 * TrackScheme edge or vertex id near a screen xy coordinate.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public abstract class AbstractTrackSchemeOverlay implements OverlayRenderer
{
	private int width;

	private int height;

	/**
	 * The {@link ScreenEntities} that are actually drawn on the canvas.
	 */
	protected final ScreenEntities entities;

	/**
	 * {@link ScreenEntities} that have been previously
	 * {@link #setScreenEntities(ScreenEntities) set} for painting. Whenever new
	 * entities are set, these are stored here and marked {@link #pending}. Whenever
	 * entities are painted and new entities are pending, the new entities are painted
	 * to the screen. Before doing this, the entities previously used for painting
	 * are swapped into {@link #pendingEntities}. This is used for double-buffering.
	 */
	private ScreenEntities pendingEntities;

	/**
	 * Whether new entitites are pending.
	 */
	private boolean pending;

	protected final TrackSchemeHighlight highlight;

	protected int highlightedVertexId;

	private int minTimepoint = 0;

	private int maxTimepoint = 100;

	private int currentTimepoint = 0;

	/**
	 * Creates a new overlay for the specified TrackScheme graph.
	 * 
	 * @param graph
	 *            the graph to paint.
	 * @param highlight
	 *            the highlight model that indicates what vertex is highlighted.
	 * @param options
	 *            options for TrackScheme look.
	 */
	public AbstractTrackSchemeOverlay(
			final TrackSchemeGraph< ?, ? > graph,
			final TrackSchemeHighlight highlight,
			final TrackSchemeOptions options )
	{
		this.highlight = highlight;
		width = options.values.getWidth();
		height = options.values.getHeight();
		entities = new ScreenEntities( graph );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D g2 = ( Graphics2D ) g;

		swapScreenEntities();

		highlightedVertexId = highlight.getHighlightedVertexId();

		paintBackground( g2, entities );

		final RefList< ScreenEdge > edges = entities.getEdges();
		final RefList< ScreenVertex > vertices = entities.getVertices();
		final RefList< ScreenVertexRange > vertexRanges = entities.getRanges();

		final ScreenVertex vt = vertices.createRef();
		final ScreenVertex vs = vertices.createRef();

		beforeDrawEdge( g2 );
		for ( final ScreenEdge edge : edges )
		{
			vertices.get( edge.getSourceScreenVertexIndex(), vs );
			vertices.get( edge.getTargetScreenVertexIndex(), vt );
			drawEdge( g2, edge, vs, vt );
		}

		beforeDrawVertex( g2 );
		for ( final ScreenVertex vertex : vertices )
		{
			drawVertex( g2, vertex );
		}

		beforeDrawVertexRange( g2 );
		for ( final ScreenVertexRange range : vertexRanges )
		{
			drawVertexRange( g2, range );
		}

		vertices.releaseRef( vs );
		vertices.releaseRef( vt );
	}

	/**
	 * Returns the internal pool index of the TrackSchemeEdge currently painted
	 * on this display at screen coordinates specified by {@code x} and
	 * {@code y} and within a distance tolerance.
	 * <p>
	 * This method exists to facilitate writing mouse handlers.
	 *
	 * @param x
	 *            the x screen coordinate
	 * @param y
	 *            the y screen coordinate
	 * @param tolerance
	 *            the maximal distance to the closest edge.
	 * @return the internal pool index of the TrackSchemeEdge at {@code (x, y)},
	 *         or -1 if there is no edge within the distance tolerance.
	 */
	public int getEdgeIdAt( final int x, final int y, final double tolerance )
	{
		synchronized ( entities )
		{
			final RealPoint pos = new RealPoint( x, y );
			final RefList< ScreenVertex > vertices = entities.getVertices();
			final ScreenVertex vt = vertices.createRef();
			final ScreenVertex vs = vertices.createRef();
			for ( final ScreenEdge e : entities.getEdges() )
			{
				vertices.get( e.getSourceScreenVertexIndex(), vs );
				vertices.get( e.getTargetScreenVertexIndex(), vt );
				if ( distanceToPaintedEdge( pos, e, vs, vt ) <= tolerance ) { return e.getTrackSchemeEdgeId(); }
			}
		}
		return -1;
	}

	/**
	 * Returns the internal pool index of the {@link TrackSchemeVertex}
	 * currently painted on this display at screen coordinates specified by
	 * {@code x} and {@code y}.
	 * <p>
	 * This method exists to facilitate writing mouse handlers.
	 *
	 * @param x
	 *            the x screen coordinate
	 * @param y
	 *            the y screen coordinate
	 * @return the internal pool index of the {@link TrackSchemeVertex} at
	 *         {@code (x, y)}, or -1 if there is no vertex at this position.
	 */
	public int getVertexIdAt( final int x, final int y )
	{
		synchronized ( entities )
		{
			final RealPoint pos = new RealPoint( x, y );
			for ( final ScreenVertex v : entities.getVertices() )
				if ( isInsidePaintedVertex( pos, v ) )
					return v.getTrackSchemeVertexId();
			return -1;
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	/**
	 * Returns the width of this overlay.
	 * 
	 * @return the width.
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Returns the hight of this overlay.
	 * 
	 * @return the height.
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * Set the timepoint range of the dataset.
	 *
	 * @param minTimepoint
	 *            the smallest timepoint to display.
	 * @param maxTimepoint
	 *            the largest timepoint to display.
	 */
	public void setTimepointRange( final int minTimepoint, final int maxTimepoint )
	{
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
	}

	/**
	 * Sets the current timepoint.
	 *
	 * @param timepoint
	 */
	public void setCurrentTimepoint( final int timepoint )
	{
		this.currentTimepoint  = timepoint;
	}

	/**
	 * Returns the smallest timepoint displayed.
	 * 
	 * @return the smallest timepoint displayed.
	 */
	protected int getMinTimepoint()
	{
		return minTimepoint;
	}

	/**
	 * Returns the largest timepoint displayed.
	 * 
	 * @return the largest timepoint displayed.
	 */
	protected int getMaxTimepoint()
	{
		return maxTimepoint;
	}

	/**
	 * Returns the timepoint currently displayed on this overlay.
	 * 
	 * @return the timepoint currently displayed.
	 */
	protected int getCurrentTimepoint()
	{
		return currentTimepoint;
	}

	/**
	 * Set the {@link ScreenEntities} to paint.
	 *
	 * @param entities
	 *            {@link ScreenEntities} to paint.
	 */
	public synchronized ScreenEntities setScreenEntities( final ScreenEntities entities )
	{
		final ScreenEntities tmp = pendingEntities;
		pendingEntities = entities;
		pending = true;
		return tmp;
	}

	/**
	 * Provides subclass access to {@link ScreenEntities} to paint.
	 * Implements double-buffering.
	 *
	 * @return current {@link ScreenEntities}.
	 */
	private synchronized ScreenEntities swapScreenEntities()
	{
		if ( pending )
		{
			synchronized ( entities )
			{
				entities.set( pendingEntities );
				pending = false;
			}
		}
		return entities;
	}

	/**
	 * Returns <code>true</code> if the specified <b>layout</b> coordinates are
	 * inside a painted vertex. As the vertex painting shape is implemented by
	 * possibly different concrete classes, they should return whether a point
	 * is inside a vertex or not.
	 * 
	 * @param pos
	 *            the layout position.
	 * @param vertex
	 *            the vertex.
	 * @return <code>true</code> if the position is inside the vertex painted.
	 */
	protected abstract boolean isInsidePaintedVertex( final RealLocalizable pos, final ScreenVertex vertex );

	/**
	 * Returns the distance from a <b>layout</b> position to a specified edge.
	 * 
	 * @param pos
	 *            the layout position.
	 * @param edge
	 *            the edge.
	 * @param source
	 *            the edge source vertex.
	 * @param target
	 *            the edge target vertex.
	 * @return the distance from the specified position to the edge.
	 */
	protected abstract double distanceToPaintedEdge( final RealLocalizable pos, final ScreenEdge edge, ScreenVertex source, ScreenVertex target );

	/**
	 * Paints background decorations.
	 * 
	 * @param g2
	 *            the graphics object.
	 * @param screenEntities
	 *            the screen entities to paint.
	 */
	protected abstract void paintBackground( Graphics2D g2, ScreenEntities screenEntities );

	/**
	 * Configures the graphics object prior to drawing vertices.
	 * 
	 * @param g2
	 *            the graphics object.
	 */
	protected abstract void beforeDrawVertex( Graphics2D g2 );

	/**
	 * Paints the specified vertex.
	 * 
	 * @param g2
	 *            the graphics object.
	 * @param vertex
	 *            the vertex to paint.
	 */
	protected abstract void drawVertex( Graphics2D g2, ScreenVertex vertex );

	/**
	 * Configures the graphics object prior to drawing vertex ranges.
	 * 
	 * @param g2
	 *            the graphics object.
	 */
	protected abstract void beforeDrawVertexRange( Graphics2D g2 );

	/**
	 * Paints the specified vertex range.
	 * 
	 * @param g2
	 *            the graphics object.
	 * @param range
	 *            the vertex range to paint.
	 */
	protected abstract void drawVertexRange( Graphics2D g2, ScreenVertexRange range );

	/**
	 * Configures the graphics object prior to drawing edges.
	 * 
	 * @param g2
	 *            the graphics object.
	 */
	protected abstract void beforeDrawEdge( Graphics2D g2 );

	/**
	 * Paints the specified edge.
	 * 
	 * @param g2
	 *            the graphics object.
	 * @param edge
	 *            the edge to paint.
	 * @param vs
	 *            the edge source vertex.
	 * @param vt
	 *            the edge target vertex.
	 */
	protected abstract void drawEdge( Graphics2D g2, ScreenEdge edge, ScreenVertex vs, ScreenVertex vt );
}
