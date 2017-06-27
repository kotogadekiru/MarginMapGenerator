package gui.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import dao.config.Agroquimico;
import dao.config.Campania;
import dao.config.Cultivo;
import dao.config.Empresa;
import dao.config.Establecimiento;
import dao.config.Fertilizante;
import dao.config.Lote;
import dao.cosecha.CosechaItem;
import gui.JFXMain;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import utils.DAH;


public class SmartTableView<T> extends TableView<T> {
	private Supplier<T> onDoubleClick=null;

	public SmartTableView(ObservableList<T> data,ObservableList<T> filtered){
		super(data);

		// 3. Wrap the FilteredList in a SortedList. 
		SortedList<T> sortedData = new SortedList<>(filtered);

		// 4. Bind the SortedList comparator to the TableView comparator.
		sortedData.comparatorProperty().bind(this.comparatorProperty());
		// 5. Add sorted (and filtered) data to the table.
		this.setItems(sortedData);

		if(data.size()>0){
			populateColumns(data.get(0).getClass());
		}else{
			System.out.println("no creo las columnas porque no hay datos");
		}

		this.setOnMouseClicked( event->{
			if ( MouseButton.PRIMARY.equals(event.getButton()) && event.getClickCount() == 2) {
				if(onDoubleClick!=null){
					data.add(onDoubleClick.get());
				}		            
			} else if(MouseButton.SECONDARY.equals(event.getButton()) && event.getClickCount() == 2){
				T rowData = this.getSelectionModel().getSelectedItem();
			
				Alert alert = new Alert(AlertType.CONFIRMATION);
				Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
				
				stage.getIcons().add(new Image(JFXMain.ICON));
				alert.setTitle("Borrar registro");
				alert.setHeaderText("Esta accion borrara permanentemente el registro. Desea Continuar?");
				Optional<ButtonType> res = alert.showAndWait();
				if(res.get().equals(ButtonType.OK) && rowData!=null){
					data.remove(rowData);

					DAH.remove(rowData);
				}
			}

		});

		data.addListener((javafx.collections.ListChangeListener.Change<? extends T> c)->{if(	getColumns().size()==0){
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

	}


	private void populateColumns(Class<?> clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		Class<?> superclass =clazz.getSuperclass();
		Method[] superMethods = superclass.getDeclaredMethods();

		Method[] result = Arrays.copyOf(methods, methods.length + superMethods.length);
		System.arraycopy(superMethods, 0, result, methods.length, superMethods.length);


		for (Method method :  result) {
			int mods = method.getModifiers();
			if(Modifier.isStatic(mods) || Modifier.isAbstract(mods)){
				continue;
			}
			String name = method.getName();
			if(name.startsWith("get")){
				Class<?> fieldType = method.getReturnType();
				String setMethodName = name.replace("get", "set");
				name = name.replace("get", "");
				if(String.class.isAssignableFrom(fieldType)){
					getStringColumn(clazz, name, fieldType, setMethodName);
				} else if(double.class.isAssignableFrom(fieldType) ||Double.class.isAssignableFrom(fieldType) ){
					getDoubleColumn(clazz, method, name, fieldType, setMethodName);				
				} else if(Calendar.class.isAssignableFrom(fieldType)){
					getCalendarColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Empresa.class.isAssignableFrom(fieldType)){					
					getEmpresaColumn(clazz, method, name, fieldType, setMethodName);
				}else 	if(Establecimiento.class.isAssignableFrom(fieldType)){
					getEstablecimientoColumn(clazz, method, name, fieldType, setMethodName);
				}else 	if(Lote.class.isAssignableFrom(fieldType)){
					getLoteColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Campania.class.isAssignableFrom(fieldType)){
					getCampaniaColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Cultivo.class.isAssignableFrom(fieldType)){
					getCultivoColumn(clazz, method, name, fieldType, setMethodName);
				} else if(Agroquimico.class.isAssignableFrom(fieldType)){
					getAgroquimicoColumn(clazz, method, name, fieldType, setMethodName);
				}else if(Fertilizante.class.isAssignableFrom(fieldType)){
					getFertilizanteColumn(clazz, method, name, fieldType, setMethodName);
				}
				 

			}//fin del if method name starts with get

		}

	}








	private void getCampaniaColumn(Class<?> clazz, Method method, String name, Class<?> fieldType,
			String setMethodName) {
		String propName = name.replace("Property", "");
		ChoiceTableColumn<T, Campania> dColumn = new ChoiceTableColumn<T,Campania>(propName,DAH.getAllCampanias(),
				(p)->{try {
					return ((Campania) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Lote) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Cultivo) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Fertilizante) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Agroquimico) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Establecimiento) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Empresa) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;},
				(p,d)->{
					try {
						Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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
					return ((Calendar) method.invoke(p, null));
				} catch (Exception e) {

					e.printStackTrace();
				}
				return null;
				},(p,d)->{try {
					Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
	}


	private void getStringColumn(Class<?> clazz, String name, Class<?> fieldType, String setMethodName) {
		//TODO obtener el nombre de la columna de un bundle de idiomas o de un archivo de configuracion
		String propName = name.replace("Property", "");
		TableColumn<T,String> column = new TableColumn<T,String>(propName);
		column.setEditable(true);
		column.setCellFactory(TextFieldTableCell.forTableColumn());
		column.setCellValueFactory(new PropertyValueFactory<>(propName));
		try {
			Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
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


	private void getDoubleColumn(Class<?> clazz, Method method, String name, Class<?> fieldType, String setMethodName) {
		String propName = name.replace("Property", "");
		DoubleTableColumn<T> dColumn = new DoubleTableColumn<T>(propName,
				(p)->{	try {
					return ((Double) method.invoke(p, (Object[])null));
				} catch (Exception e) {	e.printStackTrace();}
				return null;
				},(p,d)->{ try {
					Method setMethod = clazz.getDeclaredMethod(setMethodName, fieldType);
					setMethod.invoke(p,d);
					DAH.save(p);
					refresh();
				} catch (Exception e) {	e.printStackTrace();}
				});

		this.getColumns().add(dColumn);
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

	public void refresh() { 
		//Wierd JavaFX bug 
		ObservableList<T> data = this.getItems();
		this.setItems(null); 
		this.layout(); 
		this.setItems(data); 
	}
}
