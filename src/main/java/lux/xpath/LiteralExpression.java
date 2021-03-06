
package lux.xpath;

import java.math.BigDecimal;

import javax.xml.bind.DatatypeConverter;

import lux.exception.LuxException;
import lux.xml.QName;
import lux.xml.ValueType;

public class LiteralExpression extends AbstractExpression {
    
    private final Object value;
    private final ValueType valueType;
    
    public LiteralExpression (Object value, ValueType valueType) {
        super(Type.LITERAL);
        this.value = value;
        this.valueType = valueType;
    }

    public LiteralExpression (Object value) {
        super(Type.LITERAL);
        this.value = value;
        if (value != null) {
            valueType = computeType (value);
        } else {
            valueType = ValueType.VALUE;
        }
    }

    public static final LiteralExpression EMPTY = new LiteralExpression ("()", ValueType.EMPTY);
    public static final LiteralExpression ONE = new LiteralExpression (1L);
    
    private static ValueType computeType (Object value) {
        if (value instanceof String) {
            return ValueType.STRING;
        } else if (value instanceof Integer || value instanceof Long) {
            return ValueType.INTEGER;
        } else if (value instanceof Double) {
            return ValueType.DOUBLE;
        } else if (value instanceof Float) {
            return ValueType.FLOAT;
        } else if (value instanceof BigDecimal) {
            return ValueType.DECIMAL;
        } else if (value instanceof Boolean) {
            return ValueType.BOOLEAN;
        } else if (value instanceof QName) {
        	return ValueType.QNAME;
        }
        throw new LuxException ("unsupported java object type: " + value.getClass().getSimpleName());
    }
        
    /**
     * @return 100
     */
    @Override public int getPrecedence () {
        return 100;
    }

    public ValueType getValueType () {
        return valueType;
    }

    public Object getValue() {
        return value;
    }
    
    /**
     * renders the literal as parseable XQuery.  Note that 
     */
    @Override
    public void toString(StringBuilder buf) {
        if (value == null) {
            buf.append ("()");
            return;
        }
        switch (valueType) {
        case UNTYPED_ATOMIC:
            buf.append ("xs:untypedAtomic(");
            quoteString (value.toString(), buf);
            buf.append (')');
            break;
            
        case STRING:
            quoteString (value.toString(), buf);
            break;
            
        case BOOLEAN:
            buf.append ("fn:").append(value).append("()");
            break;
            
        case FLOAT:
            Float f = (Float) value;
            if (f.isInfinite()) {
                if (f > 0)
                    buf.append ("xs:float('INF')");
                else
                    buf.append ("xs:float('-INF')");
            }
            else if (f.isNaN()) {
                buf.append ("xs:float('NaN')");
            }
            else {
                buf.append ("xs:float(").append(f).append(')');
            }
            break;
            
        case DOUBLE:
            Double d = (Double) value;
            if (d.isInfinite()) {
                if (d > 0)
                    buf.append ("xs:double('INF')");
                else
                    buf.append ("xs:double('-INF')");
            }
            else if (d.isNaN()) {
                buf.append ("xs:double('NaN')");
            }
            else {
                buf.append ("xs:double(").append(d).append(')');
            }
            break;

        case DECIMAL:            
            buf.append("xs:decimal(").append (((BigDecimal)value).toPlainString()).append(")");
            break;
            
        case HEX_BINARY:
            buf.append("xs:hexBinary(\"");
            appendHex(buf, (byte[])value);
            buf.append("\")");
            break;
            
        case BASE64_BINARY:
            buf.append("xs:base64Binary(\"");
            buf.append(DatatypeConverter.printBase64Binary((byte[])value));
            buf.append("\")");
            break;
        
        case DATE:
        case DATE_TIME:
        case TIME:
        case DAY:
        case MONTH:
        case MONTH_DAY:
        case YEAR:
        case YEAR_MONTH:
        case DAY_TIME_DURATION:
        case YEAR_MONTH_DURATION:
            buf.append(valueType.name).append("(\"").append(value).append("\")");
            break;
            
        case QNAME:
            buf.append("fn:QName(");
            quoteString(((QName)value).getNamespaceURI(), buf);
            buf.append (",\"");
            ((QName)value).toString(buf);
            buf.append("\")");
            break;
            
        case INT:
            buf.append(valueType.name).append("(").append(value).append(")");
            break;
            
        case ATOMIC:
        default:
            // rely on the object's toString method - is it only xs:int and its ilk that do this?
            buf.append (value);
        }
    }

    private static char hexdigits[] = new char[] { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };

    private void appendHex(StringBuilder buf, byte[] bytes) {
        for (byte b : bytes) {
            int b1 = ((b & 0xF0) >> 4);
            buf.append (hexdigits[b1]);
            int b2 = b & 0xF;
            buf.append (hexdigits[b2]);
        }
    }
    
    /**
     * Append the string to the buffer, with characters escaped appropriately for XML (and XQuery) text.
     * The characters ", &, <, >, {, }, and \r are replaced with character entities or numeric character references.
     * @param s the appended string
     * @param buf the buffer appended to
     */
    public static void escapeText (String s, StringBuilder buf) {
        for (char c : s.toCharArray()) {
            switch (c) {
            case '{' : buf.append("&#x7B;"); break;
            case '}' : buf.append("&#x7D;"); break;
            //case '"': buf.append ("\"\""); break;
            case '>': buf.append ("&gt;"); break;
            case '<': buf.append ("&lt;"); break;
            case '"': buf.append ("&quot;"); break;
            case '&': buf.append ("&amp;"); break;
            case '\r': buf.append("&#xD;"); break;  // XML line ending normalization removes these unless they come in as character references
            default: buf.append (c);
            }
        }        
    }
    
    /**
     * Append the string to the buffer, escaped as in {@link #escapeText(String, StringBuilder)}, surrounded
     * by double quotes (").
     * @param s the appended string
     * @param buf the buffer appended to
     */
    public static void quoteString(String s, StringBuilder buf) {
        buf.append ('"');
        escapeText (s, buf);
        buf.append ('"');
    }

    @Override
    public AbstractExpression accept(ExpressionVisitor visitor) {
        return visitor.visit(this);
    }
    
    @Override 
    public boolean equals (Object other) {
        if (other instanceof LiteralExpression) {
            return value.equals(((LiteralExpression)other).value);
        }
        return false;
    }
    
    @Override
    public int hashCode () {
        return value.hashCode();
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
