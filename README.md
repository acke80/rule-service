# Rules Service

```
@prefix bank: <http://miniswp.tech/demo/bank#>.

[r1: (bank:customer-001 bank:hasTransaction ?o) -> drop(0)]
[r2: (bank:customer-001 bank:phone ?o) -> drop(0)]
[r3: (bank:customer-001 bank:hasAccount ?o) -> drop(0)]

```