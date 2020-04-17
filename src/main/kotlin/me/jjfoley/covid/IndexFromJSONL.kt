package me.jjfoley.covid

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.umass.cics.ciir.irene.IndexParams
import edu.umass.cics.ciir.irene.utils.smartDoLines
import java.io.File

data class Cord19Document(
        val docid: String,
        val title: String,
        val abstract: String,
        val authors: String,
        val doi: String,
        val journal: String,
        val fields: Map<String, String>
)
val COVID_INDEX_PATH = File(DATA_PATH, "covid.irene")
val SPECIAL_FIELDS = setOf("Introduction", "Conclusion")

/**
 *
 * @author jfoley.
 */
fun main() {
    val mapper = ObjectMapper()
    mapper.registerKotlinModule()

    IndexParams().apply {
        create()
        withPath(COVID_INDEX_PATH)
    }.openWriter().use { writer ->
        val DOCS_COUNT = 51045
        File(DATA_PATH, "cord-19.jsonl.gz").smartDoLines(doProgress=true, total=DOCS_COUNT.toLong()) {line ->
            val doc = mapper.readValue(line, Cord19Document::class.java)
            writer.doc {
                setId(doc.docid)
                setTextField("title", doc.title)
                setTextField("abstract", doc.abstract)
                setTextField("authors", doc.authors)
                setTextField("journal", doc.journal)
                setStringField("doi", doc.doi)

                val body = StringBuilder()
                body.append("${doc.title}\n\n")
                body.append("${doc.abstract}\n\n")
                for ((field, text) in doc.fields) {
                    if (SPECIAL_FIELDS.contains(field)) {
                        setTextField(field.toLowerCase(), text)
                    }
                    body.append("${field}\n${text}\n\n")
                }
                setTextField("body", body.toString())
            }
        }
        writer.open()
    }.use { reader ->
        println("Indexed: ${reader.totalDocuments} successfully.")
    }
}