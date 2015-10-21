/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dissertationprototype;
//<editor-fold desc="imports" defaultstate="collapsed">

import com.google.gson.Gson;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
public class DissertationPrototype extends DefaultHandler {

    private boolean isLink = false;
    private static List<String> list;

    public DissertationPrototype() {
        super();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.setProperty("proxyHost", "172.16.30.6");
        System.setProperty("proxyPort", "8080");
        try {
            list = new ArrayList<>();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            DissertationPrototype handler = new DissertationPrototype();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);

            URL url = new URL("http://www.nation.co.ke/rss.xml");
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("user-agent", "Mozilla");

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            xr.parse(new InputSource(br));

            list.stream().forEach((li) -> {
                System.out.println(li);
            });
            String jsonString = processUtterance(list);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");
            Date date = new Date();
            String filename = "C:\\wamp\\www\\diss-proto\\json\\" + dateFormat.format(date) + ".json";
            try (PrintWriter writer = new PrintWriter(filename)) {
                writer.println(jsonString);
            }
        } catch (IOException | SAXException ex) {
            Logger.getLogger(DissertationPrototype.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String processUtterance(List<String> urls) throws IOException {
        Gson gson = new Gson();
        Map<String, Map> jsonMap = new HashMap<>();

        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        for (String url : urls) {
            Map<String, Map> dataMap = new HashMap<>();
            Map<String, Map> neMap = new HashMap<>();
            Map<String, Integer> neWordMap = new HashMap();
            Map<String, Map> wordsMap = new HashMap<>();
            Map<String, Integer> wordMap;
            Map<String, String> titleMap = new HashMap<>();

            if (url.startsWith("http://www.nation.co.ke/")) {
                Document doc = Jsoup
                        .connect(url)
                        .userAgent("Mozilla")
                        .timeout(30000)
                        .get();
                Elements sections = doc.getElementsByClass("body-copy");
                if (sections != null) {
                    Element section = sections.first();
                    if (section != null) {
                        Elements articleHeadlines = doc.getElementsByAttributeValue("itemprop", "headline name");
                        Elements articleBodies = section.getElementsByAttributeValue("itemprop", "articleBody");
                        if (articleBodies != null) {
                            Element articleBody = articleBodies.first();
                            Element articleHeadline = articleHeadlines.first();
                            Element articleSummary = doc.select("section.summary").first();
                            StringBuilder summaryText = new StringBuilder();
                            String title = "Daily Nation";
                            if (articleSummary != null) {
                                for (Element liItem : articleSummary.getElementsByTag("li")) {
                                    summaryText.append(liItem.text());
                                }
                                if (articleHeadline != null) {

                                    title = articleHeadline.text();
                                }
                                titleMap.put(title, summaryText.toString());
                                dataMap.put("Summary", titleMap);
                            }

                            if (articleBody != null) {
                                String prevNeWord = "";
                                String prevNe = "";
                                String neWord;
                                for (Element paragraph : articleBody.getElementsByTag("p")) {
                                    String text = paragraph.text();

                                    //create an empty Annotation just with the given text
                                    Annotation document = new Annotation(text);
                                    //run all Annotators on this text
                                    pipeline.annotate(document);
                                    //a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
                                    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
                                    for (CoreMap sentence : sentences) {
                                        int wordCount;
                                        // traversing the words in the current sentence
                                        //a CoreLabel is a CoreMap with additional token-specific methods
                                        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                                            wordCount = 1;
                                            String word = token.get(CoreAnnotations.TextAnnotation.class);
                                            String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                                            String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                // Will try to focus more on NER to reduce file size and processing load
                /*
                                             if (wordsMap.containsKey(word)) {
                                             wordMap = wordsMap.get(word);
                                             if (wordMap.containsKey(pos)) {
                                             wordCount = wordMap.get(pos) + 1;
                                             wordMap.put(pos, wordCount);
                                             } else {
                                             wordMap.put(pos, wordCount);
                                             }
                                             } else {
                                             wordMap = new HashMap<>();
                                             wordMap.put(pos, wordCount);
                                             }
                                             wordsMap.put(word, wordMap);
                                             */
                                            //We have to get the full name entity including title which will not have been tagged by the ner
                                            //this could involve n-gram processing and addition of custom ners
                                            if (!ne.equalsIgnoreCase("O")) {
                                                neWord = word;
                                                if (ne.equalsIgnoreCase(prevNe)) {
                                                    neWord = prevNeWord + " " + neWord;
                                                    neWordMap.remove(prevNeWord);
                                                    neWordMap.put(neWord, wordCount);
                                                } else {
                                                    if (neMap.containsKey(ne)) {
                                                        neWordMap = neMap.get(ne);
                                                    } else {
                                                        neWordMap = new HashMap();
                                                    }
                                                    if (neWordMap.containsKey(neWord)) {
                                                        wordCount = neWordMap.get(neWord) + 1;
                                                    }
                                                    neWordMap.put(neWord, wordCount);    //"TSC":3
                                                }
                                                prevNeWord = neWord;
                                                neMap.put(ne, neWordMap);   //"ORGANIZATION":{"TSC":3,"Kuppet":5}
                                            }
                                            prevNe = ne;
                                        }
                                    }
                                }
                                dataMap.put("NER", neMap);
//        dataMap.put("WORD", wordsMap);
                                jsonMap.put(url, dataMap);
                            }
                        }
                    }
                }
            }
        }
        return gson.toJson(jsonMap);
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
