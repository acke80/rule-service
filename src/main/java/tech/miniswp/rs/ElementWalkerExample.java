package tech.miniswp.rs;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.syntax.*;

import java.util.Arrays;
import java.util.ListIterator;

public class ElementWalkerExample {
    public static void main(String[] args) {
        final String queryString  = "" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX champ: <http://sdp.collaborationlayer-traton.com/champ#>\n" +
                "                 \n" +
                "SELECT DISTINCT ?crr\n" +
                " WHERE {\n" +
                "?part champ:PartId \"1888075\" .\n" +
                "?part1 champ:PartId \"81.27120-0059\" .\n" +
                "?crr champ:hasPart ?part .\n" +
                "?crr champ:hasPart ?part1 .\n" +
                "} ";
        final Query query = QueryFactory.create( queryString );
        System.out.println( "== before ==\n"+query );
        System.out.println("Graph URIs: " + query.getDatasetDescription());
        System.out.println("Result Vars: " + query.getResultVars());
        System.out.println("GROUP BY: " + query.getGroupBy());
        System.out.println("HAVING: " + query.getHavingExprs());
        System.out.println("LIMIT: " + query.getLimit());
        Element myelem = query.getQueryPattern();
        if (myelem instanceof ElementGroup)
        {
            Element e = ((ElementGroup)myelem).getElements().get(0);
            if(e instanceof ElementPathBlock) {
                System.out.println("Path Block:");
                ListIterator<TriplePath> it = ((ElementPathBlock) e).getPattern().iterator();
                while ( it.hasNext() ) {
                    final TriplePath tp = it.next();
                    System.out.println(tp);
                }
            }
            else if(e instanceof ElementTriplesBlock) {
                System.out.println("Triples Block:" +  ((ElementTriplesBlock) e).getPattern().get(0).getPredicate());
            }
        }

        ElementWalker.walk( query.getQueryPattern(),
                new ElementVisitorBase() {
                    @Override
                    public void visit(ElementPathBlock el) {
                        ListIterator<TriplePath> it = el.getPattern().iterator();
                        while ( it.hasNext() ) {
                            final TriplePath tp = it.next();
                            final Var d = Var.alloc( "s1" );
                            if ( tp.getSubject().equals( d )) {
                                it.add( new TriplePath( new Triple( d, d, d )));
                            }
                        }
                    }
                });
        System.out.println( "== after ==\n"+query );

        // ?s ?p ?o .
        Triple pattern =
                Triple.create(Var.alloc("s"), Var.alloc("p"), Var.alloc("o"));
        // ( ?s < 20 )
        Expr e = new E_LessThan(new ExprVar("s"), new NodeValueInteger(20));

        ElementTriplesBlock block = new ElementTriplesBlock(); // Make a BGP
        block.addTriple(pattern);                              // Add our pattern match
        ElementFilter filter = new ElementFilter(e);           // Make a filter matching the expression
        ElementGroup body = new ElementGroup();                // Group our pattern match and filter
        body.addElement(block);
        body.addElement(filter);

        Query q = QueryFactory.make();
        q.setQueryPattern(body);                               // Set the body of the query to our group
        q.setQuerySelectType();                                // Make it a select query
        q.addResultVar("s");                                   // Select ?s

        System.out.println("QUERY:");
        System.out.println(q);

        Op op;
        BasicPattern pat = new BasicPattern();                 // Make a pattern
        pat.add(pattern);                                      // Add our pattern match
        op = new OpBGP(pat);                                   // Make a BGP from this pattern
        op = OpFilter.filter(e, op);                           // Filter that pattern with our expression
        op = new OpProject(op, Arrays.asList(Var.alloc("s"))); // Reduce to just ?s
        Query q1 = OpAsQuery.asQuery(op);                       // Convert to a query
        q.setQuerySelectType();                                // Make is a select query

        System.out.println("QUERY ALGEBRA:");
        System.out.println(q1);
    }
}