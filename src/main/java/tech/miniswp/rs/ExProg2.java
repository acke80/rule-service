package tech.miniswp.rs;

// The ARQ application API.

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.DC;

/** Example : Build a query with a filter programmatically.
 *  Note: it is often better to build and execute an algebra expression.  See other examples. */

public class ExProg2
{
    static public final String NL = System.getProperty("line.separator") ;

    public static void main(String[] args)
    {
        Model model = createModel() ;

        Query query = QueryFactory.make() ;
        query.setQuerySelectType() ;

        // See also ExProg1

        ElementGroup elg = new ElementGroup() ;

        Var varTitle = Var.alloc("title") ;
        Var varX = Var.alloc("x") ;

        Triple t1 = new Triple(varX, DC.title.asNode(),  varTitle) ;
        elg.addTriplePattern(t1) ;

        // Adds a filter.  Need to wrap variable in a NodeVar.
        Expr expr = new E_Regex(new ExprVar(varTitle), "jena", "i") ;
        ElementFilter filter = new  ElementFilter(expr) ;
        elg.addElementFilter(filter) ;

        // Attach the group to query.
        query.setQueryPattern(elg) ;

        // Choose what we want - SELECT ?title
        query.addResultVar(varTitle) ;

        // Print query with line numbers
        // Prefix mapping just helps serialization
        query.getPrefixMapping().setNsPrefix("dc" , DC.getURI()) ;
        query.serialize(new IndentedWriter(System.out,true)) ;
        System.out.println() ;

        try(QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            // Assumption: it's a SELECT query.
            ResultSet rs = qexec.execSelect() ;

            // The order of results is undefined.
            System.out.println("Titles: ") ;
            for ( ; rs.hasNext() ; )
            {
                QuerySolution rb = rs.nextSolution() ;

                // Get title - variable names do not include the '?' (or '$')
                RDFNode x = rb.get("title") ;

                // Check the type of the result value
                if ( x instanceof Literal )
                {
                    Literal titleStr = (Literal)x  ;
                    System.out.println("    "+titleStr) ;
                }
                else
                    System.out.println("Strange - not a literal: "+x) ;

            }
        }
    }

    public static Model createModel()
    {
        Model model = ModelFactory.createDefaultModel() ;

        Resource r1 = model.createResource("http://example.org/book#1") ;
        Resource r2 = model.createResource("http://example.org/book#2") ;
        Resource r3 = model.createResource("http://example.org/book#3") ;

        r1.addProperty(DC.title, "SPARQL - the book")
                .addProperty(DC.description, "A book about SPARQL") ;

        r2.addProperty(DC.title, "Advanced techniques for SPARQL") ;

        r3.addProperty(DC.title, "Jena - an RDF framework for Java")
                .addProperty(DC.description, "A book about Jena") ;

        return model ;
    }
}