package me.jjfoley.covid

import com.opencsv.CSVReader
import edu.umass.cics.ciir.irene.utils.CountingDebouncer
import edu.umass.cics.ciir.irene.utils.smartPrint
import edu.umass.cics.ciir.irene.utils.smartPrinter
import edu.umass.cics.ciir.irene.utils.smartReader
import gnu.trove.map.hash.TObjectIntHashMap
import org.lemurproject.galago.utility.Parameters
import java.io.File
import java.util.*

val DATA_PATH = File("/media/flash/trec-covid/")

data class MetadataEntry(
        val cord_uid: String,
        val sha: String,
        val source_x: String,
        val title: String,
        val doi: String,
        val pmcid: String,
        val pubmed_id: String,
        val license: String,
        val abstract: String,
        val publish_time: String,
        val authors: String,
        val journal: String,
        val microsoftPaperId: String,
        val whoCovidence: String,
        val has_pdf_parse: Boolean,
        val has_pmc_xml_parse: Boolean,
        val full_text_file: String,
        val url: String
) {
    fun getShas(): Set<String> {
        val output = hashSetOf<String>()
        if (sha.contains(";")) {
            output.addAll(sha.split(";").map { it.trim() })
        } else {
            output.add(sha)
        }
        return output.filter { it.isNotBlank() }.toSet()
    }
    fun getIds(): Set<String> {
        val output = hashSetOf<String>()
        if (this.pubmed_id.isNotBlank()) {
            output.add("pubmed$pubmed_id")
        }
        if (this.pmcid.isNotBlank()) {
            output.add(pmcid)
        }
        if (this.sha.isNotBlank()) {
            output.add(sha)
        }
        return output
    }
}

val EXPECTED_HEADERS = "cord_uid|sha|source_x|title|doi|pmcid|pubmed_id|license|abstract|publish_time|authors|journal|Microsoft Academic Paper ID|WHO #Covidence|has_pdf_parse|has_pmc_xml_parse|full_text_file|url".trim().split("|").toList()


fun row_to_metadata(row: Array<String>): MetadataEntry {
    return MetadataEntry(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11], row[12], row[13], (row[14]=="True"), has_pmc_xml_parse=(row[15]=="True"), full_text_file=row[16], url=row[17])
}

fun load_metadata(): List<MetadataEntry> {
    return CSVReader(File(DATA_PATH, "metadata.csv").smartReader()).use { csv ->
        val header = csv.readNext().toList()
        if(header != EXPECTED_HEADERS) {
            throw RuntimeException(header.joinToString(separator="|"));
        }
        val keep = ArrayList<MetadataEntry>()
        while (true) {
            val row = csv.readNext()
            if (row == null) {
                break;
            }
            keep.add(row_to_metadata(row))
        }
        keep
    }
}

/**
 *
 * @author jfoley.
 */
fun main(args: Array<String>) {
    val argp = Parameters.parseArgs(args)
    val metadata = load_metadata().associateBy { it.cord_uid }
    val cord_uid_by_sha = HashMap<String, HashSet<String>>()

    for (m in metadata.values) {
        for (sha in m.getShas()) {
            // full-text can be associated with multiple metadata?
            cord_uid_by_sha.getOrPut(sha, { hashSetOf() }).add(m.cord_uid)
        }
    }
    println("Loaded metadata! ${metadata.size} ${cord_uid_by_sha.size}")
    File("sha-collisions.log").smartPrinter().use { err ->
        for ((sha, uids) in cord_uid_by_sha) {
            if (uids.size > 1) {
                err.println(uids.joinToString(separator=" "))
            }
        }
    }

    val full_texts = HashMap<String, MutableList<PaperEntry>>()

    File("unknown-full-sha.log").smartPrint { unknown ->
        loadArticles { entry ->
            val paper_id = entry.id
            val cord_uids = cord_uid_by_sha[paper_id]
            if (cord_uids == null) {
                unknown.println(paper_id)
            } else {
                for (cord_uid in cord_uids) {
                    full_texts.getOrPut(cord_uid, { arrayListOf() }).add(entry)
                }
            }
        }
    }
    println("full_texts: ${full_texts.size}, metadata: ${metadata.size}")

    val fieldCounts = TObjectIntHashMap<String>()
    File(DATA_PATH, "cord-19.jsonl.gz").smartPrint { out ->

        val msg = CountingDebouncer(total=metadata.size.toLong())
        for ((cord_uid, meta) in metadata) {
            val texts: List<PaperEntry> = full_texts.getOrDefault(cord_uid, emptyList())
            val outP = Parameters.create()
            outP.put("docid", cord_uid)
            outP.put("title", meta.title)
            outP.put("abstract", meta.abstract)
            outP.put("authors", meta.authors)
            outP.put("doi", meta.doi)
            outP.put("journal", meta.journal)
            // list of string:
            val fields = hashMapOf<String, MutableList<String>>()
            for (entry in texts) {
                for ((field, content) in entry.paragraphs) {
                    fields.getOrPut(field, { arrayListOf()}).add(content)
                }
            }
            val fieldTexts = fields.mapValues { (_, paras) -> paras.joinToString(separator="\n") }
            for (field in fieldTexts.keys) {
                fieldCounts.adjustOrPutValue(field, 1, 1)
            }
            outP["fields"] = Parameters.wrap(fieldTexts)
            out.println(outP)
            msg.incr()?.let {
                println("save-to-json $it")
            }
        }
        println(msg.final())
    }
    println(fieldCounts)
}
