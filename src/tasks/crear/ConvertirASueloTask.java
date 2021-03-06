package tasks.crear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.data.FeatureReader;
import org.h2.expression.Function;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.gson.Gson;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.recorrida.Muestra;
import dao.recorrida.Recorrida;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.nww.LaborLayer;
import javafx.scene.paint.Color;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class ConvertirASueloTask extends ProcessMapTask<SueloItem,Suelo > {
	CosechaLabor cosecha=null;
	Recorrida recorrida=null; 

	public ConvertirASueloTask(CosechaLabor cosecha, Recorrida recorrida) {
		this.cosecha=cosecha;
		this.recorrida=recorrida;
		this.labor=new Suelo();
		labor.setNombre(cosecha.getNombre());
		LaborLayer layer = new LaborLayer();
		labor.setLayer(layer);
	}

	@Override
	protected void doProcess() throws IOException {
		//crear Suelo
	
		
		
		//Map<String, List<Muestra>> muestraNombreMap = recorrida.getMuestras().stream().collect(Collectors.groupingBy(Muestra::getNombre));
		
		//crear un map de muestra segun su nombre
		Map<String, List<Muestra>> mMap = recorrida.getMuestras().stream().collect(Collectors.groupingBy(Muestra::getNombre));
		
		//TODO recorrer los cosechaitem de la cosecha
		List<CosechaItem> cItems = new ArrayList<CosechaItem>();
		FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
			cItems.add(ci);
		}
		reader.close();
		cItems.parallelStream().forEach(ci->{
			Integer categoria = cosecha.getClasificador().getCategoryFor(ci.getAmount());
			//String catName = cosecha.getClasificador().getCategoryNameFor(catIndex);
			
			
			//Integer categoria =cosecha.getClasificador().getCategoryFor(container.getAmount());
			//Color color  = c.getClasificador().getColorForCategoria(categoria);

			//int size = c.getClasificador().getNumClasses();
			String catName = cosecha.getClasificador().getLetraCat(categoria);//""+Clasificador.abc[size-categoria-1];
			
			List<Muestra> muestrasK = mMap.get(catName); 
			if(muestrasK!=null && muestrasK.size()>0) {
				Muestra m = muestrasK.get(0);

				String obs = m.getObservacion();

				@SuppressWarnings("unchecked")
				Map<String,String> map = new Gson().fromJson(obs, Map.class);	 

				LinkedHashMap<String, Number> props = new LinkedHashMap<String,Number>();
				for(String k : map.keySet()) {
					Object value = map.get(k);
					if(String.class.isAssignableFrom(value.getClass())) {				
						Double dValue = new Double(0);
						try { dValue=new Double((String)value);
						}catch(Exception e) {System.err.println("error tratando de parsear \""+value+"\" reemplazo por 0");}
						props.put(k, dValue);//ojo number format exception
					} else if(Number.class.isAssignableFrom(value.getClass())) {
						props.put(k, (Number)value);
					}			
				}

				Number ppmP = props.get(SueloItem.PPM_FOSFORO);
				Number ppmN = props.get(SueloItem.PPM_N);
				Number ppmMO = props.get(SueloItem.PPM_MO);

				SueloItem si = new SueloItem();
				synchronized(labor){
					si.setId(labor.getNextID());
				}
				si.setPpmP(ppmP.doubleValue());
				si.setPpmN(ppmN.doubleValue());
				si.setPorcMO(ppmMO.doubleValue());

				labor.setPropiedadesLabor(si);


				si.setGeometry(ci.getGeometry());

				labor.insertFeature(si);
			}
		});
		//TODO si el nombre de la categoria del cosechaitem coincide con el nombre de la muestra
		// crear un suelo item con la geometria de la cosecha y el dato de la muestra

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, SueloItem si, ExtrudedPolygon renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = CrearSueloMapTask.buildTooltipText(this.labor, si, area);//buildTooltipText(this.labor,si,area);

		return super.getExtrudedPolygonFromGeom(poly, si,tooltipText,renderablePolygon);		
	}

	
}
