# TREC-COVID

- [TREC-COVID](https://ir.nist.gov/covidSubmit/index.html) is an information retrieval challenge running Spring 2020 that involves ~50,000 research articles.
- This repository contains code for submissions by team Smith.

## Dependencies:

- Lucene via [irene](https://github.com/jjfiv/irene)

## Running: 

1. Edit code to define variable DATA_PATH to point to a directory where you have downloaded the [CORD-19 dataset](https://pages.semanticscholar.org/coronavirus-research).
2. Run the ``MergeInputFilesKt`` class.
3. Run the ``IndexFromJSONL`` class.
