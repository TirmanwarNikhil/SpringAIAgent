package com.nikhil.SpringAIAgent.data.loader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PdfIngestionDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final VectorStore vectorStore;
    
    private static final Logger logger = LoggerFactory.getLogger(PdfIngestionDataLoader.class);
    
    @Value("${app.pdf-path}")
    private Resource pdfResource;

    public PdfIngestionDataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            logger.info("==============>>> STARTING PDF INGESTION PIPELINE <<<==============");
            
            // 1. Read the PDF File
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
            List<Document> rawDocuments = pdfReader.get();
            logger.info("-> Successfully parsed PDF. Total Pages: {}", rawDocuments.size());

            // 2. Split the text into tokens/chunks
            @SuppressWarnings("removal")
			TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(rawDocuments);
            logger.info("-> Text splitter generated chunks count: {} ", splitDocuments.size());

            // 3. Strip out illegal Postgres null-byte characters
            List<Document> sanitizedDocuments = splitDocuments.stream()
                .map(doc -> {
                    String cleanText = doc.getText().replace("\u0000", ""); 
                    return new Document(doc.getId(), cleanText, doc.getMetadata());
                })
                .toList();

            // 4. Force write to the vector store database
            logger.info("-> Syncing embeddings to pgvector... (Please wait a moment)");
            vectorStore.accept(sanitizedDocuments);
            
            logger.info("==============>>> PDF DATA CONVERTED & INSTANTLY SAVED TO DB! <<<==============");
            
        } catch (Exception e) {
            logger.error("!!! Critical Error during automatic PDF Ingestion: {}" , e.getMessage());
            e.printStackTrace();
        }
    }
}