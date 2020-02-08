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
                .withSparqlEndpoint("http://dbpedia.org/sparql")
                .withSparqlEndpoint("https://query.wikidata.org/sparql")
                .create();

        try (RepositoryConnection conn = repository.getConnection()) {

            String query =
                    "PREFIX wd: <http://www.wikidata.org/entity/> "
                            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
                            + "SELECT * WHERE { "
                            + " ?country a <http://dbpedia.org/class/yago/WikicatMemberStatesOfTheEuropeanUnion> ."
                            + " ?country <http://www.w3.org/2002/07/owl#sameAs> ?countrySameAs . "
                            + " ?countrySameAs wdt:P2131 ?gdp ."
                            + "}";

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
