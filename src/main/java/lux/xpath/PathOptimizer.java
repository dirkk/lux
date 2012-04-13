package lux.xpath;

import java.util.LinkedList;

import lux.XPathQuery;
import lux.api.ValueType;
import lux.xpath.PathStep.Axis;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;

public class PathOptimizer extends ExpressionVisitor {

    private LinkedList<XPathQuery> queryStack;
    //private AbstractExpression expr;
    
    private final String attrQNameField = "lux_att_name_ms";
    private final String elementQNameField = "lux_elt_name_ms";
    
    private static final XPathQuery MATCH_ALL_QUERY = XPathQuery.MATCH_ALL;
    
    public PathOptimizer() {
        queryStack = new LinkedList<XPathQuery>();
        queryStack.push(MATCH_ALL_QUERY);
    }
    
   
    /*
     public AbstractExpression optimized () {
        if (queryStack.isEmpty()) {
            return expr;
        }
        return new FunCall (FunCall.luxSearchQName, new LiteralExpression(getQuery().toString()), expr);
    }
    */
    
    public void visit(AbstractExpression expr) {
        System.out.println ("visit " + expr);
        expr.accept (this);
    }
    
    public void visit (Root expr) {
        push (MATCH_ALL_QUERY);
    }
    
    // An absolute path is a PathExpression whose left-most expression is a Root.
    // divide the expression tree into regions bounded on the right and left by Root
    // then optimize these absolute paths as searches, and then optimize some functions 
    // like count(), exists(), not() with arguments that are searches
    //
    // also we may want to collapse some path expressions when they return documents;
    // like //a/root().  in this case 
    public void visit(PathExpression pathExpr) {
        System.out.println ("visit path " + pathExpr);
        XPathQuery rq = pop();
        XPathQuery lq = pop();
        XPathQuery query = combineQueries (lq,  Occur.MUST, rq, Occur.MUST, rq.getResultType());
        push(query);
    }
    
    public void visit(PathStep step) {
        QName name = step.getNodeTest().getQName();
        Axis axis = step.getAxis();
        boolean isMinimal = (axis == Axis.Descendant|| axis == Axis.DescendantSelf|| axis == Axis.Attribute);
        XPathQuery query;
        if (name == null) {
            ValueType type = step.getNodeTest().getType();
            if (axis == Axis.Self && (type == ValueType.NODE || type == ValueType.VALUE)) {
                // if axis==self and the type is loosely specified, use the prevailing type
                // TODO: handle this when combining queries?
                type = getQuery().getResultType();
            }
            else if (axis == Axis.AncestorSelf && (type == ValueType.NODE || type == ValueType.VALUE)
                    && getQuery().getResultType() == ValueType.DOCUMENT) {
                type = ValueType.DOCUMENT;
            }
            query = new XPathQuery (null, new MatchAllDocsQuery(), getQuery().getFacts(), type);
        } else {
            TermQuery termQuery = nodeNameTermQuery(step.getAxis(), name);
            query = new XPathQuery (null, termQuery, isMinimal ? XPathQuery.MINIMAL : 0, step.getNodeTest().getType());
        }
       push(query);
    }
    
    public void visit(FunCall funcall) {
        Occur occur = Occur.SHOULD;
        if (funcall.getQName().equals(FunCall.notQName)) {
            occur = Occur.MUST;
        }
        combineTopQueries(funcall.getSubs().length, occur, funcall.getReturnType());
    }
        
    private static final XPathQuery combineQueries(XPathQuery lq, Occur loccur, XPathQuery rq, Occur roccur, ValueType valueType) {
        return lq.combine(loccur, rq, roccur, valueType);
    }
    
    private TermQuery nodeNameTermQuery(Axis axis, QName name) {
        String nodeName = name.getClarkName(); //name.getLocalPart();
        String fieldName = (axis == Axis.Attribute) ? attrQNameField : elementQNameField;
        TermQuery termQuery = new TermQuery (new Term (fieldName, nodeName));
        return termQuery;
    }
    
    @Override
    public void visit(Dot dot) {
        // FIXME - should have value type=VALUE?
        push(XPathQuery.MATCH_ALL);
    }

    @Override
    public void visit(BinaryOperation op) {
        System.out.println ("visit binary op " + op);
        XPathQuery rq = pop();
        XPathQuery lq = pop();
        ValueType type = null;
        Occur occur = Occur.SHOULD;
        boolean minimal = false;
        switch (op.getOperator()) {
        
        case AND: 
            occur = Occur.MUST;
        case OR:
            minimal = true;
        case EQUALS: case NE: case LT: case GT: case LE: case GE:
            type = ValueType.BOOLEAN;
            break;
            
        case ADD: case SUB: case DIV: case MUL: case IDIV: case MOD:
            type = ValueType.ATOMIC;
            // TODO: verify that casting an empty sequence to an operand of an operator expeciting atomic values raises an error
            occur = Occur.MUST;
            break;
            
        case AEQ: case ANE: case ALT: case ALE: case AGT: case AGE:
            type = ValueType.BOOLEAN;
            occur = Occur.MUST;
            break;
            
        case IS: case BEFORE: case AFTER:
            occur = Occur.MUST;
            break;
            
        case INTERSECT:
            occur = Occur.MUST;
        case UNION:
            minimal = true;
            break;
            
        case EXCEPT:
            type = lq.getResultType().promote (rq.getResultType());
            push (combineQueries(lq, Occur.MUST, rq, Occur.MUST_NOT, type));
            return;
        }
        XPathQuery query = lq.combine(rq, occur);
        if (type != null) {
            query.setType(type);
        }
        if (minimal == false) {
            query.setFact(XPathQuery.MINIMAL, false);
        }
        push (query);
    }

    @Override
    public void visit(LiteralExpression literal) {
        push (XPathQuery.MATCH_NONE);
    }

    @Override
    public void visit(Predicate predicate) {
        XPathQuery filterQuery = pop();
        XPathQuery baseQuery = pop();
        XPathQuery query = baseQuery.combine(filterQuery, Occur.MUST);
        /* FIXME
            // in the case that this is a numeric expression (int range expression)
             // we can't predict whether the predicate will be satisfied
        if (filterQuery.getResultType().isAtomic) {
            query.setFact(XPathQuery.MINIMAL, false);
        }
        */
        push (query);
        System.out.println ("visit predicate " + predicate);
    }

    @Override
    public void visit(Sequence sequence) {
        combineTopQueries (sequence.getSubs().length, Occur.SHOULD);
    }

    private void combineTopQueries (int n, Occur occur) {
        combineTopQueries (n, occur, null);
    }

    private void combineTopQueries (int n, Occur occur, ValueType valueType) {
        XPathQuery query = pop();
        for (int i = 0; i < n-1; i++) {
            query = pop().combine(query, occur);
        }
        if (valueType != null) {
            if (query.isImmutable())
                query = new XPathQuery(null, query.getQuery(), query.getFacts(), valueType);
            else
                query.setType(valueType);
        }
        push (query);
    }

    @Override
    public void visit(SetOperation expr) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void visit(UnaryMinus predicate) {
        // TODO Auto-generated method stub
        
    }

    public XPathQuery pop() {
        return queryStack.pop();
    }
    
    public void push(XPathQuery query) {
        queryStack.push(query);
    }
    
    public XPathQuery getQuery() {        
        return queryStack.getFirst();
    }


}