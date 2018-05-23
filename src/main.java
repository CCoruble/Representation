import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.VCARD;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.apache.jena.vocabulary.RDF.predicate;

/**
 * Created by Clovis on 23/05/2018.
 */
public class main
{
	static final String inputFileName = "travail_1.xml";
	static final String inputFileName2 = "travail_12.xml";
	static final String herculeURI = "http://www.example.com/base#Hercule";

	public static void main (String args[])
	{
		// create an empty model
		Model model = ModelFactory.createDefaultModel();

		// use the FileManager to find the input file
		InputStream in = FileManager.get().open(inputFileName);
		InputStream in2 = FileManager.get().open(inputFileName2);
		if (in == null)
		{
			throw new IllegalArgumentException("File: " + inputFileName + " not found");
		}

		// read the RDF/XML file
		model.read(new InputStreamReader(in), "");
		model.read(new InputStreamReader(in2), "");

		Resource hercule = model.getResource(herculeURI);
		hercule.addProperty(VCARD.NICKNAME,"Heracles");

		Utils.printSpacer("#");
		//<http://www.example.com/base#Hercule>
		String sparql = "SELECT distinct ?a WHERE {?a ?b ?c}";

		Query qry = QueryFactory.create(sparql);
		QueryExecution qe = QueryExecutionFactory.create(qry, model);
		ResultSet rs = qe.execSelect();

		while(rs.hasNext())
		{
			QuerySolution sol = rs.nextSolution();
			RDFNode str = sol.get("?a");
			Utils.print(str.toString());
		}

		qe.close();
		Utils.printSpacer("#");	
	}
	public static void test(){
		Utils.print("Debut test");
		Utils.print("Fin test");
	}
}
