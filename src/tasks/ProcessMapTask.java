package tasks;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPolygon;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolygons;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.VecBufferSequence;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.DoubleStream;

import javax.media.opengl.GL2;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.stage.Screen;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
//import org.geotools.filter.Filter;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.JenksNaturalBreaksFunction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;

import sun.java2d.DestSurfaceProvider;
import utils.ProyectionConstants;

import com.jogamp.common.nio.Buffers;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import dao.Clasificador;
import dao.Configuracion;
import dao.CosechaItem;
import dao.FeatureContainer;
import dao.FertilizacionLabor;
import dao.Labor;
import dao.Producto;
//import org.opengis.filter.FilterFactory2;

public abstract class ProcessMapTask<FC extends FeatureContainer,E extends Labor<FC>> extends Task<E>{
	public static final String ZOOM_TO_KEY = "ZOOM_TO";
	protected int featureCount=0;
	protected int featureNumber=0;
	//protected RenderableLayer layer = null;//new Group();
	protected E labor;

	protected ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();
	
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	public ProcessMapTask() {
	}
	
	public ProcessMapTask(E labor2) {
		labor=labor2;
	}

	@Override
	protected E call() throws Exception {

		try {
			doProcess();
		} catch (Exception e1) {
			System.err.println("Error al procesar el Shape de cosechas");
			e1.printStackTrace();
		}

		return labor;
	}

	protected abstract void doProcess()throws IOException ;

	protected abstract int getAmountMin() ;
	protected abstract int gerAmountMax() ;


	protected gov.nasa.worldwind.render.ExtrudedPolygon  getPathFromGeom(Geometry poly, FeatureContainer dao) {	
		// Set the coordinates (in degrees) to draw your polygon
		// To radians just change the method the class Position
		// to fromRadians().
		ArrayList<Position> positions = new ArrayList<Position>();     

		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];

