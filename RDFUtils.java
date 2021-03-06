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

/**
 * @author Baptiste Michel
 */
public class RDFUtils {
    private final static Logger logger = Logger.getLogger(RDFUtils.class);

    public final static String QUERY_PREFIXES = String.join("\n",
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>",
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

    private final static String HTML_TEMPLATE = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<title>Query Results</title>\n" +
            "<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.1.1/css/bootstrap.min.css\" integrity=\"sha384-WskhaSGFgHYWDcbwN70/dfYBj47jz9qbsMId/iRN3ewGhXQFZCSftd1LZCfmhktB\" crossorigin=\"anonymous\">\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "%CONTENT%" +
            "</body>\n" +
            "\n" +
            "</html>";

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

        List<String> head;
        List<Map<String, String>> body = new ArrayList<>();
        List<QuerySolution> solutions = new ArrayList<>();
        try {
            ResultSet results = qexec.execSelect();
            head = results.getResultVars();
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                solutions.add(solution);
                Iterator<String> it = solution.varNames();
                Map<String, String> row = new HashMap<>();
                while (it.hasNext()) {
                    String s = it.next();
                    row.put(s, solution.get(s).toString());
                }
                body.add(row);
            }
        } finally {
            qexec.close();
        }

        logger.info("Génération du contenu HTML...");

        String content = "<table class=\"table\"><thead><tr><th>#</th>";
        for (String s : head) {
            content += "<th>";
            content += StringEscapeUtils.escapeHtml4(s);
            content += "</th>";
        }
        content += "</tr></thead><tbody>";
        int cnt = 1;
        for (Map<String, String> line : body) {
            content += "<tr>";
            content += "<td>" + cnt + "</td>";
            cnt++;
            for (String s : head) {
                String val = line.get(s);
                if (val == null)
                    val = "-";
                content += "<td>";
                if (val.startsWith("http://")) {
                    content += "<a href=\"";
                    content += val;
                    content += "\">";
                    content += StringEscapeUtils.escapeHtml4(val);
                    content += "</a>";
                } else
                    content += StringEscapeUtils.escapeHtml4(val);
                content += "</td>";
            }
            content += "</tr>";
        }
        content += "</tbody></table>";
        try (PrintWriter out = new PrintWriter("query.html")) {
            out.println(HTML_TEMPLATE.replace("%CONTENT%", content));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Desktop.getDesktop().open(new File("query.html"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("HTML généré avec succès.");

        return solutions;
    }
}
