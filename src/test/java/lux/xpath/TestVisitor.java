package lux.xpath;

import lux.compiler.PathOptimizer;
import lux.index.IndexConfiguration;
import lux.xml.QName;
import lux.xml.ValueType;
import lux.xpath.PathStep.Axis;

import org.junit.Test;

public class TestVisitor {

    @Test public void testVisit () {
        PathOptimizer opt = new PathOptimizer (IndexConfiguration.DEFAULT);
        PathExpression expr = new PathExpression (
                new Root(),
                new Predicate(
                new PathStep (Axis.Child, new NodeTest (ValueType.ELEMENT, new QName("foo"))),
                new LiteralExpression (1))
                );
        expr.accept(opt);
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
