package modification;


import edu.thuir.sentires.profile.*;
import net.librec.data.model.ArffAttribute;

import java.util.ArrayList;
import java.util.Set;

public class ProductAdder extends Product{

    private ArrayList<ArffAttribute> attributes;
    private String userId;
    private String productName;
    private String[] data;

    public ProductAdder(String productName, String url) {
        super(productName, url);
        this.productName = productName;
        data = null;
    }

    public ProductAdder(String productName, String url, ArrayList<ArffAttribute> attributes) {
        super(productName, url);
        this.productName = productName;
        this.attributes = attributes;
        data = null;
    }

    public void setAttributes(ArrayList<ArffAttribute> attributes) {
        this.attributes = attributes;
    }
    public void setData(String[] data) {this.data = data;}

    public String getUserId() { return this.userId; }
    public String getProductName() { return this.productName; }
    public ArffAttribute getArffAttribute(int columnId) { return attributes.get(columnId); }
    public String getData(int columnId) { return data[columnId]; }


}
