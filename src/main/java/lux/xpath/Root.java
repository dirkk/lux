package lux.xpath;

import lux.ExpressionVisitor;

public class Root extends AbstractExpression {

    public Root () {
        super (Type.ROOT);
        subs = new AbstractExpression[0];
    }
    
    @Override
    public void toString(StringBuilder buf) {
        buf.append ("(/)");
    }
    
    @Override
    public boolean isAbsolute() {
        return true;
    }

    public AbstractExpression accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }
    
    /**
     * @return 0
     */
    @Override public int getPrecedence () {
        return 100;
    }

    /** 
     * replace this with the search function call
     * @param search the search function call to use
     * @return the search function call
     */
    public AbstractExpression replaceRoot(FunCall search) {        
        return search;
    }

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
