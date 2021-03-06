package lux.index.field;

import java.util.Collections;

import lux.index.XmlIndexer;

import org.apache.lucene.document.Field.Store;

/**
 * A stored field that is used to store the entire XML document.
 *
 */
public class DocumentField extends FieldDefinition {
    
    private static final DocumentField instance = new DocumentField();
    
    public static DocumentField getInstance() {
        return instance;
    }
    
    protected DocumentField () {
        super ("lux_xml", null, Store.YES, Type.STRING, true);
    }
    
    /**
     * This will be a byte[] value if the document is a binary document, or if it is an 
     * XML document indexed using TinyBinary.  Otherwise it will be a String value.
     */
    @Override
    public Iterable<?> getValues(XmlIndexer indexer) {
        byte[] bytes = indexer.getDocumentBytes();
        if (bytes != null) {
            return Collections.singleton(bytes);
        }
        return Collections.singleton(indexer.getDocumentText());
    }

}
