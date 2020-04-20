create table if not exists relevance(
  qid text not NULL,
  user text not NULL,
  docid text not NULL,
  label text not NULL,
  at_time timestamp not NULL
);