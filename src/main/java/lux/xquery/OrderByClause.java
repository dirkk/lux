package lux.xquery;

import lux.ExpressionVisitor;
import lux.xpath.AbstractExpression;

public class OrderByClause extends FLWORClause {

    private final SortKey[] sortKeys;
    
    public OrderByClause(SortKey[] sortKeys) {
        this.sortKeys = sortKeys;
    }

    @Override
    public void toString(StringBuilder buf) {
        buf.append ("order by ");
        sortKeys[0].toString(buf);
        for (int i = 1; i < sortKeys.length; i++) {
            buf.append(", ");
            sortKeys[i].toString(buf);
        }
    }

    public AbstractExpression accept(ExpressionVisitor visitor) {
        for (int i = 0; i < sortKeys.length; i++) {
            AbstractExpression key = sortKeys[i].getKey();
            AbstractExpression key2 = key.accept(visitor);
            if (key != key2) {
                sortKeys[i] = new SortKey(key2, sortKeys[i].getOrder(), sortKeys[i].getCollation(), sortKeys[i].isEmptyLeast());
            }
        }
        return null;
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
