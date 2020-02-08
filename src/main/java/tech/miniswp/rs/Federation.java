package tech.miniswp.rs;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class Federation {

    public static void main(String[] args)  {
        Repository repository = FedXFactory.newFederation()
                .withSparqlEndpoint("http://admin:admin@localhost:5820/bank/query")
                //.withSparqlEndpoint("https://query.wikidata.org/sparql")
                .withSparqlEndpoint("http://localhost:7200/repositories/bank")
                .create();

        try (RepositoryConnection conn = repository.getConnection()) {

            /*
            String query =
                    "PREFIX wd: <http://www.wikidata.org/entity/> "
                            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
                            + "SELECT * WHERE { "
                            + " ?country a <http://dbpedia.org/class/yago/WikicatMemberStatesOfTheEuropeanUnion> ."
                            + " ?country <http://www.w3.org/2002/07/owl#sameAs> ?countrySameAs . "
                            + " ?countrySameAs wdt:P2131 ?gdp ."
                            + "}";
             */

            String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX bank: <http://miniswp.tech/demo/bank#>\n" +
                    "\n" +
                    "SELECT ?o2 ?o1\n" +
                    "WHERE {\n" +
                    "    ?s bank:phone \"01010101\" .\n" +
                    "    ?s bank:appliedForLoan ?o .\n" +
                    "    ?o bank:fromIP ?o1 .\n" +
                    "    ?o2 bank:fromIP ?o1\n" +
                    "} \n";

            TupleQuery tq = conn.prepareTupleQuery(query);
            try (TupleQueryResult tqRes = tq.evaluate()) {

                int count = 0;
                while (tqRes.hasNext()) {
                    BindingSet b = tqRes.next();
                    System.out.println(b);
                    count++;
                }

                System.out.println("Results: " + count);
            }
        }

        repository.shutDown();
    }

}
