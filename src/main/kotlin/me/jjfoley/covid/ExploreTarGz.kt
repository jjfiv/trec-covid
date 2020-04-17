package me.jjfoley.covid

import edu.umass.cics.ciir.irene.galago.getStr
import edu.umass.cics.ciir.irene.utils.CountingDebouncer
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.lemurproject.galago.utility.Parameters
import org.lemurproject.galago.utility.StreamCreator
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream

val TAR_FILES = listOf(
        "noncomm_use_subset.tar.gz",
        "biorxiv_medrxiv.tar.gz",
        "comm_use_subset.tar.gz",
        "custom_license.tar.gz"
)
val TOTAL_TAR_ENTRIES = 59311

/**
 * For when you want to read ''amt'' bytes from an InputStream, no matter how many syscalls it takes.
 * @param is the input stream to read from.
 * @param amt the number of bytes to read.
 * @return a byte array filled with the next amt bytes from the input stream.
 * @throws EOFException if done
 * @throws IOException if the stream complains
 */
@Throws(IOException::class)
fun readBytes(`is`: InputStream, amt: Int): ByteArray {
    var amt = amt
    if (amt == 0) {
        return ByteArray(0)
    }
    val buf = ByteArray(amt)

    // Begin I/O loop:
    var off = 0
    while (true) {
        assert(off + amt <= buf.size)
        val read = `is`.read(buf, off, amt)
        if (read < -1) {
            throw EOFException()
        }
        if (read == amt) break

        // Ugh; try again
        off += read
        amt -= read
    }
    return buf
}

// TODO: citations
data class PaperEntry(
        val id: String,
        val paragraphs: List<Pair<String, String>>
)

fun loadArticles(progress: Boolean=true, explore: Boolean=false, onArticle: (PaperEntry)->Unit) {
    val msg = CountingDebouncer(total= TOTAL_TAR_ENTRIES.toLong())
    for (tarFileName in TAR_FILES) {
        val tarFile = File(DATA_PATH, tarFileName)
        assert(tarFile.exists())
        val tarStream = TarArchiveInputStream(StreamCreator.openInputStream(tarFile))
        while(true) {
            val entry = tarStream.nextEntry ?: break
            if (entry.isDirectory) continue
            val entryIntSize = entry.size.toInt()
            val buffer = readBytes(tarStream, entryIntSize)
            val data = Parameters.parseBytes(buffer)
            val paper_id = data.getStr("paper_id")
            if (explore) {
                println(data.keys)
            }
            val paragraphs = ArrayList<Pair<String, String>>()
            for (part in listOf("abstract", "body_text", "back_matter")) {
                for (text_span in data.getAsList(part, Parameters::class.java)) {
                    paragraphs.add(Pair(text_span.get("section", "NA"), text_span.getString("text") ?: continue))
                }
            }
            if (paragraphs.size > 0) {
                onArticle(PaperEntry(paper_id, paragraphs))
            }
            if (progress) {
                msg.incr()?.let {
                    println("loadArticles $it")
                }
            }
        }
    }
}

/**
 *
 * @author jfoley.
 */
fun main() {
    loadArticles { (paper_id, paragraphs) ->
        println("paper_id: ${paper_id}")
        println("Found ${paragraphs.size} paragraphs!")
    }
}