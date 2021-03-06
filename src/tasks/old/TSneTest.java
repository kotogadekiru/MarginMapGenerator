package tasks.old;
import java.io.File;
import java.util.List;

import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.MatrixOps;
import com.jujutsu.utils.MatrixUtils;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

//barnes hut version
//https://github.com/lejon/T-SNE-Java
public class TSneTest extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		 int initial_dims = 55;
		 double perplexity = 10.0;//20.0;//relacionado al tamanio de los grupos buscados
		  File input =   chooseFiles("TXT","*.txt").get(0);
		  //new File("src/main/resources/datasets/mnist2500_X.txt")
		  //mnist2500_X.txt tiene 2500 registros y 783 dimensiones
		  //yo tengo solo 10 o 20 dimensiones pero mas registros
		    double [][] X = MatrixUtils.simpleRead2DMatrix(input, "   ");
		    System.out.println("registros: "+X.length);
		    System.out.println("dimensiones: "+X[0].length);
		   // System.out.println(MatrixOps.doubleArrayToPrintString(X, ", ", 50,10));
		    BarnesHutTSne tsne;
		    boolean parallel = true;
		    if(parallel) {          
		        tsne = new ParallelBHTsne();
		    } else {
		        tsne = new BHTSne();
		    }
		    double [][] Y = tsne.tsne(X, 1, initial_dims, perplexity);   
		    System.out.println("registros: "+Y.length);
		    System.out.println("dimensiones: "+Y[0].length);
		    /*
		     registros: 249
			dimensiones: 2
		     */
		   System.out.println(MatrixOps.doubleArrayToPrintString(Y, ", ", 50,10));
		  
		    // Plot Y or save Y to file and plot with some other tool such as for instance R
		   System.exit(0);
	}
	
	
	private List<File> chooseFiles(String f1,String f2) {
		List<File> files =null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(f1, f2));
		fileChooser.setInitialDirectory(new File("C:\\Users\\quero\\workspaceHackaton2015\\T-SNE-Java-master\\tsne-demos\\src\\main\\resources\\datasets"));
		try{
			files = fileChooser.showOpenMultipleDialog(new Stage());
			//		file = files.get(0);
		}catch(IllegalArgumentException e){
			e.printStackTrace();
		}
		return files;
	}
	
	public static void main(String[] args) {
		Application.launch(TSneTest.class, args);
	}
}

