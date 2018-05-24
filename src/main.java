import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Clovis on 23/05/2018.
 */
public class main
{
	static final String baseFileFolder = "./baseFile";
	static final String baseURI = "http://www.example.com/base#";
	static final String herculeURI = "http://www.example.com/base#Hercule";

	public static void main (String args[])
	{
		// Create an empty model
		Model model = ModelFactory.createDefaultModel();

		//Resource hercule = model.getResource(herculeURI);
		//hercule.addProperty(VCARD.NICKNAME,"Heracles");

		//Utils.print(model.getProperty("rdf:type").getURI());
		//Utils.print(model.getProperty("http://www.example.com/properties#son_of").getLocalName());

		while(true)
		{
			Utils.printSpacer("#");
			Utils.print("1: Charger un fichier dans le modèle");
			Utils.print("2: Sauvegarder le modèle");
			Utils.print("3: Faire une requête sur le modèle");
			Utils.print("4: Afficher le modèle complet");
			Utils.print("Autre: Quitter");
			Utils.printSpacer("#");
			int choice = InputFunction.getIntInput();

			switch(choice)
			{
				case 1:
					configModel(model);
					break;

				case 2:
					saveModel(model);
					break;

				case 3:
					queryModel(model);
					break;

				case 4:
					printModel(model);
					break;

				default:
					return;
			}
		}
	}

	public static void configModel(Model model){
		Utils.print("1: Charger les fichiers de base");
		Utils.print("2: Charger un fichier spécifique");
		Utils.print("Autre: Quitter");
		int choice = InputFunction.getIntInput();

		switch(choice){
			case 1:
				loadBaseFiles(model);
				break;
			case 2:
				loadSpecificFile(model);
				break;
			default:
				break;
		}
	}

	public static void loadBaseFiles(Model model){
		File folder = new File(baseFileFolder);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles){
			InputStream in = FileManager.get().open(file.getAbsolutePath());
			model.read(new InputStreamReader(in), "");
		}
		Utils.print(listOfFiles.length + " fichier(s) chargé(s)");
	}

	public static void loadSpecificFile(Model model){
		ArrayList<String> formatList = new ArrayList<>();
		formatList.add("RDF/XML");
		formatList.add("N-TRIPLE");
		formatList.add("TURTLE");
		formatList.add("N3");

		// file type
		Utils.print("Format de votre fichier:");
		for(int i = 0; i < formatList.size(); i++){
			Utils.print((i+1) + ": " + formatList.get(i));
		}
		Utils.print("Autre: Quitter");
		int choice = InputFunction.getIntInput();
		if(choice < 0 || choice > formatList.size())
			return;

		// file name
		Utils.print("Nom de votre fichier:");
		String fileName = InputFunction.getStringInput();
		InputStream in = FileManager.get().open(fileName);
		if (in == null) {
			Utils.print( "File: " + fileName + " not found");
			return;
		}
		model.read(new InputStreamReader(in), formatList.get(choice - 1));
		Utils.print("Fichier '" + fileName + "' au format '" + formatList.get(choice - 1) + "' ajouté au modèle");
	}

	public static void saveModel(Model model){
		FileOutputStream output;
		Utils.print("Entrez le nom du fichier de sauvegarde:");
		String fileName = InputFunction.getStringInput();
		try  {
			output = new FileOutputStream(fileName);
			model.write(output, "RDF/XML");
			output.close();
		} catch(Exception e) {
			Utils.print("Impossible de sauvegarder le modèle !");
		}
	}

	public static void printModel(Model model){
		model.write(System.out,"RDF/XML");
	}

	public static void queryModel(Model model){
		//<http://www.example.com/base#Hercule>
		QueryExecution qe = null;
		String sparql;

		Utils.print("Donnez votre requête SPARQL:");
		Utils.print("Example: \"SELECT distinct * WHERE {?sujet ?predicat ?objet}\"");
		sparql = InputFunction.getStringInput();

		try
		{
			Query qry = QueryFactory.create(sparql);
			qe = QueryExecutionFactory.create(qry, model);
			ResultSet rs = qe.execSelect();

			Utils.print(ResultSetFormatter.asText(rs));
		} catch (Exception e){
			Utils.print("Erreur dans la requête !");
		} finally {
			qe.close();
		}
	}

	public static void addResource(Model model){
		Utils.print("Indiquez l'URI de la ressource à ajouter.");
		Utils.print("(Ne pas donnez la base 'http://www.example.com/base#')");
		String uri = InputFunction.getStringInput();

		Resource resource = model.createResource(baseURI + uri);

		int choice;
		Utils.print("1: Ajouter une propriété");
		Utils.print("autre : Quitter");
		choice = InputFunction.getIntInput();
		while(choice == 1){
			Utils.print("");
			//resource.addProperty(,"retzertera");
			Utils.print("1: Ajouter une propriété");
			Utils.print("autre: Quitter");
			choice = InputFunction.getIntInput();
		}
	}

	public static void test(){
		Utils.print("Debut test");
		Utils.print("Fin test");
	}
}
