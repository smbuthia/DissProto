/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dissertationprototype.enhanced;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author smbuthia
 */
public class HTMLArticle {
    
    String headline;
    String summary;
    List<String> paragraphs;

    public HTMLArticle(String urlString) {
        try {
            Document document = Jsoup
                    .connect(urlString)
                    .userAgent("Mozilla")
                    .timeout(30000)
                    .get();
            headline = getArticleHeadline(document);
            summary = getArticleSummary(document);
            paragraphs = getArticleBodyText(document);
        } catch (IOException ex) {
            Logger.getLogger(HTMLArticle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static String getArticleHeadline(Document doc) {
        Element headerElement = doc.getElementsByTag("header").get(1);
        if (headerElement != null) {
            Elements h1Elements = headerElement.getElementsByTag("h1");
            if (h1Elements != null) {
                Element titleElement = h1Elements.first();
                if (titleElement.hasText()) {
                    return titleElement.text();
                }
            }
        }
        return "Daily Nation";
    }

    private static String getArticleSummary(Document doc) {
        Element articleSummary = doc.select("section.summary").first();
        StringBuilder summaryText = new StringBuilder();
        if (articleSummary != null) {
            articleSummary.getElementsByTag("li").stream().forEach((liItem) -> {
                summaryText.append(liItem.text());
            });
        }
        return summaryText.toString();
    }

    private static List<String> getArticleBodyText(Document doc) {
        List<String> bodyItemsList = new ArrayList<>();
        Elements sections = doc.getElementsByClass("body-copy");
        if (sections != null) {
            Element section = sections.first();
            Elements articleBodies = section.getElementsByTag("div");
            articleBodies.stream().forEach((articleBody) -> {
                bodyItemsList.add(articleBody.getElementsByTag("p").first().text());
            });
        }
        return bodyItemsList;
    }
}
