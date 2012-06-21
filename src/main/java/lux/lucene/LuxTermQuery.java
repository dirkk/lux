package lux.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.util.ToStringUtils;

/**
 * An analogue of TermQuery whose toString method produces an expression that can be parsed by
 * Lucene's standard query parser (and its surround query parser).
 *
 */
public class LuxTermQuery extends ParseableQuery {

    private final Term term;
    
    private final float boost;
    
    public LuxTermQuery(Term t, float boost) {
        this.term = t;
        this.boost = boost;
    }
    
    public LuxTermQuery(Term t) {
        this (t, 1.0f);
    }
    
    public String toXml (String field) {
        StringBuilder buffer = new StringBuilder("<TermQuery");
        if (term.field() != null && !term.field().equals(field)) {
            buffer.append (" fieldName=\"").append (field).append ("\"");
        }
        if (boost != 1) {
            buffer.append (" boost=\"").append(boost).append("\"");
        }
        buffer.append(">").append(term.text()).append("</TermQuery>");
        return buffer.toString();
    }
    
    public String toString (String field) {
        return LuxTermQuery.toString (field, term, boost);
    }
    
    public static String toString (String field, Term term, float boost) {

        StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field) && !term.field().isEmpty()) {
          buffer.append(term.field());
          buffer.append(":");
        }
        // quote the term text in case it is an operator
        buffer.append ('"');
        String value = term.text();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': buffer.append("\\\""); break;
                case '\\': buffer.append("\\\\"); break;
                default: buffer.append(c); break;
            }
        }
        buffer.append ('"');
        buffer.append(ToStringUtils.boost(boost));
        return buffer.toString();
    }

    public Term getTerm() {
        return term;
    }

    public float getBoost() {
        return boost;
    }

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
