package me.jjfoley.covid

import org.jsoup.Jsoup
import java.io.File

data class TrecQuery(
        val number: String,
        val query: String,
        val question: String,
        val narrative: String
)

fun loadRound1Topics(): List<TrecQuery> {
    val path = File(DATA_PATH, "topics-rnd1.xml")
    val doc = Jsoup.parse(path, "UTF-8")
    val topics = ArrayList<TrecQuery>()
    for  (topic in doc.select("topics").select("topic")) {
        val q = TrecQuery(
                topic.attr("number"),
                topic.selectFirst("query").text(),
                topic.selectFirst("question").text(),
                topic.selectFirst("narrative").text()
        )
        topics.add(q)
    }
    return topics
}

/**
 *
 * @author jfoley.
 */
fun main() {
    println(loadRound1Topics()[12])
}