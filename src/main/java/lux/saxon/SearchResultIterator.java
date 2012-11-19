package lux.saxon;

import java.io.IOException;

import lux.api.QueryStats;
import lux.search.LuxSearcher;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

public class SearchResultIterator implements SequenceIterator<NodeInfo> {
    
    private final DocIdSetIterator docIter;
    private final Query query;
    private final QueryStats stats;
    private final LuxSearcher searcher;
    private final Saxon saxon;
    private CachingDocReader docCache;
    private NodeInfo current = null;
    private int position = 0;
    
    public SearchResultIterator (Saxon saxon, Query query) throws IOException {
        this.query = query;
        this.saxon = saxon;
        this.stats = saxon.getQueryStats();
        if (stats != null) {
            stats.query = query.toString();
        }
        searcher = saxon.getSearcher();
        docCache = saxon.getDocReader();
        docIter = searcher.searchOrdered(query);
    }

    public NodeInfo next() throws XPathException {
        long t = System.nanoTime();
        int startPosition = position;
        try {
            int docID = docIter.nextDoc();
            // LoggerFactory.getLogger(ResultIterator.class).trace("GET {} {}", docID, query);
            if (docID == Scorer.NO_MORE_DOCS) {
                position = -1;
                current = null;
            } else {
                long t1 = System.nanoTime();
                XdmItem doc = docCache.get(docID);
                NodeInfo item = (NodeInfo) doc.getUnderlyingValue();
                // assert documents in order 
                assert (current == null || ((TinyDocumentImpl)item).getDocumentNumber() > ((TinyDocumentImpl)current).getDocumentNumber());
                current = item;
                ++position;
                if (stats != null) {
                    stats.retrievalTime += System.nanoTime() - t1;
                }
            }
        } catch (IOException e) {
            throw new XPathException(e);
        } finally {
            if (stats != null) {
                if (position >= 0) {
                    stats.docCount += (position - startPosition);
                }
                stats.totalTime += System.nanoTime() - t;
            }
        }
        return current;
    }

    public NodeInfo current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        // Saxon doesn't call this reliably
    }

    public SequenceIterator<NodeInfo> getAnother() throws XPathException {
        try {
            return new SearchResultIterator (saxon, query);
        } catch (IOException e) {
            throw new XPathException (e);
        }
    }

    public int getProperties() {
        return SequenceIterator.LOOKAHEAD;
    }
    

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */