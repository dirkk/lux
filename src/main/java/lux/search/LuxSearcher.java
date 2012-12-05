package lux.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.Directory;

public class LuxSearcher extends IndexSearcher {

  public LuxSearcher (Directory dir) throws IOException {
    super (dir);
  }
  
  public LuxSearcher (IndexSearcher searcher) {
      super (searcher.getIndexReader());
  }
  
  public LuxSearcher (IndexReader reader) {
      super (reader);
  }

  /**
   * @param query the Lucene query
   * @return the unordered results of the query as a Lucene DocIdSetIterator.  Unordered means the order
   * is not predictable and may change with subsequent calls. 
   * @throws IOException
   */
  public DocIdSetIterator search (Query query) throws IOException {
      return new DocIterator (query, false);
  }
  
  /**
   * @param query the Lucene query
   * @return the results of the query as a Lucene DocIdSetIterator, ordered using the sort criterion. 
   * Results are returned in batches, so deep paging is possible, but expensive.
   * @throws IOException
   */
  public DocIdSetIterator search (Query query, Sort sort) throws IOException {
      return new TopDocsIterator (query, sort);
  }

  /**
   * @param query the Lucene query
   * @return the results of the query as a Lucene DocIdSetIterator in docID order
   * @throws IOException
   */
  public DocIdSetIterator searchOrdered (Query query) throws IOException {
      return new DocIterator (query, true);
  }
  
  class DocIterator extends DocIdSetIterator {
      
      private final Weight weight;
      private final boolean ordered;
      private int nextReader;
      private int docID;
      private int docBase; // add to docID which is relative to each sub-reader
      private Scorer scorer;
      
      /**
       * @param query the lucene query whose results will be iterated
       * @param ordered whether the docs must be scored in order
       * @throws IOException
       */
      DocIterator (Query query, boolean ordered) throws IOException {
          weight = createNormalizedWeight(query);
          this.ordered = ordered;
          nextReader = 0;
          docID = -1;
          advanceScorer();
      }

      private void advanceScorer () throws IOException {
          while (nextReader < subReaders.length) {
              docBase = docStarts[nextReader];
              scorer = weight.scorer(subReaders[nextReader++], ordered, true);
              if (scorer != null) {
                  return;
              }
          }
          scorer = null;
      }
      
    @Override
    public int docID() {
        return docID;
    }

    @Override
    public int nextDoc() throws IOException {
        while (scorer != null) {
            docID = scorer.nextDoc();
            if (docID != NO_MORE_DOCS) {
                return docID + docBase;
            }
            advanceScorer();
        }
        return NO_MORE_DOCS;
    }

    @Override
    public int advance(int target) throws IOException {
        while (scorer != null) {
            docID = scorer.advance(target - docBase);
            if (docID != NO_MORE_DOCS) {
                return docID + docBase;
            }
            advanceScorer();
        }
        return NO_MORE_DOCS;
    }
      
  }
  
  class TopDocsIterator extends DocIdSetIterator {
      
      private final Query query;
      private final Sort sort;
      private int docID = -1;
      private int iDocNext = 0;
      private int iDocBase = 0;
      private TopDocs topDocs;
      private static final int BATCH_SIZE = 200;
      
      TopDocsIterator (Query query, Sort sort) throws IOException {
          this.query = query;
          this.sort = sort;
          topDocs = search(createNormalizedWeight(query), null, BATCH_SIZE, sort, false);
      }

      @Override
      public int docID() {
          return docID;
      }

      @Override
      public int nextDoc() throws IOException {
          if (iDocNext < topDocs.scoreDocs.length) {
              docID = topDocs.scoreDocs[iDocNext++].doc;
          }
          else if (iDocBase + iDocNext < topDocs.totalHits) {
              // load a larger batch of docs
              topDocs = search(createNormalizedWeight(query), null, BATCH_SIZE, sort, false);
          }
          else {
              // exhausted the entire result set
              docID = -1;
          }
          return docID;
      }

    @Override
    public int advance(int target) throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }
    
  }

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