			double z = coord.z>=0?coord.z:0;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			//XXX no entiendo que mierda quise decir :(
			Position pos = Position.fromDegrees(coord.y,coord.x,z);
			//			System.out.println(i+" "+pos.toString());//XXX sacar comment
			positions.add(pos);
		}

		gov.nasa.worldwind.render.ExtrudedPolygon  renderablePolygon = new gov.nasa.worldwind.render.ExtrudedPolygon (positions);

		Color currentColor = null;
		try{
			currentColor = labor.getClasificador().getColorFor(dao.getAmount());
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}
		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		Material material = new Material(awtColor);

		ShapeAttributes outerAttributes = new BasicShapeAttributes();

		outerAttributes.setOutlineWidth(0.01);
		outerAttributes.setOutlineOpacity(0.01);
		outerAttributes.setDrawInterior(true);
		outerAttributes.setDrawOutline(false);
		outerAttributes.setInteriorOpacity(1);
		outerAttributes.setInteriorMaterial(material);
		//normalAttributes.setOutlineMaterial(material);
		renderablePolygon.setAttributes(outerAttributes);

		ShapeAttributes sideAttributes = new BasicShapeAttributes();
		sideAttributes.setOutlineWidth(0.01);
		sideAttributes.setOutlineOpacity(0.01);
		sideAttributes.setDrawInterior(true);
		sideAttributes.setDrawOutline(false);
		sideAttributes.setInteriorOpacity(1);
		sideAttributes.setInteriorMaterial(Material.BLACK);
		renderablePolygon.setSideAttributes(sideAttributes);

		renderablePolygon.setAltitudeMode(WorldWind.ABSOLUTE);


		return renderablePolygon;
	}

	/**
	 * 
	 * @param inputGeom
	 * @param dao
	 * @param tooltipText 
	 * @return devuelve el objeto rendereable que se agrega en la coleccion de pathaTooltips y se muestra en runlater()
	 */
	protected List<gov.nasa.worldwind.render.Polygon>  getPathFromGeom2D(Geometry inputGeom, FeatureContainer dao, String tooltipText) {
		if(inputGeom.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ inputGeom);
			return null;
		}

		Color currentColor = null;
		try{
			currentColor = labor.getClasificador().getColorFor(dao.getAmount());
		}catch(Exception e){
			//e.printStackTrace();
			currentColor = Color.WHITE;
		}
		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		Material material = new Material(awtColor);

		ShapeAttributes outerAttributes = new BasicShapeAttributes();
		outerAttributes.setOutlineWidth(0.01);
		outerAttributes.setOutlineOpacity(0.01);
		outerAttributes.setDrawInterior(true);
		outerAttributes.setDrawOutline(false);
		outerAttributes.setInteriorOpacity(0.8);
		outerAttributes.setInteriorMaterial(material);

		List<gov.nasa.worldwind.render.Polygon> renderablePolygons = new ArrayList<>();

		for(int geometryN=0;geometryN<inputGeom.getNumGeometries();geometryN++){
			Geometry forGeom = inputGeom.getGeometryN(geometryN);
			if(forGeom instanceof Polygon){
				gov.nasa.worldwind.render.Polygon  renderablePolygon = new gov.nasa.worldwind.render.Polygon();
				renderablePolygon.setAttributes(outerAttributes);

				Polygon forPolygon = (Polygon)forGeom;

				List<Position> exteriorPositions = coordinatesToPositions(forPolygon.getExteriorRing().getCoordinates());
				renderablePolygon.setOuterBoundary(exteriorPositions);

				for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
					List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
					renderablePolygon.addInnerBoundary(interiorPositions);
				}
				renderablePolygon.setValue(AVKey.DISPLAY_NAME, tooltipText);
				labor.getLayer().addRenderable(renderablePolygon);
				//renderablePolygons.add(renderablePolygon);

			}				
		}//termine de recorrer el multipoligono

		return renderablePolygons;
	}
	
	
	protected List<SurfacePolygon>  getSurfacePolygons(Geometry inputGeom, FeatureContainer dao) {
		if(inputGeom.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ inputGeom);
			return null;
		}

		Color currentColor = null;
		try{
			currentColor = labor.getClasificador().getColorFor(dao.getAmount());
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}
		java.awt.Color awtColor = new java.awt.Color((float) currentColor.getRed(),(float) currentColor.getGreen(),(float) currentColor.getBlue());
		Material material = new Material(awtColor);

		ShapeAttributes outerAttributes = new BasicShapeAttributes();
		outerAttributes.setOutlineWidth(0.01);
		outerAttributes.setOutlineOpacity(0.01);
		outerAttributes.setDrawInterior(true);
		outerAttributes.setDrawOutline(false);
		outerAttributes.setInteriorOpacity(0.8);
		outerAttributes.setInteriorMaterial(material);

		List<SurfacePolygon> renderablePolygons = new ArrayList<>();

		

	            
		for(int geometryN=0;geometryN<inputGeom.getNumGeometries();geometryN++){
			Geometry forGeom = inputGeom.getGeometryN(geometryN);
			if(forGeom instanceof Polygon){
//				List<LatLon> locations = Arrays.asList(
//		                LatLon.fromDegrees(20, -170),
//		                LatLon.fromDegrees(15, 170),
//		                LatLon.fromDegrees(10, -175),
//		                LatLon.fromDegrees(5, 170),
//		                LatLon.fromDegrees(0, -170),
//		                LatLon.fromDegrees(20, -170));
		            SurfacePolygon renderablePolygon = new SurfacePolygon();
		           
		        //    shape.setAttributes(outerAttributes);
		            
			//	gov.nasa.worldwind.render.Polygon  renderablePolygon = new gov.nasa.worldwind.render.Polygon();
				renderablePolygon.setAttributes(outerAttributes);

				Polygon forPolygon = (Polygon)forGeom;

				List<Position> exteriorPositions = coordinatesToPositions(forPolygon.getExteriorRing().getCoordinates());
				renderablePolygon.setOuterBoundary(exteriorPositions);

				for(int interiorN=0;interiorN<forPolygon.getNumInteriorRing();interiorN++){
					List<Position> interiorPositions = coordinatesToPositions(forPolygon.getInteriorRingN(interiorN).getCoordinates());
					renderablePolygon.addInnerBoundary(interiorPositions);
				}
				
				renderablePolygons.add(renderablePolygon);

			}			
			
		}//termine de recorrer el multipoligono

		return renderablePolygons;
	}
	
	/**
	 * este metodo anda bien pero no se muestra nada en la pantalla ???
	 * @param inputGeom
	 * @param attrs
	 * @return
	 */
	private SurfacePolygons createSurfacePolygons(Geometry inputGeom, ShapeAttributes attrs){
		
		//record.getCompoundPointBuffer()
		//(ShapefileRecordPolygon) record).getBoundingRectangle()
		Envelope envelope = inputGeom.getEnvelopeInternal();
		double minLongitude=envelope.getMinX();
		double minLatitude=envelope.getMaxX();
		double maxLatitude=envelope.getMinY();
		double maxLongitude=envelope.getMaxY();
		Sector sector =  Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude);
		
      
        
      Coordinate[] coords = inputGeom.getCoordinates();
      
      DoubleBuffer doubleBuffer = Buffers.newDirectDoubleBuffer(2 * coords.length);
      VecBufferSequence compoundVecBuffer =new VecBufferSequence(
              new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(doubleBuffer)));
      
      for(int i =0;i<coords.length;i++){
    	  Coordinate c = coords[i];
    	 // doubleBuffer.put(new double[]{c.x,c.y});
    	  DoubleBuffer pointBuffer =DoubleBuffer.wrap(new double[]{c.x,c.y});//aca pongo las coordenadas del punto
    	  VecBuffer vecBuffer = new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(pointBuffer));
    	 
    		  compoundVecBuffer.append(vecBuffer);
      }
     
		SurfacePolygons surfacePolygons = new SurfacePolygons(sector,compoundVecBuffer){
			protected void drawInterior(DrawContext dc, SurfaceTileDrawContext sdc){
				
				  // Exit immediately if the polygon has no coordinate data.
		        if (this.buffer.size() == 0)
		            return;

		        Position referencePos = this.getReferencePosition();
		        if (referencePos == null)
		            return;

		        // Attempt to tessellate the polygon's interior if the polygon's interior display list is uninitialized, or if
		        // the polygon is marked as needing tessellation.
		        int[] dlResource = (int[]) dc.getGpuResourceCache().get(this.interiorDisplayListCacheKey);
		        if (dlResource == null || this.needsInteriorTessellation)
		            dlResource = this.tessellateInterior(dc, referencePos);

		        // Exit immediately if the polygon's interior failed to tessellate. The cause has already been logged by
		        // tessellateInterior().
		        if (dlResource == null)
		            return;

		        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		        this.applyInteriorState(dc, sdc, this.getActiveAttributes(), this.getTexture(), referencePos);
		        gl.glCallList(dlResource[0]);

		        if (this.crossesDateLine)
		        {
		            gl.glPushMatrix();
		            try
		            {
		                // Apply hemisphere offset and draw again
		                double hemisphereSign = Math.signum(referencePos.getLongitude().degrees);
		                gl.glTranslated(360 * hemisphereSign, 0, 0);
		                gl.glCallList(dlResource[0]);
		            }
		            finally
		            {
		                gl.glPopMatrix();
		            }
		        }
			}
		};
            surfacePolygons.setAttributes(attrs);
            // Configure the SurfacePolygons as a single large polygon.
            // Configure the SurfacePolygons to correctly interpret the Shapefile polygon record. Shapefile polygons may
            // have rings defining multiple inner and outer boundaries. Each ring's winding order defines whether it's an
            // outer boundary or an inner boundary: outer boundaries have a clockwise winding order. However, the
            // arrangement of each ring within the record is not significant; inner rings can precede outer rings and vice
            // versa.
            //
            // By default, SurfacePolygons assumes that the sub-buffers are arranged such that each outer boundary precedes
            // a set of corresponding inner boundaries. SurfacePolygons traverses the sub-buffers and tessellates a new
            // polygon each  time it encounters an outer boundary. Outer boundaries are sub-buffers whose winding order
            // matches the SurfacePolygons' windingRule property.
            //
            // This default behavior does not work with Shapefile polygon records, because the sub-buffers of a Shapefile
            // polygon record can be arranged arbitrarily. By calling setPolygonRingGroups(new int[]{0}), the
            // SurfacePolygons interprets all sub-buffers as boundaries of a single tessellated shape, and configures the
            // GLU tessellator's winding rule to correctly interpret outer and inner boundaries (in any arrangement)
            // according to their winding order. We set the SurfacePolygons' winding rule to clockwise so that sub-buffers
            // with a clockwise winding ordering are interpreted as outer boundaries.
            surfacePolygons.setWindingRule(AVKey.CLOCKWISE);
            surfacePolygons.setPolygonRingGroups(new int[] {0});
            surfacePolygons.setPolygonRingGroups(new int[] {0});
           return surfacePolygons;// layer.addRenderable(shape);
	}

	private List<Position> coordinatesToPositions(Coordinate[] coordinates){
		ArrayList<Position> positions = new ArrayList<Position>();     
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate coord = coordinates[i];			
			double z =1;//coord.z>=0?coord.z:0;//XXX hacer que dibujar las coordenadas sea opcional o mover las alturas al model (mas difici)
			Position pos = Position.fromDegrees(coord.y,coord.x,z);
			positions.add(pos);
		}
		return positions;
	}

	@Deprecated
	protected Path getPathFromGeom(Geometry poly, Integer colorIndex) {			
		Path path = new Path();		
		/**
		 * recorro el vector de puntos que contiene el poligono gis y creo un
		 * path para dibujarlo
		 */
		if(poly.getNumPoints()==0){
			System.err.println("dibujando un path con cero puntos "+ poly);
			return null;
		}
		for (int i = 0; i < poly.getNumPoints(); i++) {
			Coordinate coord = poly.getCoordinates()[i];
			// como las coordenadas estan en long/lat las convierto a metros
			// para dibujarlas con menos error.
			double x = coord.x / ProyectionConstants.metersToLong;
			double y = coord.y /ProyectionConstants.metersToLat;
			if (i == 0) {
				path.getElements().add(new MoveTo(x, y)); // primero muevo el
			}
			path.getElements().add(new LineTo(x, y));// dibujo una linea desde
		}

		Paint currentColor = null;
		try{
			currentColor = Clasificador.colors[colorIndex];
		}catch(Exception e){
			e.printStackTrace();
			currentColor = Color.WHITE;
		}

		path.setFill(currentColor);
		path.setStrokeWidth(0.05);

		path.getStyleClass().add(currentColor.toString());//esto me permite luego asignar un estilo a todos los objetos con la clase "currentColor.toString()"
		return path;
	}

	/**
	 * metodo usado por las capas de siembra fertilizacion, pulverizacion y suelo para obtener los poligonos
	 * @param dao
	 * @return
	 */
	protected List<Polygon> getPolygons(FeatureContainer dao){
		List<Polygon> polygons = new ArrayList<Polygon>();
		Object geometry = dao.getGeometry();
		//	System.out.println("obteniendo los poligonos de "+geometry);

		if (geometry instanceof MultiPolygon) {		
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry g = mp.getGeometryN(i);
				if(g instanceof Polygon){
					polygons.add((Polygon) g);
				}				
			}

		} else if (geometry instanceof Polygon) {
			polygons.add((Polygon) geometry);
		} else if(geometry instanceof Point){ 
			//si es una capa de puntos lo cambio por una capa de cuadrados de lado 5mts
			Point p = (Point) geometry;
			GeometryFactory fact = p.getFactory();
			Double r = 5*ProyectionConstants.metersToLat;

			Coordinate D = new Coordinate(p.getX() - r , p.getY() + r ); // x-l-d
			Coordinate C = new Coordinate(p.getX() + r , p.getY()+ r);// X+l-d
			Coordinate B = new Coordinate(p.getX() + r , p.getY() - r );// X+l+d
			Coordinate A = new Coordinate(p.getX() - r , p.getY() -r );// X-l+d

			Coordinate[] coordinates = { A, B, C, D, A };// Tiene que ser cerrado.

			// PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
			// fact= new GeometryFactory(pm);

			LinearRing shell = fact.createLinearRing(coordinates);
			LinearRing[] holes = null;
			Polygon poly = new Polygon(shell, holes, fact);

			polygons.add(poly);
			System.out.println("creando polygon default");//Las geometrias son POINT. que hago?
			//TODO crear un poligono default

		}
		//System.out.println("devolviendo los polygons "+polygons);
		return polygons;
	}
	protected abstract void getPathTooltip(Geometry p, FC  fc);

	protected void runLater(Collection<FC> itemsToShow) {	
		//this.pathTooltips.clear();
		labor.getLayer().removeAllRenderables();
		for(FC c:itemsToShow){
			Geometry g = c.getGeometry();
			if(g instanceof Polygon){
				//	pathTooltips.add(
				getPathTooltip((Polygon)g,c);
				//		);	
			} else if(g instanceof MultiPolygon){
				MultiPolygon mp = (MultiPolygon)g;			
				for(int i=0;i<mp.getNumGeometries();i++){
					Polygon p = (Polygon) (mp).getGeometryN(i);
					getPathTooltip(p,c);
					//	pathTooltips.add(getPathTooltip(p,c));	
				}

			}
		}
		
		//int i =0;
	//	labor.getLayer().removeAllRenderables();

//		for (ArrayList<Object> pathTooltip : pathTooltips) {
//			updateProgress(i, pathTooltips.size());
//			i++;
//			gov.nasa.worldwind.render.Polygon path = (gov.nasa.worldwind.render.Polygon) pathTooltip.get(0);
//			//	String tooltipText = (String) pathTooltip.get(1);				
//			labor.getLayer().addRenderable(path);		
//
//		}

		//gov.nasa.worldwind.render.Polygon path = (gov.nasa.worldwind.render.Polygon) pathTooltips.get(0).get(0);
	//	SurfacePolygon path = (SurfacePolygon) pathTooltips.get(0).get(0);
		Coordinate centre = labor.outCollection.getBounds().centre();
	//	LatLon latlon = LatLon.fromDegrees(centre.y, centre.x);
	//	LatLon latlon=	path.getReferencePosition();
		
		Position pointPosition = //new Position(latlon.latitude,latlon.longitude);
				Position.fromDegrees(centre.y, centre.x);
		PointPlacemark pmStandard = new PointPlacemark(pointPosition);
		PointPlacemarkAttributes pointAttribute = new PointPlacemarkAttributes();
		pointAttribute.setImageColor(java.awt.Color.red);
		//pointAttribute.setLabelFont(java.awt.Font.decode("Verdana-Bold-22"));
		pointAttribute.setLabelMaterial(Material.CYAN);
		pmStandard.setLabelText(labor.nombreProperty.get());
		pmStandard.setAttributes(pointAttribute);
		labor.getLayer().addRenderable(pmStandard);
		labor.getLayer().setValue(ZOOM_TO_KEY, pointPosition);


	}

	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
		//progressPane.getChildren().remove(progressBarTask);
	}

	public void start() {
		Thread currentTaskThread = new Thread(this);
		currentTaskThread.setDaemon(true);
		currentTaskThread.start();
	}

	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label(labor.nombreProperty.get());
		progressBarLabel.setTextFill(Color.BLACK);





		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream("/mmg/gui/event-close.png"));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(progressBarLabel,progressBarTask,cancel);
		progressBox.getChildren().add(progressContainer);


	}

	//	protected void canvasRunLater() {		
	//		Platform.runLater(new Runnable() {
	//			@Override
	//			public void run() {
	//				Screen screen = Screen.getPrimary();
	//				Rectangle2D bounds = screen.getVisualBounds();
	//				double size = Math.sqrt(pathTooltips.size());
	//				int lado = 5;
	//				Canvas canvas = new Canvas(bounds.getWidth(),bounds.getHeight());//size*lado+10,size*lado+10);
	//				GraphicsContext gc = canvas.getGraphicsContext2D();		
	//				//6898688,-6898688
	//
	//
	//				BoundingBox bounds2 = getBounds();
	//
	//				System.out.println("bounds2 "+bounds2);
	//
	//				int i = 0;
	//				for (ArrayList<Object> pathTooltip : pathTooltips) {
	//					updateProgress(i, pathTooltips.size());
	//					i++;
	//					Object obj =pathTooltip.get(0);
	//					if (obj instanceof Path ) {
	//						Path path = (Path) obj;
	//
	//						gc.setFill(path.getFill());
	//						gc.setStroke(path.getStroke());
	//
	//						ObservableList<PathElement> elements = path.getElements();
	//
	//						double[] xCoords = new double[elements.size()-1];
	//						double[] yCoords = new double[elements.size()-1];
	//
	//
	//						for (int j =0; j<elements.size()-1;j++ ) {
	//							PathElement pe = elements.get(j);
	//
	//							if (pe instanceof MoveTo ) {
	//								MoveTo mt = (MoveTo) pe;
	//
	//								xCoords[j]=mt.getX()-bounds2.getMinX()/ProyectionConstants.metersToLongLat;//+7074000;
	//								yCoords[j]=mt.getY()-bounds2.getMinY()/ProyectionConstants.metersToLongLat;//3967057;
	//
	//							} else if (pe instanceof LineTo ) {
	//								LineTo lt = (LineTo) pe;
	//
	//								xCoords[j]=lt.getX()-bounds2.getMinX()/ProyectionConstants.metersToLongLat;//7074000;
	//								yCoords[j]=lt.getY()-bounds2.getMinY()/ProyectionConstants.metersToLongLat;//3967057;
	//							}
	//						}
	//						System.out.println("fillPolygon "+Arrays.toString(xCoords) + " "+ Arrays.toString(yCoords) );
	//
	//						gc.fillPolygon(xCoords,yCoords,xCoords.length);
	//					}//no se que hacer si no es un Path
	//
	//				}//fin del for
	//				//double size = Math.sqrt(pathTooltips.size());
	//
	//
	//				//	map.getChildren().add(new Label("HolaGroup"));
	//				//group.getChildren().add(canvas);
	//			}
	//
	//
	//		});
	//	}
	//	public BoundingBox getBounds() {
	//		BoundingBox bounds2=null;
	//		try {
	//			bounds2 = store.getFeatureSource().getBounds();
	//
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		return bounds2;
	//	}


}
