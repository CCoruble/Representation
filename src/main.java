import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Clovis on 23/05/2018.
 */

public class main
{
	static final String baseFileFolder = "./baseFile";
	static final String baseURI = "http://www.example.com/base#";

	public static void main (String args[])
	{
		// Create an empty model
		Model model = ModelFactory.createDefaultModel();

		//Resource hercule = model.getResource(herculeURI);
		//hercule.addProperty(VCARD.NICKNAME,"Heracles");

		//Utils.print(model.getProperty("rdf:type").getURI());
		//Utils.print(model.getProperty("http://www.example.com/properties/son_of").getLocalName());

		while(true)
		{
			Utils.printSpacer("#");
			Utils.print("1: Charger un fichier dans le modèle");
			Utils.print("2: Sauvegarder le modèle");
			Utils.print("3: Faire une requête sur le modèle");
			Utils.print("4: Afficher le modèle complet");
			Utils.print("5: Ajouter Triple");
            Utils.print("6: Rechercher les sous-classes");
            Utils.print("7: Rechercher les ressources d'une classe et de ses sous-classes");
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
				case 5:
					addTriple(model);
					break;
                case 6:
                    HashSet<String> listeDesSubclasses = new HashSet<String>();
                    Utils.print("nom de la classe ?");
                    String className = InputFunction.getStringInput();
                    searchClassAmongSubclasses(model,className,listeDesSubclasses);
                    for (String subclasseName:listeDesSubclasses)
                    {
                        Utils.print(subclasseName);
                    }
                    break;
                case 7:
                    HashSet<String> uriList = new HashSet<String>();
                    Utils.print("nom de la classe ?");
                    String classNameToURI = InputFunction.getStringInput();
                    searchURIAmongSubclasses(model,classNameToURI,uriList);
                    for (String subclassName:uriList)
                    {
                        Utils.print(subclassName);
                    }
                    break;
                case 12:
                Set<String> coco =  RDFUtils.getPropertyValuesForResourceTransitive(model, model.getResource("http://www.example.com/base#Hercule") ,RDFS.seeAlso, true);
                for (String coke:coco)
                {
                    Utils.print(coke);
                }
                coco = RDFUtils.getPropertyValuesForUris(model,RDFS.comment,coco,true);
                for (String coke:coco)
                {
                    Utils.print(coke);
                }
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

    public static void searchURIAmongSubclasses(Model model, String mainClassName, HashSet<String> uriList){
        HashSet<String> subclassesList = new HashSet<String>();
	    searchClassAmongSubclasses(model, mainClassName,subclassesList);

	    subclassesList.add(mainClassName);

        for (String subclassName:subclassesList)
        {

            //pour chaque sous classe du sujet et de l'objet, effectuer la requète
            QueryExecution qe = null;
            String typeRdf = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#";
            String sparql = "SELECT distinct ?ressource WHERE {?ressource  a " + "<http://www.example.com/classes/"+subclassName + "> }";
            //Utils.print("debug: " + sparql);

            try
            {
                Query qry = QueryFactory.create(sparql);
                qe = QueryExecutionFactory.create(qry, model);
                ResultSet rs = qe.execSelect();

                while(rs.hasNext() ) {
                    QuerySolution qs = rs.next();
                    Resource subclass = qs.getResource("ressource").asResource();
                    //ajout de l'URI dans la liste
                    if(!uriList.contains(subclass.getLocalName()))
                        uriList.add(subclass.getLocalName());
                }

            } catch (Exception e){
                Utils.print("Erreur dans la requête !");
            } finally {
                qe.close();
            }
        }
    }

	public static void searchClassAmongSubclasses(Model model, String mainClassName, HashSet<String> subclassesList)
    {
        //pour chaque sous classe du sujet et de l'objet, effectuer la requète
        QueryExecution qe = null;
        //String sparql = "SELECT distinct ?subclass WHERE {?subclass <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://www.example.com/classes/"+mainClassName + "> }";
        String sparql = RDFUtils.QUERY_PREFIXES + "SELECT distinct ?subclass WHERE {?subclass rdfs:subClassOf classes:"+mainClassName + " }";
        Utils.print("debug: " + sparql);

        try
        {
            Query qry = QueryFactory.create(sparql);
            qe = QueryExecutionFactory.create(qry, model);
            ResultSet rs = qe.execSelect();

            while(rs.hasNext() ) {
                QuerySolution qs = rs.next();
                Resource subclass = qs.getResource("subclass").asResource();
                Utils.print("debug subclass.localname() " + subclass.getLocalName());
                subclassesList.add(subclass.getLocalName());
                searchClassAmongSubclasses(model, subclass.getLocalName(),subclassesList);
            }

        } catch (Exception e){
            Utils.print("Erreur dans la requête !");
        } finally {
            qe.close();
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
		sparql = RDFUtils.QUERY_PREFIXES + InputFunction.getStringInput();


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

        Utils.print("Fonction Bapt");
        RDFUtils.executeAndDisplayUserQuery(model,sparql,true);

    }

	public static void addTriple(Model model){
		Utils.print("Indiquez l'URI complète de la ressource à ajouter.");
		Utils.print("pour un élément de notre base : base 'http://www.example.com/base#' + Item  ");
		Utils.print("pour une classe : base 'http://www.example.com/classes/' + ClassName  ");
		Utils.print("pour une propriété : base 'http://www.example.com/properties/' + PropertyName ");
		String uri = InputFunction.getStringInput();
		//Utils.print("debug: " + uri);
		Resource resource = null;
		Resource objet = null;

		if(!model.contains(model.getResource(uri), null ))
		{
			Utils.print("Creation de la ressource");
			resource = model.createResource(uri);
		}
		else
		{
			Utils.print("ressource trouvée dans le modèle local");
			resource = model.getResource(uri);
		}

		int choice;
		Utils.print("1: Ajouter une propriété");
		Utils.print("autre : Quitter");
		choice = InputFunction.getIntInput();
		while(choice == 1){
			Property prop = null;
			Utils.print("");
			Utils.print("Entrez l'URI de votre propriété");
			uri = InputFunction.getStringInput();
			//Utils.print("debug: " + uri);

			if(!model.contains(null,model.getProperty(uri) ))
			{
				Utils.print("Creation de la propriété");
				prop = model.createProperty(uri);
			}
			else
			{
				Utils.print("proposition trouvée dans le modèle local");
				prop = model.getProperty(uri);
			}

			Utils.print("");
			Utils.print("Entrez l'URI l'objet du triplet");
			uri = InputFunction.getStringInput();
			//Utils.print("debug: " + uri);

			if(!model.contains(model.getResource(uri), null ))
			{
				Utils.print("Creation de la ressource");
				objet = model.createResource(uri);
			}
			else
			{
				Utils.print("objet trouvé dans le modèle local");
				objet = model.getResource(uri);
			}


			resource.addProperty(prop,objet);

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
