package org.bsdevelopment.servermaster.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;

public class ModifiedHtml extends Component {
    private static final PropertyDescriptor<String, String> innerHtmlDescriptor = PropertyDescriptors.propertyWithDefault("innerHTML", "");

    public ModifiedHtml(String outerHtml) {
        super(null);
        if (outerHtml == null || outerHtml.isEmpty()) throw new IllegalArgumentException("HTML cannot be null or empty");

        setOuterHtml(outerHtml);
    }

    private void setOuterHtml(String outerHtml) {
        Document doc = Jsoup.parseBodyFragment(outerHtml);

        org.jsoup.nodes.Element root = doc.body();
        Attributes attrs = root.attributes();
        attrs.forEach(this::setAttribute);

        doc.outputSettings().prettyPrint(true);
        setInnerHtml(root.html()
                .replaceAll("<pre>", "<pre style=\"padding: 10px;\">")
                .replaceAll("<code>", "").replaceAll("</code>", ""));
    }

    private void setAttribute(Attribute attribute) {
        String name = attribute.getKey();
        String value = attribute.getValue();
        if (value == null) value = "";

        getElement().setAttribute(name, value);
    }

    private void setInnerHtml(String innerHtml) {
        set(innerHtmlDescriptor, innerHtml);
    }
}