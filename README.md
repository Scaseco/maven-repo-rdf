# nexus-maven-repository-index-to-rdf
RDF Conversion of the Nexus Maven Repository index file to be found at https://repo.maven.apache.org/maven2/.index/

Download the full index and run the groovy script. Uses Apache Jena.
Remove duplicates with `sort -u` (the groovy script may be improved with local caching to produce fewer duplicates).
Note: First start may hang without output because groovy downloads the dependencies.

```
chmod +x convert-to-rdf.groovy
```

```
wget wget https://repo.maven.apache.org/maven2/.index/nexus-maven-repository-index.gz
./convert-to-rdf.groovy > data.nt
sort -u data.nt > data.sorted.nt
```
