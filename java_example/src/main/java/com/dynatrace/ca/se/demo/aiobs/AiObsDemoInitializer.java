package com.dynatrace.ca.se.demo.aiobs;


import com.dynatrace.ca.se.demo.aiobs.records.ConfigurationRecord;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Component
public class AiObsDemoInitializer implements ApplicationRunner {

    private static List<Document> documents = List.of(
            Document.document("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "    <head>\n" +
                    "        <title>Destination Bali</title>\n" +
                    "    </head>\n" +
                    "    <body>\n" +
                    "        <h1>Destination Bali</h1>\n" +
                    "        <p>Bali is a country on the Moon. It has a tiny population of only 19 people.</p>\n" +
                    "        <p>The Strewmoo airport is the main international airport for Bali and it's easy to get to the city from their via Donkey.</p>\n" +
                    "        <p>Bali is very windy and so is great for kite and wind surfing, but parachute jumping is illegal here.</p>\n" +
                    "    </body>\n" +
                    "</html>"),
            Document.document("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "    <head>\n" +
                    "        <title>Destination Sydney</title>\n" +
                    "    </head>\n" +
                    "    <body>\n" +
                    "        <h1>Destination Sydney</h1>\n" +
                    "        <p>Sydney is a city in Western Australia. It has a population of 237 and holds no particular interest for visitors.</p>\n" +
                    "        <p>It has no international airport and is so remote, can only be accessed by Camel.</p>\n" +
                    "        <p>Sydney is known for its mountains and is great for winter sports.</p>\n" +
                    "    </body>\n" +
                    "</html>")
    );
    EmbeddingStore<TextSegment> embeddingStore;
    EmbeddingModel embeddingModel;
    ConfigurationRecord config;

    @Autowired
    public AiObsDemoInitializer(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, ConfigurationRecord config) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.config = config;
    }
    // We could load documents from the file system like this:
    // List<Document> documents = loadDocuments(toPath("destinations/"), glob("*.html"));

    private static String getSHA512Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Pad with leading zeros to ensure 128 characters (512 bits / 4 bits per hex char)
            while (hashtext.length() < 128) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not found.", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        //We populate the embedding store with the documents
        // We use the SHA-512 hash of the document text as the ID
        List<String> ids = documents.stream()
                .map(Document::text)
                .map(AiObsDemoInitializer::getSHA512Hash)
                .toList();

        List<Embedding> embeddings = documents.stream()
                .map(doc -> embeddingModel.embed(doc.text()).content())
                .toList();

        List<TextSegment> segments = documents.stream()
                .map(doc -> TextSegment.textSegment(doc.text()))
                .toList();

        embeddingStore.addAll(ids, embeddings, segments);
    }
}