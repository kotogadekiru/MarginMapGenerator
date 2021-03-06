package gui.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.persistence.EntityTransaction;
import javax.persistence.Transient;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableFilter.Builder;
import org.opengis.feature.simple.SimpleFeature;

import dao.Labor;
import dao.LaborItem;
import dao.Poligono;
import dao.OrdenDeCompra.Producto;
import dao.config.Agroquimico;
import dao.config.Campania;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.utils.JPAStringProperty;
import gui.JFXMain;
import gui.Messages;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.DAH;
import utils.ExcelHelper;



public class SmartTableView<T> extends TableView<T> {
	private Supplier<T> onDoubleClick=null;
	private Consumer<T> onShowClick=null;
	
	private Consumer<List<T>> eliminarAction = list->DAH.removeAll((List<Object>) list);;
	private Map<MenuItem,Consumer<T>> consumerMap=new HashMap<>();
	private List<String> rejectedColumns=new ArrayList<>();
	private List<String> orderColumns=new ArrayList<>();
	private boolean permiteEliminar=true;
//	public HBox filters = new HBox();
//	private FilteredList<T> filteredData=null;
	//i18n FilterTable Strings
	//"filterpanel.search.field"
	//"filterpanel.apply.button"
	//"filterpanel.none.button"
	//"filterpanel.all.button"
	//"filterpanel.resetall.button"
	public SmartTableView(ObservableList<T> data,List<String> rejectedColumns,List<String> order){
		super(data);
		this.rejectedColumns=rejectedColumns;
		this.orderColumns=order;
		construct(data);
		
	}
	
	public SmartTableView(ObservableList<T> data){//,ObservableList<T> observable){
		super(data);
		construct(data);
		

	}
	
	public void toExcel() {
		Platform.runLater(()->{
			//TODO implementar exportar a excell 
			ExcelHelper xHelper = new ExcelHelper();
			Map<String, Object[]> data=new TreeMap<String,Object[]>();//HashMap no mantiene el orden
			List<Object> itemData = new ArrayList<Object>();
							
			
			Integer row = new Integer(1);//excel empieza a contar desde 1 duh!
			//headers
			for(TableColumn<?, ?> col:this.getColumns()) {		
				Object cellData = col.getText();
				itemData.add(cellData);
			}
			data.put("0",itemData.toArray());
			//row++;
			itemData.clear();
			
			for(T item :  this.getItems()) {			
				
				for(TableColumn col:this.getColumns()) {		
					Object cellData = col.getCellData(item);
					itemData.add(cellData);
				}
				data.put(row.toString(),itemData.toArray());
				row++;
				itemData.clear();
			}

			xHelper.exportData(null, data);
			});
	}

