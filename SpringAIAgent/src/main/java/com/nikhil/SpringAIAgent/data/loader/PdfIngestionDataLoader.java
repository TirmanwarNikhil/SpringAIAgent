package com.nikhil.SpringAIAgent.data.loader;

import java.util.ArrayList;
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
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Component
public class PdfIngestionDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final VectorStore vectorStore;
    
    private static final Logger logger = LoggerFactory.getLogger(PdfIngestionDataLoader.class);
    
    @Value("${app.pdf-path:classpath*:documents/*.pdf}")
    private String pdfLocationPattern;
    
    private final ResourcePatternResolver resourcePatternResolver =
            new PathMatchingResourcePatternResolver();

    public PdfIngestionDataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            logger.info("==============>>> STARTING PDF INGESTION PIPELINE <<<==============");
            
            // 1. Read the PDF File
         // 1. Resolve and read every PDF individually. A directory is not a PDF resource.
            logger.info("-> Finding PDFs with resource pattern: {}", pdfLocationPattern);
            Resource[] pdfResources = resourcePatternResolver.getResources(pdfLocationPattern);
            if (pdfResources.length == 0) {
                logger.warn("No PDF files matched '{}'. Skipping ingestion.", pdfLocationPattern);
                return;
            }

            List<Document> rawDocuments = new ArrayList<>();
            for (Resource pdfResource : pdfResources) {
                try {
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
                    List<Document> pages = pdfReader.get();
                    rawDocuments.addAll(pages);
                    logger.info("-> Parsed '{}' ({} pages)", pdfResource.getFilename(), pages.size());
                }
                catch (RuntimeException e) {
                    // Keep ingesting valid files while clearly identifying a corrupt/unsupported PDF.
                    logger.error("Could not parse PDF '{}': {}", pdfResource.getFilename(), e.getMessage());
                }
            }

            if (rawDocuments.isEmpty()) {
                logger.warn("No PDF pages were parsed successfully. Skipping ingestion.");
                return;
            }
            logger.info("-> Successfully parsed PDF pages: {}", rawDocuments.size());

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