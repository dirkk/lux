package lux.xpath;

public class Root extends AbstractExpression {

    public Root () {
        super (Type.Root);
    }
    
    @Override
    public String toString() {
       return "/";
    }
    
    public boolean isAbsolute() {
        return true;
    }

    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
