# Rules Service

```
@prefix bank: <http://miniswp.tech/demo/bank#>.

[r1: (bank:customer-001 bank:hasTransaction ?o) -> drop(0)]
[r2: (bank:customer-001 bank:phone ?o) -> drop(0)]
[r3: (bank:customer-001 bank:hasAccount ?o) -> drop(0)]

```

Triple for to protect Predicate
```
TRIPLET:  bank:L1 bank:hasProtected bank:hasAccount
```

Query to create RULE, need to define rules name
```

PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX bank: <http://miniswp.tech/demo/bank#>

SELECT * WHERE { bank:L1 bank:hasProtected ?o .

    BIND(CONCAT("[r1: ( ?s <", STR(?o), "> ?o ) -> drop(0) ]") as ?rule)
}

```