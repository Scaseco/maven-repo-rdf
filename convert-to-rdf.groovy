#!/usr/bin/env groovy
/*
 * list-central-artifacts.groovy
 *
 * Usage:
 *   groovy list-central-artifacts.groovy \
 *         nexus-maven-repository-index.gz   > artifacts.txt
 *
 * Requires only Groovy (≥ 3.x) and an internet connection the first time
 * to download the dependencies declared with @Grab.
 */

@Grab('org.apache.maven.indexer:indexer-reader:7.1.6')
@Grab('org.apache.jena:jena-arq:5.4.0')
import org.apache.maven.index.reader.ChunkReader
import java.nio.file.*
import java.util.zip.GZIPInputStream

import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.system.StreamRDFWriter
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple

import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.DCTerms

import java.lang.Long
import java.time.Instant
import org.apache.jena.sparql.util.NodeFactoryExtra
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.GregorianCalendar

def NS = "https://w3id.org/aksw/mvn#"

// def G = NodeFactory.createURI(NS + "G")
// def GA = NodeFactory.createURI(NS + "GA")
// def GAV = NodeFactory.createURI(NS + "GAV")
def Artifact = NodeFactory.createURI(NS + "Artifact")

def ga = NodeFactory.createURI(NS + "ga")
def gav = NodeFactory.createURI(NS + "gav")
def art = NodeFactory.createURI(NS + "artifact")


def groupId = NodeFactory.createURI(NS + "groupId")
def artifactId = NodeFactory.createURI(NS + "artifactId")
def version = NodeFactory.createURI(NS + "version")
def type = NodeFactory.createURI(NS + "type")
def classifier = NodeFactory.createURI(NS + "classifier")

def hasSubGroup = NodeFactory.createURI(NS + "hasSubGroup")

// g hasArtifact g:a
def hasArtifact = NodeFactory.createURI(NS + "hasArtifact")

// g:a hasVersion g:a:v
def hasVersion = NodeFactory.createURI(NS + "hasVersion")

// g:a:v hasMember
def hasMember = NodeFactory.createURI(NS + "hasMember")


// 1) pick the file: first CLI arg or default name in current dir
def gzFileName = args ? args[0] : 'nexus-maven-repository-index.gz'
def gzFile = Path.of(gzFileName)

def writer = StreamRDFWriter.getWriterStream(System.out, RDFFormat.NTRIPLES)
writer.start()
new ChunkReader('central', Files.newInputStream(gzFile)).withCloseable { records ->
  records.each { rec ->
      // g = groupId, a = artifactId  (field names defined by the index format)

    def u = rec.get('u')
    if (u != null) {
      def parts = u.split("\\|") as List
      def naIndex = parts.indexOf("NA");
      if (naIndex == parts.size() - 1) {
        parts.remove(naIndex)
      }
      def l = parts.size()

      def mg = l >= 1 ? parts.get(0) : null
      def ma = l >= 2 ? parts.get(1) : null
      def mv = l >= 3 ? parts.get(2) : null
      def mc = l == 5 ? parts.get(3) : null
      def mt = l == 5 ? parts.get(4) : l == 4 ? parts.get(3) : null

      def rg = null
      if (mg != null) {
        def subGs = mg.split("\\.") as List
        def sgl = subGs.size()
        if (sgl > 1) {
          def current = NodeFactory.createURI("urn:mvn:" + subGs.get(0))
          for (def i = 1; i < sgl; ++i) {
            def contrib = subGs.get(i)
            def base = current.getURI()
            def nextx = NodeFactory.createURI(base + "." + contrib)
            writer.triple(Triple.create(current, hasSubGroup, nextx))
            current = nextx
          }
        }

        rg = NodeFactory.createURI("urn:mvn:" + mg)
        // writer.triple(Triple.create(rg, RDF.Node.type, G))
      }

      def ra = null
      if (ma != null) {
        rga = NodeFactory.createURI("urn:mvn:" + mg + ":" + ma)
        writer.triple(Triple.create(rg, ga, rga))
      }

      def rgav = null
      if (mv != null) {
        rgav = NodeFactory.createURI("urn:mvn:" + mg + ":" + ma + ":" + mv)
        writer.triple(Triple.create(rga, gav, rgav))
      }

      // def ga = NodeFactory.createURI("urn:mvn:" + g + ":" + a)

      def desc = rec.get('d')

      def artifact = parts.join(":")

      def rs = NodeFactory.createURI("urn:mvn:" + artifact)
      // writer.triple(Triple.create(rs, RDF.Nodes.type, MavenArtifact))
      if (l > 3) {
        writer.triple(Triple.create(rgav, art, rs))
      }

      if (desc != null) {
        writer.triple(Triple.create(rs, RDFS.Nodes.label, NodeFactory.createLiteralString(desc)))
      }

      def im = rec.get('m')
      if (im != null) {
        def epochMilli = Long.parseLong(im)
        def instant = Instant.ofEpochMilli(epochMilli)
        def calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
        def o = NodeFactoryExtra.dateTimeToNode(calendar)
        writer.triple(Triple.create(rs, DCTerms.modified.asNode(), o))
      }

      // println artifact
      // println "${g}:${rec.get('a')}"
  }

//        print rec
} }

writer.finish()

