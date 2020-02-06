package tech.miniswp.rs;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.PrintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static org.apache.jena.vocabulary.ReasonerVocabulary.PROPderivationLogging;
import static org.apache.jena.vocabulary.ReasonerVocabulary.PROPtraceOn;

@Component
@EnableScheduling
public class RulesServiceEng {
    private final Logger log = LoggerFactory.getLogger(RulesServiceEng.class);
    Model gmod;
    ClassPathResource fileRULES;

    @Value("${rules.sparql.1}")
    private String sparql1;

    private String query1;

    @Value("${jena.rules}")
    private String rulesFile;

    @Value("${rdf.storage.url}")
    private String dbURL;

    @Value("${rdf.storage.dataset}")
    private String dbDataset;

    @PostConstruct
    void init(){
        log.info("Initializing Rules Data Set and Model.......");
        gmod = ModelFactory.createDefaultModel();
        fileRULES = new ClassPathResource(rulesFile);


        if( sparql1 != null) {
            try {
                InputStream isFile = new ClassPathResource(sparql1).getInputStream();
                Scanner scanner = new Scanner(isFile, StandardCharsets.UTF_8.name());
                query1 = scanner.useDelimiter("\\A").next();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            query1 = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
        }
        log.info("Rules QUERY1: " + query1);

        try {

            System.out.println(fileRULES.getURL().toURI().toString());
            List<Rule> rules = Rule.rulesFromURL(fileRULES.getURL().toURI().toString());
            //schema = FileManager.get().loadModel(fileRULES.getURL().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addData(Model mod){
        reInit();
        gmod.add(mod);
        StringWriter out = new StringWriter();
        gmod.write(out, "N-TRIPLES");
        log.info(out.toString());
    }

    public void reInit(){
        log.info("Re-initializing Rules Data Set and Model.......");
        gmod = ModelFactory.createDefaultModel();
    }

    public String execRules(String qry, String rulesMsg) {

        String rdfxml = constructGRAPHDB();
        List<Rule> rules = Rule.parseRules(Rule.rulesParserFromReader(new BufferedReader(new StringReader(rulesMsg))));

        Reasoner reasoner = new GenericRuleReasoner(rules);

        reasoner.setDerivationLogging(Boolean.TRUE);
        reasoner.setParameter(PROPtraceOn, Boolean.TRUE);
        reasoner.setParameter(PROPderivationLogging, Boolean.TRUE);

        Model data = null;
        try {
            data = ModelFactory.createDefaultModel()
                    .read(IOUtils.toInputStream(rdfxml, "UTF-8"), null, "RDF/XML");
        } catch (IOException e) {
            e.printStackTrace();
        }

        InfModel infmodel = ModelFactory.createInfModel(reasoner, data);
        System.out.println(infmodel.validate().isValid());
        Boolean res = !(infmodel.getDeductionsModel().isEmpty());

        /*
        StmtIterator i = infmodel.listStatements();

        while (i.hasNext()) {
            System.out.println(" - " + PrintUtil.print(i.nextStatement()));
        }
        */

        Query query = QueryFactory.create("SELECT ?o WHERE {?s <http://sdp.collaborationlayer-traton.com/champ#isValid> ?o}");
        QueryExecution queryExec = QueryExecutionFactory.create(query, infmodel);
        ResultSet rs = queryExec.execSelect();

        String qres = "";
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            System.out.println(qs);
            qres = qres + qs.toString();

        }
        queryExec.close();

        return(res.toString());
    }

    public String constructGRAPHDB(){

        int sc = 0;
        String rdfxml = null;
        try {
            String query = URLEncoder.encode("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}","utf-8");
            String dbH = dbURL + "repositories/" + dbDataset;

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(dbH);

            httpPost.addHeader("Content-type", "application/sparql-query");
            httpPost.addHeader("Accept", "application/rdf+xml");
            httpPost.addHeader("User-Agent", "MiniSWP/1.0");
            httpPost.addHeader("Cache-Control", "no-cache");
            httpPost.addHeader("Accept-Encoding", "gzip, deflate");
            httpPost.addHeader("Connection", "keep-alive");

            StringEntity entity = new StringEntity("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}");
            httpPost.setEntity(entity);

            CloseableHttpResponse response = client.execute(httpPost);
            rdfxml = EntityUtils.toString(response.getEntity());
            //System.out.println(rdfxml);
            sc = response.getStatusLine().getStatusCode();
            System.out.println("RESPONSE STATUS CODE: " + sc);
            client.close();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rdfxml;

    }


    public static void main(String[] args) throws IOException, URISyntaxException {

        ClassPathResource fileOWL = new ClassPathResource("CHAMP-V5.rdf");
        System.out.println(fileOWL.getURL());
        String champURI = "http://sdp.collaborationlayer-traton.com/champ#";

        // Register PREFIX(s)
        PrintUtil.registerPrefix("champ", champURI);

        //String rules = "[r1: (?crr champ:hasPart ?p1), (?crr champ:hasPart ?p2) -> (?p1 owl:sameAs ?p2)]";
        ClassPathResource fileRules = new ClassPathResource("my-rules.rules");
        System.out.println(fileRules.getURL().toURI().toString());
        List<Rule> rules = Rule.rulesFromURL(fileRules.getURL().toURI().toString());


        //Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        Reasoner reasoner = new GenericRuleReasoner(rules);

        Model data = FileManager.get().loadModel(fileOWL.getURL().toString());

        InfModel infmodel = ModelFactory.createInfModel(reasoner, data);
        System.out.println(infmodel.validate().isValid());

        //System.out.println("INFERRED MODEL: " + infmodel.toString());

        // Query for all things related to "a" by "p"
        Property p = data.getProperty("http://www.w3.org/2002/07/owl#", "sameAs");
        Resource a = data.getResource(champURI + "part-mantb-08.71000-1205");
        //StmtIterator i = infmodel.listStatements(a, p, (RDFNode)null);
        //StmtIterator i = infmodel.listStatements();

        //while (i.hasNext()) {
        //    System.out.println(" - " + PrintUtil.print(i.nextStatement()));
        //}

        Query query = QueryFactory.create("SELECT ?o WHERE {?s <http://sdp.collaborationlayer-traton.com/champ#isValid> ?o}");
        QueryExecution queryExec = QueryExecutionFactory.create(query, infmodel);
        ResultSet rs = queryExec.execSelect();

        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            System.out.println(qs);

        }
        queryExec.close();

    }
}
