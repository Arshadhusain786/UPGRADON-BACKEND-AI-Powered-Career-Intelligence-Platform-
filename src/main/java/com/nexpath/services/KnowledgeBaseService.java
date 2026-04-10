package com.nexpath.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final VectorStore vectorStore;

    @PostConstruct
    public void init() {
        try {
            log.info("🚀 Initializing Knowledge Base for RAG...");
            
            // 1. Load the document
            ClassPathResource resource = new ClassPathResource("knowledge.txt");
            if (!resource.exists()) {
                log.warn("⚠️ knowledge.txt not found in resources. RAG will be empty.");
                return;
            }

            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            // 2. Split into chunks for better retrieval
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            // 3. Add to VectorStore
            vectorStore.add(splitDocs);
            
            log.info("✅ Knowledge Base indexed successfully with {} chunks.", splitDocs.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Knowledge Base: {}", e.getMessage(), e);
        }
    }
}
