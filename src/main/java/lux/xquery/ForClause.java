package lux.xquery;

import lux.xpath.AbstractExpression;

public class ForClause extends FLWORClause {
    
    private Variable var;
    private Variable pos;
    private AbstractExpression seq;

    /**
     * create an XQuery 'for' clause
     * @param var the range variable (for $x)
     * @param pos the position variable (at $n)
     * @param seq the sequence (in ...)
     */
    public ForClause(Variable var, Variable pos, AbstractExpression seq) {
        this.var = var;
        this.pos = pos;
        this.seq = seq;
    }

    @Override
    public void toString(StringBuilder buf) {
        buf.append ("for ");
        var.toString(buf);
        if (pos != null) {
            buf.append (" at ");
            pos.toString(buf);
        }
        buf.append (" in ");
        seq.toString(buf);
    }

}
