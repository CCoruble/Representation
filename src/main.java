import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import javax.annotation.Resource;
import java.beans.Statement;
import java.io.InputStream;

/**
 * Created by Clovis on 23/05/2018.
 */
public class main
{
	public static void main(String [] args)
	{
		// Création du modèle
		Model model = ModelFactory.createDefaultModel();
	}

	public static void test(){
		Utils.print("Debut test");

		Utils.print("Fin test");
	}
}
