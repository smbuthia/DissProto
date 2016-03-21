/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dissertationprototype.enhanced;

//<editor-fold desc="imports" defaultstate="collapsed">
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
//</editor-fold>

/**
 *
 * @author smbuthia
 */
public class RSSFeed extends DefaultHandler {

    private boolean isLink = false;
    private List<String> list;

    public RSSFeed(String urlString) {
        super();
        try {
            list = new ArrayList<>();
            //Intantiate an XMLReader object that will read RSS/XML content
            XMLReader xr = XMLReaderFactory.createXMLReader();
            RSSFeed handler = this;
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            //Create URL object for the main feed being read
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("user-agent", "Mozilla");

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            xr.parse(new InputSource(br));
        } catch (MalformedURLException ex) {
            Logger.getLogger(RSSFeed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | SAXException ex) {
            Logger.getLogger(RSSFeed.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public List<String> getUrls() {
        return list;
    }

    @Override
    public void startElement(String uri, String name,
            String qName, Attributes atts) {
        if (qName.equalsIgnoreCase("link")) {
            isLink = true;
        }
    }

    @Override
    public void endElement(String uri, String localName,
            String qName) throws SAXException {
        if (qName.equalsIgnoreCase("link")) {
            isLink = false;
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (isLink) {
            list.add(new String(ch, start, length));
        }
    }
}
