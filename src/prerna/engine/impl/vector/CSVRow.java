package prerna.engine.impl.vector;

import com.pgvector.PGvector;

public class CSVRow {
	
	private PGvector embeddings = null; // This could be a placeholder or identifier for actual embeddings
	private String source;
	private String modality;
	private String divider;
	private String part;
	private Integer tokens;
	public String content;
	private String keywords = "";

    public CSVRow(String source, String modality, String divider, String part, int tokens, String content) {
        // Initially, embeddings might not be set
        this.source = source;
        this.modality = modality;
        this.divider = divider;
        this.part = part;
        this.tokens = tokens;
        this.content = content;
    }

    // Method to update the embeddings for a row
    public void setEmbeddings(PGvector embeddings) {
        this.embeddings = embeddings;
    }
    
    public PGvector getEmbeddings() {
        return this.embeddings;
    }
    
    public String getSource() {
    	return this.source;
    }
    
    public String getModality() {
    	return this.modality;
    }
    
    public String getDivider() {
    	return this.divider;
    }
    
    public String getPart() {
    	return this.part;
    }

    public Integer getTokens() {
    	return this.tokens;
    }
    
    public String getContent() {
    	return this.content;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public String getKeywords() {
        return this.keywords;
    }
}
