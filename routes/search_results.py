from routes import app
import flask
from irene import *
from irene.lang import *
import attr
import os
from bs4 import BeautifulSoup
from functools import lru_cache
import time


@attr.s
class TrecQuery(object):
    """qid, query, question, narrative"""

    qid = attr.ib()
    query = attr.ib()
    question = attr.ib()
    narrative = attr.ib()


DATA_PATH = os.getenv("DATA_PATH")
assert DATA_PATH is not None, "env var DATA_PATH should be set."


def _load_queries():
    queries = []
    with open(os.path.join(DATA_PATH, "topics-rnd1.xml")) as fp:
        xml = BeautifulSoup(fp.read(), "lxml")
        for topic in xml.find_all("topic"):
            qid = topic["number"]
            query = topic.query.string
            question = topic.question.string
            narrative = topic.narrative.string
            queries.append(TrecQuery(qid, query, question, narrative))
    return queries


QUERIES = _load_queries()
QUERY_DICT = dict((q.qid, q) for q in QUERIES)


def get_index() -> IreneIndex:
    service = IreneService(port=4444)
    return service.open("covid", "{}/covid.irene".format(DATA_PATH))


@app.route("/")
def home_page():
    return flask.render_template("index.j2", queries=QUERIES)


@attr.s
class DocData(object):
    name = attr.ib()
    title = attr.ib()
    abstract = attr.ib()
    authors = attr.ib()
    journal = attr.ib()
    doi = attr.ib()
    body = attr.ib()


@lru_cache(maxsize=10000)
def lookup_document(name: str) -> DocData:
    raw = get_index().doc(name)
    return DocData(
        raw["id"],
        raw["title"],
        raw["abstract"],
        raw["authors"],
        raw["journal"],
        raw["doi"],
        raw["body"],
    )


@app.route("/search/<qid>")
def search_results(qid: str):
    if qid not in QUERY_DICT:
        return flask.redirect("/")
    query = QUERY_DICT[qid]
    index = get_index()
    start = time.time()
    terms = index.tokenize(query.query)
    results = index.query(
        query=MeanExpr([DirQLExpr(TextExpr(t)) for t in terms]), depth=20
    )
    documents = {}
    for docr in results.topdocs:
        documents[docr.name] = lookup_document(docr.name)
    end = time.time()
    return flask.render_template(
        "search_results.j2",
        results=results,
        documents=documents,
        query=query,
        queries=QUERIES,
        timing="{:.3} seconds".format(end - start),
    )


@app.route("/doc/<name>")
def document_page(name: str):
    return lookup_document(name).body.replace("\n", "<br /><br />")
