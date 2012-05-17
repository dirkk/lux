package lux.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.ToStringUtils;

public class LuxTermQuery extends TermQuery {

    public LuxTermQuery(Term t) {
        super(t);
    }
    
    @Override
    public String toString (String field) {
        return LuxTermQuery.toString (field, getTerm(), getBoost());
    }
    
    public static String toString (String field, Term term, float boost) {

        StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field)) {
          buffer.append(term.field());
          buffer.append(":");
        }
        // quote the term text in case it is an operator
        buffer.append ('\"');
        if (term.text().indexOf('\"') >= 0) {
            buffer.append (term.text().replace ("\"", "\\\""));
        } else {
            buffer.append (term.text());
        }
        buffer.append ('\"');
        buffer.append(ToStringUtils.boost(boost));
        return buffer.toString();
    }

}