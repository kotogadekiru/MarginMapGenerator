package dao.OrdenDeCompra;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
//@NamedQueries({
//	@NamedQuery(name=Ndvi.FIND_ALL, query="SELECT c FROM Ndvi c") ,
//	@NamedQuery(name=Ndvi.FIND_NAME, query="SELECT o FROM Ndvi o where o.nombre = :name") ,
//	@NamedQuery(name=Ndvi.FIND_ACTIVOS, query="SELECT o FROM Ndvi o where o.activo = true") ,
//}) 
public class OrdenCompraItem {

	public static final String FIND_ALL="OrdenCompraItem.findAll";
	public static final String FIND_NAME = "OrdenCompraItem.findName";

	@javax.persistence.Id @GeneratedValue
	private Long id=null;
	
	@ManyToOne
	private OrdenCompra ordenCompra =null;
	
	@ManyToOne
	private Producto producto =null;
	
	private Double cantidad=null;
	
	public OrdenCompraItem() {
		
	}
	
	public OrdenCompraItem(Producto producto2, Double cantidad2) {
		this.producto=producto2;
		this.cantidad=cantidad2;
	}

}