	private void construct(ObservableList<T> data) {
		impl.org.controlsfx.i18n.Localization.setLocale(Locale.forLanguageTag("es-ES"));//XXX en java 10 falla; pasar al controlsfx-9.0.0.jar
	
		//filteredData = new FilteredList<>(observable, p -> true);
		// 3. Wrap the FilteredList in a SortedList. 
//		SortedList<T> sortedData = new SortedList<>(data);
//
//		// 4. Bind the SortedList comparator to the TableView comparator.
//		sortedData.comparatorProperty().bind(this.comparatorProperty());
//		// 5. Add sorted (and filtered) data to the table.
//		this.setItems(sortedData);

		if(data.size()>0){
			populateColumns(data.get(0).getClass());
		}else{
			//populateColumns(onDoubleClick.get().getClass());
			System.out.println("no creo las columnas porque no hay datos");
		}

	//	Map<String,Consumer<T>> consumerMap = new HashMap<String,Consumer<T>>();
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem mostrarItem = new MenuItem(Messages.getString("SmartTableView.Cargar"));//"Cargar"
		MenuItem eliminarItem = new MenuItem(Messages.getString("SmartTableView.Eliminar"));//Eliminar
		
		//Map<MenuItem,Consumer<T>> mIMap = new HashMap<MenuItem,Consumer<T>>();
		


		this.setContextMenu(contextMenu);

		this.setOnMouseClicked( event->{
			contextMenu.getItems().clear();
			List<T> rowData = this.getSelectionModel().getSelectedItems();
			
			if(rowData != null && rowData.size()>0 ){
				if(onShowClick!=null) contextMenu.getItems().add(mostrarItem);
				if(permiteEliminar)   contextMenu.getItems().add(eliminarItem);
				

				if ( MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
					if(onDoubleClick!=null){
						data.add(onDoubleClick.get());
					}		            
				} 
				else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 1){
					
					consumerMap.keySet().stream().forEach(mi->{
						contextMenu.getItems().add(mi);
						mi.setOnAction((ev)->{
							Platform.runLater(()->	rowData.forEach(consumerMap.get(mi)));
						});
					});
						

					mostrarItem.setOnAction((ev)->{
						Platform.runLater(()->	rowData.forEach(onShowClick));
						//onShowClick.accept(rowData);
					});
					
					eliminarItem.setOnAction((aev)->{						
						Alert alert = new Alert(AlertType.CONFIRMATION);
						Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

						stage.getIcons().add(new Image(JFXMain.ICON));
						alert.setTitle(Messages.getString("SmartTableView.BorrarRegistro"));//"Borrar registro"
						alert.setHeaderText(Messages.getString("SmartTableView.BorrarRegistroWarning"));//"Esta accion borrara permanentemente el registro. Desea Continuar?");
						Optional<ButtonType> res = alert.showAndWait();
						if(res.get().equals(ButtonType.OK) && rowData!=null){
							try{
								//EntityTransaction transaction = DAH.em().getTransaction();
								this.eliminarAction.accept((List<T>) rowData);
								//DAH.removeAll((List<Object>) rowData);
							//	rowData.forEach(each->{
//								DAH.remove(each);
//							//	data.remove(each);
//								});
								data.removeAll(rowData);
								if(data.size()==0){
									data.add(onDoubleClick.get());
								}
								refresh();
							}catch(Exception e){
								Alert eliminarFailAlert = new Alert(AlertType.ERROR);
								((Stage) eliminarFailAlert.getDialogPane().getScene().getWindow()).
										getIcons().add(new Image(JFXMain.ICON));
								eliminarFailAlert.setTitle(Messages.getString("SmartTableView.BorrarRegistro"));//"Borrar registro");
								eliminarFailAlert.setHeaderText(Messages.getString("SmartTableView.BorrarRegistroError"));//"No se pudo borrar el registro");
								eliminarFailAlert.setContentText(e.getMessage());
								eliminarFailAlert.show();
							}							
						}
					});			
				}
			}
		});

		data.addListener((javafx.collections.ListChangeListener.Change<? extends T> c)->{
			if(	getColumns().size()==0){			
			populateColumns(c.getList().get(0).getClass());
		}});

		//		new ListChangeListener<T>(){
		//
		//			@Override
		//			public void onChanged(javafx.collections.ListChangeListener.Change<? extends T> c) {
		//			if(	getColumns().size()==0){
		//				populateColumns(c.getList().get(0).getClass());
		//			}
		//				
		//			}
		//			
		//		});
		
