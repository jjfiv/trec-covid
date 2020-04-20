from routes.search_results import QUERIES, get_index
from irene.server import QueryResponse
from irene.lang import *
from irene.models import QueryLikelihood, BM25
import gzip
from tqdm import tqdm

index = get_index()

orig_weight = 0.3
fb_docs = 20
fb_terms = 100


def write_trecrun(qid: str, sys_name: str, results: QueryResponse, out):
    for (i, doc) in enumerate(results.topdocs):
        rank = i + 1
        print(
            "{0} Q0 {1} {2} {3} {4}".format(qid, doc.name, rank, doc.score, sys_name),
            file=out,
        )


with gzip.open("smith.rm3.trecrun.gz", "wt") as out_rm3:
    with gzip.open("smith.ql.trecrun.gz", "wt") as out_ql:
        with gzip.open("smith.bm25.trecrun.gz", "wt") as out_bm25:
            for q in tqdm(QUERIES):
                words = index.tokenize(q.query)
                print(q.qid, words)
                expr = RM3Expr(QueryLikelihood(words), orig_weight, fb_docs, fb_terms)
                results = index.query(expr, depth=1000)
                write_trecrun(q.qid, "smith.rm3", results, out_rm3)

                expr = BM25(words)
                results = index.query(expr, depth=1000)
                write_trecrun(q.qid, "smith.bm25", results, out_bm25)

                expr = QueryLikelihood(words)
                results = index.query(expr, depth=1000)
                write_trecrun(q.qid, "smith.ql", results, out_ql)
