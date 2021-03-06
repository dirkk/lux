package lux.functions;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;

public abstract class InterpreterCall extends NamespaceAwareFunctionCall {

    protected void bindParameters (@SuppressWarnings("rawtypes") SequenceIterator<? extends Item> params) throws XPathException {
        Item<?> param;
        while ((param = params.next()) != null) {
            Item<?> value = params.next();
            if (value == null) {
                throw new XPathException ("Odd number of items in third argument to lux:transform, which should be parameter/value pairs");
            }
            String paramName = param.getStringValue();
            String[] parts = paramName.split(":", 2);
            StructuredQName sQName;
            if (parts.length < 2) {
                sQName = new StructuredQName("", "", paramName);
            } else {
                String prefix = parts[0];
                String name = parts[1];
                String nsURI = getNamespaceResolver().getURIForPrefix(prefix, false);
                sQName = new StructuredQName(prefix, nsURI, name);
            }
            setParameter(sQName, value);
        }
    }
    
    protected abstract void setParameter (StructuredQName name, Item<?> value);
}