		Builder<T> builder = TableFilter.forTableView(this);
		TableFilter<T> tableFilter = builder.lazy(true).apply();
		tableFilter.setSearchStrategy((input,target) -> {
		    try {
		        return target.toLowerCase().startsWith(input.toLowerCase());
		    } catch (Exception e) {
		    	e.printStackTrace();
		        return false;
		    }
		});
	}

	private String getMethodName(Method method) {
		String name = method.getName();
	
		name = name.replace("get", "");
		name = name.replace("set", "");
		name = name.replace("is", "");
		
		return name;
		
	}

	private void populateColumns(Class<?> clazz) {
		Method[] methods = clazz.getMethods();//ok esto me trae todos los metodos heredados
		List<Method> methodList = Arrays.asList(methods);
		
		methodList.sort((a,b)->{
			
			String nameA = getMethodName(a);//.getName();
			String nameB =  getMethodName(b);//b.getName();
			if(orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
				return Integer.compare(orderColumns.indexOf(nameA),orderColumns.indexOf(nameB));
			} else 	if(orderColumns.contains(nameA)&&!orderColumns.contains(nameB)) {
				return -1;
			}  else if(!orderColumns.contains(nameA)&&orderColumns.contains(nameB)) {
				return 1;
			} 
			return nameA.compareToIgnoreCase(nameB);
		});
	//	System.out.print("creando tabla para "+ clazz+" con los metodos\n"+methodList);
	//	Class<?> superclass =clazz.getSuperclass();
	//	Method[] superMethods = superclass.getDeclaredMethods();

	//	Method[] result = Arrays.copyOf(methods, methods.length + superMethods.length);
	//	System.arraycopy(superMethods, 0, result, methods.length, superMethods.length);


		for (Method method :  methodList) {
			int mods = method.getModifiers();
			boolean transiente = method.isAnnotationPresent(Transient.class);
			if(Modifier.isStatic(mods) || Modifier.isAbstract(mods)||transiente){
				continue;
			}
			String name = method.getName();
			if(name.startsWith("get")||name.startsWith("is")){
				Class<?> fieldType = method.getReturnType();
				String setMethodName = null;
				if(name.startsWith("is")){
					setMethodName = name.replace("is", "set");
				} else {
					setMethodName = name.replace("get", "set");
				}
				
				name = name.replace("get", "");
				if(this.rejectedColumns.contains(name)) {continue;}
				if(String.class.isAssignableFrom(fieldType)){
					getStringColumn(clazz,method, name, fieldType, setMethodName);
				} else 	if(StringProperty.class.isAssignableFrom(fieldType)  ){
					getJPAStringPropertyColumn(clazz, method, name, fieldType, setMethodName);				
				} else 	if(double.class.isAssignableFrom(fieldType) ||Number.class.isAssignableFrom(fieldType) ){
					getNumberColumn(clazz, method, name, fieldType, setMethodName);				
				} else 	if(DoubleProperty.class.isAssignableFrom(fieldType) ){
					getDoublePropertyColumn(clazz, method, name, fieldType, setMethodName);				
				}else if(boolean.class.isAssignableFrom(fieldType) ||Boolean.class.isAssignableFrom(fieldType) ){
					getBooleanColumn(clazz, method, name, fieldType, setMethodName);				
				}else if(Calendar.class.isAssignableFrom(fieldType)){
					getCalendarColumn(clazz, method, name, fieldType, setMethodName);
				}else if(LocalDate.class.isAssignableFrom(fieldType)){
					getLocalDateColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Empresa.class.isAssignableFrom(fieldType)){					
					getEmpresaColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Establecimiento.class.isAssignableFrom(fieldType)){
					getEstablecimientoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Lote.class.isAssignableFrom(fieldType)){
					getLoteColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Campania.class.isAssignableFrom(fieldType)){
					getCampaniaColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Cultivo.class.isAssignableFrom(fieldType)){
					getCultivoColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Cultivo.class.isAssignableFrom(fieldType)){
					getCultivoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Agroquimico.class.isAssignableFrom(fieldType)){
					getAgroquimicoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Fertilizante.class.isAssignableFrom(fieldType)){
					getFertilizanteColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Poligono.class.isAssignableFrom(fieldType)){
					getPoligonoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Producto.class.isAssignableFrom(fieldType)){
					getProductoColumn(clazz, method, name, fieldType, setMethodName);
				}
//				else {//no quiero que muestre los metodos class ni id
//					getStringColumn(clazz,method, name, fieldType, setMethodName);
//				}


			}//fin del if method name starts with get

		}//fin del method list
		this.getColumns().stream().forEach(c->{
			javafx.scene.layout.StackPane graphic = (StackPane) c.getGraphic();
			double maxWidth =0;
			if(graphic==null)return;
			for(Node child:graphic.getChildren()) {
				if(child instanceof Label) {
				javafx.scene.control.Label l = (Label) child;
				l.setWrapText(false);
				maxWidth=Math.max(maxWidth, l.getWidth());
				}
			//System.out.println(child.getClass().getName());//javafx.scene.Parent$2
			}
			//c.setPrefWidth(maxWidth+20);
		});
		//this.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

	}







	private void getCampaniaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Campania> dColumn = new ChoiceTableColumn<T,Campania>(propName,DAH.getAllCampanias(),
				(p)->{try {
					return ((Campania) method.invoke(p, (Object[])null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getLoteColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Lote> dColumn = new ChoiceTableColumn<T,Lote>(propName,DAH.getAllLotes(),
				(p)->{try {
					return ((Lote) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}

	private void getCultivoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Cultivo> dColumn = new ChoiceTableColumn<T,Cultivo>(propName,DAH.getAllCultivos(),
				(p)->{try {
					return ((Cultivo) method.invoke(p, (Object[])null));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}

	private void getFertilizanteColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Fertilizante> dColumn = new ChoiceTableColumn<T,Fertilizante>(propName,DAH.getAllFertilizantes(),
				(p)->{try {
					return ((Fertilizante) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}
	
	
	private void getPoligonoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Poligono> dColumn = new ChoiceTableColumn<T,Poligono>(propName,DAH.getAllPoligonos(),
				(p)->{try {
					return ((Poligono) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}


	
	private void getAgroquimicoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Agroquimico> dColumn = new ChoiceTableColumn<T,Agroquimico>(propName,DAH.getAllAgroquimicos(),
				(p)->{try {
					return ((Agroquimico) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);

	}

	private void getEstablecimientoColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Establecimiento> dColumn = new ChoiceTableColumn<T,Establecimiento>(propName,DAH.getAllEstablecimientos(),
				(p)->{try {
					return ((Establecimiento) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getEmpresaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Empresa> dColumn = new ChoiceTableColumn<T,Empresa>(propName,DAH.getAllEmpresas(),
				(p)->{try {
					return ((Empresa) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getMethod(setMethodName, fieldType);
						setMethod.invoke(p,d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}


	private void getCalendarColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		DateTableColumn<T> dColumn = new DateTableColumn<T>(propName,
				(p)->{try {
					return ((Calendar) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}

	private void getLocalDateColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		LocalDateTableColumn<T> dColumn = new LocalDateTableColumn<T>(propName,
				(p)->{try {
					return ((LocalDate) method.invoke(p, (Object[])null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	

	private void getStringColumn(Class<?> clazz,Method getMethod, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
					 stringValue =(String)  getMethod.invoke(cellData.getValue(), (Object[])null);
					
					
					return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);
						
						return new SimpleStringProperty("sin datos");
					}
				});
		try {
			Method setMethod = clazz.getMethod(setMethodName, fieldType);
			if(setMethod!=null){
				column.setOnEditCommit(cellEditingEvent -> { 
					int row = cellEditingEvent.getTablePosition().getRow();
					T p = cellEditingEvent.getTableView().getItems().get(row);
					try {
						setMethod.invoke(p,cellEditingEvent.getNewValue());
						DAH.save(p);
						refresh();
					} catch (Exception e) {	e.printStackTrace();}
				});
			}

		} catch (NoSuchMethodException e1) {
			//XXX el metodo es solo de tipo get
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}

		this.getColumns().add(column);

	}

	
	private void getProductoColumn(Class<?> clazz,Method getMethod, String name, Class<?> fieldType, String setMethodName) {
		//System.out.println("Obteniendo stringColumn para "+name);
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(false);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(//new PropertyValueFactory<>(propName)
				cellData ->{
					String stringValue = null;
					try{
					 stringValue =((Producto)  getMethod.invoke(cellData.getValue(), (Object[])null)).getNombre();
					
					
					return new SimpleStringProperty(stringValue);	
					}catch(Exception e){
						//System.out.println("La creacion de SimpleStringProperty en getStringColumn "+name +" con valor: "+stringValue);
						
						return new SimpleStringProperty("sin datos");
					}
				});
		try {
			Method setMethod = clazz.getMethod(setMethodName, fieldType);
			if(setMethod!=null){
				column.setOnEditCommit(cellEditingEvent -> { 
					int row = cellEditingEvent.getTablePosition().getRow();
					T p = cellEditingEvent.getTableView().getItems().get(row);
					try {
						((Producto)  getMethod.invoke(p, (Object[])null)).setNombre(cellEditingEvent.getNewValue());
						//((OrdenCompraItem)p).getProducto()
						//setMethod.invoke(p,cellEditingEvent.getNewValue());
						DAH.save(p);
						refresh();
					} catch (Exception e) {	e.printStackTrace();}
				});
			}

		} catch (NoSuchMethodException e1) {
			//XXX el metodo es solo de tipo get
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}

		this.getColumns().add(column);

	}
	
	
	private void getJPAStringPropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		//System.out.print("construyendo un JPASTringPropertyColumn para "+name);
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(
				cellData ->{
					try {
						Object o =  method.invoke(cellData.getValue(), (Object[]) null);
						if(o==null){
							o=new JPAStringProperty();
							try {
								Method setMethod = clazz.getMethod(setMethodName, fieldType);
								if(setMethod!=null){							
									T p = cellData.getValue();
									try {
										setMethod.invoke(p,o);
									} catch (Exception e) {	e.printStackTrace();}
								}
							} catch (NoSuchMethodException | SecurityException e1) {
								e1.printStackTrace();
							}
						}					
						if(StringProperty.class.isAssignableFrom(o.getClass())) {
							StringProperty ssp = (StringProperty)o;
							ssp.addListener((obj,old,n)->{
								DAH.save(cellData.getValue());
							});
							return ssp;
						}
					
						return null;	
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
						e1.printStackTrace();
						return new SimpleStringProperty();
					}
				});
		this.getColumns().add(column);
	}

	private void getDoubleColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					return ((Double) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	private void getNumberColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					Number n = ((Number) method.invoke(p, (Object[])null));
					if(n!=null) {
						return n.doubleValue();
					} else {
						return 0.0;
					}
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}

	private void getDoublePropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					DoubleProperty n = ((DoubleProperty) method.invoke(p, (Object[])null));
					if(n!=null) {
						return n.get();
					} else {
						return 0.0;
					}
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					
					DoubleProperty n = ((DoubleProperty) method.invoke(p, (Object[])null));
					if(n!=null) {
						n.set(d);
					} 
				//	Method setMethod = clazz.getMethod(setMethodName, fieldType);
				//	setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	//No se usa. definir los getters y setters es mas facil creo.
	private void getEstablecimientoPropertyColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,	String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Establecimiento> dColumn = new ChoiceTableColumn<T,Establecimiento>(propName,DAH.getAllEstablecimientos(),
				(p)->{try {
					Property<Establecimiento>prop = ((Property<Establecimiento>) method.invoke(p, (Object[])null));
					return prop.getValue();
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Property<Establecimiento>prop = ((Property<Establecimiento>) method.invoke(p, (Object[])null));
						prop.setValue(d);
						DAH.save(p);
						refresh();
					} catch (Exception e) {

						e.printStackTrace();
					}
				}
				);
		this.getColumns().add(dColumn);
	}
	
	private void getBooleanColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		
		
		try {
			clazz.getMethod(setMethodName, fieldType);	//check method exists
		} catch (Exception e) {	
			//e.printStackTrace();
			return;
		}
		
		
		BooleanTableColumn<T> dColumn = new BooleanTableColumn<T>(propName,
				(p)->{	try {
					return ((Boolean) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}
	
	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnShowClick(Consumer<T> onShowClick) {
		this.onShowClick = onShowClick;
	}
	
	public void addSecondaryClickConsumer(String localizedName, Consumer<T> consumer) {
		this.consumerMap.put(new MenuItem(localizedName), consumer);
	}

	public void setPermiteEliminar(boolean b) {
		this.permiteEliminar=b;
	}
	/**
	 * @return the onDoubleClick
	 */
	public Supplier<T> getOnDoubleClick() {
		return onDoubleClick;
	}


	/**
	 * @param onDoubleClick the onDoubleClick to set
	 */
	public void setOnDoubleClick(Supplier<T> onDoubleClick) {
		this.onDoubleClick = onDoubleClick;
	}
	

	/**
	 * @param eliminarAction Consumer que se ocupa de eliminar el objeto deseado. 
	 */
	public void setEliminarAction(Consumer<List<T>> eliminarAction) {
		this.eliminarAction = eliminarAction;
	}

	public void refresh() { 
		//Wierd JavaFX bug 
		ObservableList<T> data = this.getItems();
		this.setItems(null); 
		this.layout(); 
		this.setItems(data); 
	}

	public static void showLaborTable(Labor<?> labor) {
		Platform.runLater(()->{

			ArrayList<LaborItem> liLista = new ArrayList<LaborItem>();
			System.out.println("Comenzando a cargar la los datos de la tabla"); //$NON-NLS-1$
			Iterator<?> it = labor.outCollection.iterator();
			while(it.hasNext()){
				LaborItem lI = labor.constructFeatureContainerStandar((SimpleFeature)it.next(), false);
				liLista.add(lI);
			}

			final ObservableList<LaborItem> dataLotes =	FXCollections.observableArrayList(liLista);

			SmartTableView<LaborItem> table = new SmartTableView<LaborItem>(dataLotes);
			table.setEditable(false);
			//Button toExcel = new Button("To Excel");
			Button exportButton = new Button(Messages.getString("CosechaHistoChart.16")); //"Exportar"
			exportButton.setOnAction(a->{
				table.toExcel();
			});
			BorderPane bottom = new BorderPane();
			bottom.setRight(exportButton);
			VBox.setVgrow(table, Priority.ALWAYS);
			VBox vBox = new VBox(table,bottom);
			Scene scene = new Scene(vBox, 800, 600);
			Stage tablaStage = new Stage();
			tablaStage.getIcons().add(new Image(JFXMain.ICON));
			tablaStage.setTitle(labor.getNombre());
			tablaStage.setScene(scene);
			tablaStage.show();	 
		});
		
	}


}
