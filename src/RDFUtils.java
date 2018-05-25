import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;


public class RDFUtils {
    private final static Logger logger = Logger.getLogger(RDFUtils.class);

    public final static String QUERY_PREFIXES = String.join("\n",
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>",
            "PREFIX classes: <http://www.example.com/classes/>",
            "PREFIX properties: <http://www.example.com/properties/>",
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>",
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>",
            "PREFIX : <http://dbpedia.org/resource/>",
            "PREFIX dbpedia2: <http://dbpedia.org/property/>",
            "PREFIX dbpedia: <http://dbpedia.org/>",
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>",
            "");

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static Resource addPropertyToResource(Model model, Resource res, Property property, String val) {
        Resource valRes = ResourceFactory.createResource(val);
        if (model.containsResource(valRes)) {
            logger.info("URI existante détectée.");
            return res.addProperty(property, valRes);
        } else {
            if (isInteger(val)) {
                logger.info("Litéral 'Integer' détecté.");
                return res.addProperty(property, model.createTypedLiteral(Integer.parseInt(val)));
            } else {
                logger.info("Litéral detecté.");
                return res.addProperty(property, val);
            }
        }
    }

    public static Resource addPropertyToResource(Model model, Resource res, String property, String val) {
        Property prop = model.getProperty(property);
        return addPropertyToResource(model, res, prop, val);
    }

    public static Set<String> getPropertyValuesForUri(Model model, String uri, Property property, boolean isExt) {
        Set<String> uris = new HashSet<>();

        String queryString = String.join("\n",
                QUERY_PREFIXES,
                "SELECT ?uri",
                "WHERE {",
                "<" + uri + "> <" + property.toString() + "> ?uri",
                "}");

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = isExt ? QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query) : QueryExecutionFactory.create(query, model);

        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                uris.add(solution.get("uri").toString());
            }
        } finally {
            qexec.close();
        }

        return uris;
    }

    private static Set<String> getPropertyValuesForUriTransitive(Model model, Set<String> uris, String uri, Property property, boolean isExt) {
        if (!uris.contains(uri)) {
            uris.add(uri);
            Set<String> tempUris = getPropertyValuesForUri(model, uri, property, isExt);
            for (String tempUri : tempUris) {
                if (!uris.contains(tempUri)) {
                    getPropertyValuesForUriTransitive(model, uris, tempUri, property, isExt);
                }
            }
        }
        return uris;
    }

    private static Set<String> getPropertyValuesForUriTransitive(Model model, String uri, Property property, boolean isExt) {
        return getPropertyValuesForUriTransitive(model, new HashSet<>(), uri, property, isExt);
    }

    public static Set<String> getPropertyValuesForResourceTransitive(Model model, Resource res, Property property, boolean isExt) {
        Set<String> uris = new HashSet<>();

        StmtIterator it = res.listProperties(property);
        while (it.hasNext()) {
            Statement st = it.next();
            String uri = st.getObject().toString();
            uris.addAll(getPropertyValuesForUriTransitive(model, uri, property, isExt));
        }

        return uris;
    }

    public static Set<String> getPropertyValuesForUris(Model model, Property property, Set<String> uris, boolean isExt) {
        Set<String> props = new HashSet<>();

        for (String uri : uris) {
            props.addAll(getPropertyValuesForUri(model, uri, property, isExt));
        }
        return props;
    }

    public static List<QuerySolution> executeAndDisplayUserQuery(Model model, String userQuery, boolean isExt) {
        String queryString = String.join("\n",
                RDFUtils.QUERY_PREFIXES,
                userQuery).replace('[', '<').replace(']', '>');
        /*try {
            queryString = new String(queryString.getBytes("UTF-8"), "ISO-8859-1");
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = isExt ? QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query) : QueryExecutionFactory.create(query, model);

        List<String> variables;
        List<Map<String, String>> solutionMap = new ArrayList<>();
        List<QuerySolution> solutions = new ArrayList<>();
        try {
            ResultSet results = qexec.execSelect();
            variables = results.getResultVars();
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                solutions.add(solution);
                Iterator<String> it = solution.varNames();
                Map<String, String> row = new HashMap<>();
                while (it.hasNext()) {
                    String s = it.next();
                    row.put(s, solution.get(s).toString());
                }
                solutionMap.add(row);
            }
        } finally {
            qexec.close();
        }

        logger.info("Génération du contenu HTML...");

        String content = "";
        for (String s : variables) {
            content += s + "\t";
        }
        Utils.print(content);

        content = "";
        int cnt = 1;
        for (Map<String, String> line : solutionMap) {
            content +=  cnt + "\t";
            cnt++;
            for (String s : variables) {
                String val = line.get(s);
                if (val == null)
                    val = "-";

                content += StringEscapeUtils.escapeHtml4(val) + "\t";
            }
            Utils.print(content);
            content = "";
        }

        return solutions;
    }
}
