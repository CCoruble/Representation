import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
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
	static final String herculeURI = "http://www.example.com/base#Hercule";

	public static void main (String args[])
	{
		// create an empty model
		Model model = ModelFactory.createDefaultModel();

		// use the FileManager to find the input file
		InputStream in = FileManager.get().open(inputFileName);
		if (in == null)
		{
			throw new IllegalArgumentException("File: " + inputFileName + " not found");
		}

		// read the RDF/XML file
		model.read(new InputStreamReader(in), "");

		// retrieve the Adam Smith vcard resource from the model
		Resource hercule = model.getResource(herculeURI);

		// retrieve the value of the N property
		Resource name = (Resource) hercule.getRequiredProperty("name");
				.getObject();
		// retrieve the given name property
		String fullName = hercule.getRequiredProperty(VCARD.FN)
				.getString();
		// add two nick name properties to vcard
		hercule.addProperty(VCARD.NICKNAME, "Smithy")
				.addProperty(VCARD.NICKNAME, "Adman");

		// set up the output
		System.out.println("The nicknames of \"" + fullName + "\" are:");
		// list the nicknames
		StmtIterator iter = hercule.listProperties(VCARD.NICKNAME);
		while (iter.hasNext())
		{
			System.out.println("    " + iter.nextStatement().getObject()
					.toString());
		}
	}
	public static void test(){
		Utils.print("Debut test");

		Utils.print("Fin test");
	}
}
