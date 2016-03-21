/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dissertationprototype.enhanced;
//<editor-fold desc="imports" defaultstate="collapsed">
import com.google.gson.Gson;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
//</editor-fold>
/**
 *
 * @author smbuthia
 */
public class Prototype {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //Set system properties (proxy)
            System.setProperty("proxyHost", "172.16.30.6");
            System.setProperty("proxyPort", "8080");
            RSSFeed feed = new RSSFeed("http://www.nation.co.ke/rss.xml");
            List<String> list = feed.getUrls();
            list.stream().forEach((li) -> {
                System.out.println(li);
            });
            String[] jsonStrings = processUtterance(list);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");
            Date date = new Date();
            String filename1 = "C:\\wamp\\www\\diss-proto\\json\\" + dateFormat.format(date) + ".json";
            writeToFile(filename1, jsonStrings[0]);
            String filename2 = "C:\\wamp\\www\\diss-proto\\json\\counties-" + dateFormat.format(date) + ".json";
            writeToFile(filename2, jsonStrings[1]);
        } catch (IOException ex) {
            Logger.getLogger(Prototype.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void writeToFile(String fileName, String content) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(content);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Prototype.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String[] processUtterance(List<String> urls) throws IOException {
        Gson gson = new Gson();
        Map<String, Map> jsonMap = new HashMap<>();

        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        /**
         * A map that holds the location name entities as mapped to counties
         */
        Map<String, Integer> countiesMap = new HashMap<>();
        for (String url : urls) {
            Map<String, Map> dataMap = new HashMap<>();
            Map<String, Map> neMap = new HashMap<>();
            Map<String, Integer> neWordMap = new HashMap();
            Map<String, Map> wordsMap = new HashMap<>();
            Map<String, Integer> wordMap;
            Map<String, String> titleMap = new HashMap<>();

            if (url.startsWith("http://www.nation.co.ke/")) {
                HTMLArticle htmlArticle = new HTMLArticle(url);
                List<String> paragraphList = htmlArticle.paragraphs;
                String summary = htmlArticle.summary;
                String title = htmlArticle.headline;
                titleMap.put(title, summary);
                dataMap.put("Summary", titleMap);
                

                if (!paragraphList.isEmpty()) {
                    String prevNeWord = "";
                    String prevNe = "";
                    String neWord;
                    for (String text : paragraphList) {
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

                                /* 
                                 We have to get the full name entity including title which will not have been
                                 tagged by the ner this could involve n-gram processing and addition of custom ners
                                 */
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
                    /*
                     at this point map out the location name entities to counties
                     */
                    Map locationsMap = neMap.get("LOCATION");
                    if (locationsMap != null) {
                        Iterator it1 = locationsMap.entrySet().iterator();
                        List<String> counties = new ArrayList<>();
                        while (it1.hasNext()) {
                            Map.Entry me = (Map.Entry) it1.next();
                            String location = me.getKey().toString();
                            //if it is a location name entity then get the county the location belongs to
                            if (!location.equalsIgnoreCase("Kenya")) {
                                String county = getCounty(location);
                                if (!county.isEmpty() && !counties.contains(county)) {
                                    counties.add(county);
                                }
                            }
                        }
                        counties.stream().forEach((county) -> {
                            if (countiesMap.containsKey(county)) {
                                int countyCount = countiesMap.get(county) + 1;
                                countiesMap.replace(county, countyCount);
                            } else {
                                countiesMap.put(county, 1);
                            }
                        });
                    }
                    dataMap.put("NER", neMap);
                    dataMap.put("WORD", wordsMap);
                    jsonMap.put(url, dataMap);
                }

            }
        }
        String[] jsonArrays = {gson.toJson(jsonMap), gson.toJson(countiesMap)};
        return jsonArrays;
    }

    public static String getCounty(String location) {
        try {
            String command = "C:/Python34/python.exe";
            String pyFile = "C:/Users/smbuthia/PycharmProjects/dissertationproject/edu/strathmore/location_processor.py";
            ProcessBuilder pbuilder = new ProcessBuilder(command, pyFile, location);
            Process proc = pbuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String county = reader.readLine();
            if (county != null) {
                if (county.length() < 20) {
                    return county;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Prototype.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

}
