package com.gooddata.maql.jdbc.driver;

import com.fasterxml.jackson.databind.JsonNode;

public class MdObject {

	    private String title;
	    private String category;
	    private String link;
	    private String aggregation;
	    private String alias;
	    private String identifier;
	    
	    MdObject(String title, String category, String link, String identifier)
	    {
	    	this.title=title;
	    	this.category=category;
	    	this.link=link;
	    	this.identifier=identifier;
	    	this.alias=null;
	    }

	    MdObject(JsonNode node)
	    {
	        this.title=node.get("title").asText();
	        this.category=node.get("category").asText();
	        this.link=node.get("link").asText();
	        this.identifier=node.get("identifier").asText();
	    }
	    
	    public String getTitle()
	    {
	    	return this.title;
	    }
	    
	    public String getIdentifier()
	    {
	    	return this.identifier;
	    }
	    
	    public String getAlias()
	    {
	    	return this.alias;
	    }
	    
	    public String setAlias(String alias)
	    {
	    	return this.alias=alias;
	    }
	    
	    public String getCategory()
	    {
	    	return this.category;
	    }
	    
	    public String getLink()
	    {
	    	return this.link;
	    }
	    
	    public String toString()
	    {
	    	return this.title+' '+this.category+' '+this.link;
	    }
	    
	    public boolean isMetric()
	    {
	    	return this.category.equals("metric");
	    }
	    
	    public boolean isFact()
	    {
	    	return this.category.equals("fact");
	    }
	    
	    public boolean isAttribute()
	    {
	    	return this.category.equals("attribute");
	    }
	    
	    public void setAggregation(String aggregation)
	    {
	    	this.aggregation = aggregation;
	    }
	    
	    public String getAggregation()
	    {
	    	return aggregation;
	    }
	
}
