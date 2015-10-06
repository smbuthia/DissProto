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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//</editor-fold>

/**
 *
 * @author smbuthia
 */
public class DissertationPrototype {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            String jsonString = processUtterance();
            String filename = "C:\\Users\\smbuthia\\Desktop\\corpus-builder\\article.json";
            try (PrintWriter writer = new PrintWriter(filename)) {
                writer.println(jsonString);
            }
        } catch (IOException ex) {
            Logger.getLogger(DissertationPrototype.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String processUtterance() throws IOException {
        Gson gson = new Gson();
        Map<String, Map> jsonMap = new HashMap<>();
        Map<String, Map> dataMap = new HashMap<>();
        Map<String, Map> neMap = new HashMap<>();
        Map<String, Integer> neWordMap = new HashMap();
        Map<String, Map> wordsMap = new HashMap<>();
        Map<String, Integer> wordMap;
        String url = "http://www.nation.co.ke"
                + "/news"
                + "/TSC-to-hire-70-000-temporary-teachers-to-alleviate-strike/-/1056/2892104/-/56ridx/-/index.html";

//        Document doc = Jsoup
//                .connect(url)
//                .userAgent("Mozilla")
//                .get();
//        Elements sections = doc.getElementsByClass("body-copy");
//        Element section = sections.first();
//        Elements articleBodies = section.getElementsByAttributeValue("itemprop", "articleBody");
//        Element articleBody = articleBodies.first();
        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        String prevNeWord = "";
        String prevNe = "";
        String neWord;
//        for (Element paragraph : articleBody.getElementsByTag("p")) {
//            String text = paragraph.text();
        String text = "The Kenya National Examinations Council (Knec) has said 1.4 million "
                + "candidates will sit for national examinations this year. Knec Chief "
                + "Executive Officer Mr Joseph Kivilu said the examinations will go on "
                + "despite the ongoing teachers’ strike. Dr Kivilu said the Kenya "
                + "Certificate of Primary Education (KCPE) examinations will begin on "
                + "November 10 and end on November 12. Candidates will sit for the Kenya "
                + "Certificate of Secondary Education (KCSE) examinations from October 12. "
                + "He said 937,467 candidates are expected to sit for the KCPE examinations "
                + "and 525, 802 candidates for KCSE. Dr Kivilu asked teachers not to "
                + "disrupt learning during their strike as candidates are revising.";
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
                        neWordMap.put(neWord, wordCount);    //"TSC":3
                    }
                    prevNeWord = neWord;
                    neMap.put(ne, neWordMap);   //"ORGANIZATION":{"TSC":3,"Kuppet":5}
                }
                prevNe = ne;
            }
            //Something not right!!!!!!
            System.out.println(neMap);
        }
        System.out.println(neMap);
//        }
        dataMap.put("NER", neMap);
        dataMap.put("WORD", wordsMap);
        jsonMap.put(url, dataMap);
        return gson.toJson(jsonMap);
    }
}
