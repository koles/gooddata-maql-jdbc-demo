package com.gooddata.maql.jdbc.driver;

import java.io.IOError;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonArray;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.gooddata.http.client.GoodDataHttpClient;
import com.gooddata.http.client.LoginSSTRetrievalStrategy;
import com.gooddata.http.client.SSTRetrievalStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;


public class GoodDataLiteClient {
	private String user;
	private String password;
	private String server;
	private String pid;
	private HttpHost host;
	private HttpClient client;	
	private final static Logger logger = Logger.getGlobal();//Logger.getLogger(Connection.class.getName());
	private ArrayList<MdObject> catalog = null;

	GoodDataLiteClient(String user, String password, String server, String pid)
	{
		this.user=user;
		this.password=password;
		this.server=server;
		this.pid=pid;
		
		this.host = new HttpHost(server, 443, "https");

    	// create login strategy, which will obtain SST via credentials
    	SSTRetrievalStrategy sstStrategy = new LoginSSTRetrievalStrategy(user, password);

    	this.client = new GoodDataHttpClient(HttpClientBuilder.create().build(), host, sstStrategy);
	}
	
	public String getPid()
	{
		return pid;
	}
	
	public void readCatalog() throws JsonProcessingException, IOException, SQLException
	{
		HttpGet getMetrics = new HttpGet("/gdc/md/"+pid+"/query/metrics");
    	getMetrics.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
    	HttpResponse getMetricsResponse;
		   	
	    logger.info("execute start");
		getMetricsResponse = client.execute(host,getMetrics);
		logger.info("execute end");
		if (getMetricsResponse.getStatusLine().getStatusCode()!=200)
		{
			// connection failed
			throw new SQLException("Connection failed. "+getMetricsResponse.getStatusLine().getReasonPhrase(),"2F004",0);
		}
			
    	String jsonString=EntityUtils.toString(getMetricsResponse.getEntity());
    	 
    	logger.info("response: "+jsonString);
		
    	//create ObjectMapper instance
    	ObjectMapper objectMapper = new ObjectMapper();

        //read JSON like DOM Parser
    	JsonNode rootNode = objectMapper.readTree(jsonString);    	
    	 
    	catalog = new ArrayList<MdObject>();
    	 
    	for (JsonNode item : rootNode.path("query").withArray("entries"))
    	{
    		if (item.get("deprecated").asInt()!=1) catalog.add(new MdObject(item));
    	}
    	 
    	// Attributes
    	
    	HttpGet getAttributes = new HttpGet("/gdc/md/"+pid+"/query/attributes");
	    getAttributes.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
	    HttpResponse getAttributesResponse;    	
		
		logger.info("execute start");
		getAttributesResponse = client.execute(host,getAttributes);
		logger.info("execute end");
		
		if (getAttributesResponse.getStatusLine().getStatusCode()!=200)
		{
			// connection failed
			throw new SQLException("Connection failed. "+getAttributesResponse.getStatusLine().getReasonPhrase(),"2F004",0);
		}
		
	    jsonString=EntityUtils.toString(getAttributesResponse.getEntity());
	    	 
	    logger.info("response: "+jsonString);
	    	
	    //read JSON like DOM Parser
	    rootNode = objectMapper.readTree(jsonString);    	  	 
	    	 
	    for (JsonNode item : rootNode.path("query").withArray("entries"))
	    {
	    	if (item.get("deprecated").asInt()!=1) catalog.add(new MdObject(item));
	    }
	    
	    // Facts
	    
	    HttpGet getFacts = new HttpGet("/gdc/md/"+pid+"/query/facts");
	    getFacts.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
	    HttpResponse getFactsResponse;    	
		
		logger.info("execute start");
		getFactsResponse = client.execute(host,getFacts);
		logger.info("execute end");
		
		if (getFactsResponse.getStatusLine().getStatusCode()!=200)
		{
			// connection failed
			throw new SQLException("Connection failed. "+getFactsResponse.getStatusLine().getReasonPhrase(),"2F004",0);
		}
		
	    jsonString=EntityUtils.toString(getFactsResponse.getEntity());
	    	 
	    logger.info("response: "+jsonString);
	    	
	    //read JSON like DOM Parser
	    rootNode = objectMapper.readTree(jsonString);    	  	 
	    	 
	    for (JsonNode item : rootNode.path("query").withArray("entries"))
	    {
	    	if (item.get("deprecated").asInt()!=1) catalog.add(new MdObject(item));
	    }
	    
    	logger.info("objects in catalog: "+catalog.size());
    }
	
	public ArrayList<MdObject> getCatalog()
	{
		return catalog;
	}
	
	public AfmCreatorListener parse(String query) throws SQLException
	{
		  CharStream stream = CharStreams.fromString(query);
		  MaqlLiteLexer lexer = new MaqlLiteLexer(stream);
		  CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
		  MaqlLiteParser parser = new MaqlLiteParser(commonTokenStream);
		  parser.removeErrorListeners();
		  parser.addErrorListener(new BaseErrorListener() 
		  {
	            @Override
	            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int pos, String msg, RecognitionException e) 
	            {
	            	throw new IllegalStateException("Syntax error: [" + line + ":" + pos + "] " + msg);
	            	
	            }
	       });
	      
		  
		  try {
			  ParseTree tree = parser.query(); 
			  
			  ParseTreeWalker walker = new ParseTreeWalker();
			  
			  AfmCreatorListener listener = new AfmCreatorListener(catalog);
			  walker.walk(listener, tree);	     
			  
			  logger.info(tree.toStringTree(parser));
			  
			  return listener;
		  
		  }
		  catch (IllegalStateException e)
		  {
		    throw new SQLException(e.getMessage(),"42000",0);
		  }
		        
	}
	
	public JsonNode execute(String sqlInput) throws ClientProtocolException, IOException, SQLException
	{
		String sql=sqlInput;
		/*
		String regexp="(?is).*from[\\s\\n\\r]*\\((.*)\\).*";
		
	    Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find())
        {
		  sql=matcher.group(1);
		  logger.info("group 1: "+sql);
        }
        else
        {
        	logger.info("not nested sql");
        }
		*/
		
		
		
		String [] columns=sql.replaceFirst("(?i)^[ \t\n]*select ", "").split("[ \t\n]*,[ \t\n]*");
		HashMap<String,String> displayForms = new HashMap<String,String>();
		ArrayList<String> filterDisplayForms = new ArrayList<String>();
		ArrayList<ArrayList<String>> elementUri=new ArrayList<ArrayList<String>>();
		SQLException elementException = null;
		
		AfmCreatorListener listener = parse(sql);
		
		if (listener.getError()!=null)
		{
			// Error detected in parset
			throw new SQLException(listener.getError(),"46121",0);
		}
		
		if (listener.getIsAllColumns())
		{
			ArrayList<String> headers = new ArrayList<String>();
			ArrayList<String> isNumber=new ArrayList<String>();
			
			ArrayList<MdObject> metrics=listener.getMetrics();
			ArrayList<MdObject> attributes=listener.getAttributes();
			
			
			for (MdObject obj : attributes)
			{
				headers.add(obj.getTitle());
				isNumber.add("false");
			}
			for (MdObject obj : metrics)
			{
				headers.add(obj.getTitle());
				isNumber.add("true");
			}
			
			String list = "\""+ String.join("\",\"", headers) +"\"";
			String list2 = String.join(",", isNumber);

			logger.info(list);
			
			String jsonString = "{\"headers\": ["+list+"],\"isnumber\":["+list2+"]}";
			
			logger.info(jsonString);
			
			//create ObjectMapper instance
		    ObjectMapper objectMapper = new ObjectMapper();

		    //read JSON like DOM Parser
		    JsonNode rootNode = objectMapper.readTree(jsonString);
		    return rootNode;
		}
		
		if (listener.getIsNumber())
		{
			String selectedNumber=listener.getSelectedNumber();
			//create ObjectMapper instance
		    ObjectMapper objectMapper = new ObjectMapper();

		    //read JSON like DOM Parser
		    JsonNode rootNode = objectMapper.readTree(selectedNumber);
		    return rootNode;
		}
		
		ArrayList<MdObject> metrics=listener.getMetrics();
		ArrayList<MdObject> attributes=listener.getAttributes();
		
		ArrayList<MdObject> filterAttribute=listener.getFilterAttribute();
		ArrayList<ArrayList<String>> filterValue=listener.getFilterValue();
		ArrayList<Boolean> filterPositive=listener.getFilterPositive();
		
		
		// Get display forms of attributes
		
		if (attributes.size()>0)
		{
			String links = "\""+attributes.get(0).getLink()+"\"";
			for (int index=1;index<attributes.size();index++)
			{
			  links+=",\""+attributes.get(index).getLink()+"\"";
			}
			
			logger.info("links: "+links);
			HttpPost postObjects = new HttpPost("/gdc/md/"+pid+"/objects/get");
			postObjects.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
			postObjects.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			//List<NameValuePair> params = new ArrayList<NameValuePair>();
		    //params.add(new BasicNameValuePair("objectUris", links));
		    //postObjects.setEntity(new UrlEncodedFormEntity(params));
			StringEntity entity = new StringEntity("{\"get\":{\"items\":["+links+"]}}");
			postObjects.setEntity(entity);
			
			HttpResponse postObjectsResponse; 
			    			
		    logger.info("get objects start");
			postObjectsResponse = client.execute(host,postObjects);
			logger.info("get objects end");
			   
			String jsonString=EntityUtils.toString(postObjectsResponse.getEntity());
			logger.info("objects: "+jsonString);
			
			//create ObjectMapper instance
		    ObjectMapper objectMapper = new ObjectMapper();

		    //read JSON like DOM Parser
		    JsonNode rootNode = objectMapper.readTree(jsonString); 
		    
		    for (JsonNode item : rootNode.path("objects").withArray("items"))
	    	{
		    	 JsonNode forms=item.get("attribute").get("content").get("displayForms");
		    	 for (int i=0;i<forms.size();i++)
		    	 {
		    		 if ((forms.get(i).get("content").has("default") && forms.get(i).get("content").get("default").asInt()==1)||(i==0))
		    		 {

		    			 String formOf=forms.get(i).get("content").get("formOf").asText();
		    			 String formUri=forms.get(i).get("meta").get("uri").asText();
		    			 displayForms.put(formOf, formUri);
		    		 }
		    	 }
	    	}
		     
		}
		
		// Get element links for attribute filters
		
		if (filterAttribute.size()>0)
		{
			// Get display forms of attribute filters
			
			String links = "\""+filterAttribute.get(0).getLink()+"\"";
			for (int index=1;index<filterAttribute.size();index++)
			{
			  links+=",\""+filterAttribute.get(index).getLink()+"\"";
			}
			
			
			logger.info("links: "+links);
			HttpPost postObjects = new HttpPost("/gdc/md/"+pid+"/objects/get");
			postObjects.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
			postObjects.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			StringEntity entity = new StringEntity("{\"get\":{\"items\":["+links+"]}}");
			postObjects.setEntity(entity);
			
			HttpResponse postObjectsResponse; 
			    			
		    logger.info("get objects start");
			postObjectsResponse = client.execute(host,postObjects);
			logger.info("get objects end");
			   
			String jsonString=EntityUtils.toString(postObjectsResponse.getEntity());
			logger.info("objects: "+jsonString);
			
			//create ObjectMapper instance
		    ObjectMapper objectMapper = new ObjectMapper();

		    //read JSON like DOM Parser
		    JsonNode rootNode = objectMapper.readTree(jsonString); 
		    
		    // Use labels resource to translate attribute values to links
		    
		    for (JsonNode item : rootNode.path("objects").withArray("items"))
	    	{
	    		 filterDisplayForms.add(item.findValue("uri").asText());
	    	}
		    
		    
		    String labelJson = "{ \"elementLabelToUri\": ["+
		    "{\n" + 
		    "      \"mode\": \"EXACT\",\n" + 
		    "      \"labelUri\": \""+filterDisplayForms.get(0)+"\",\n" + 
		    "      \"patterns\": [\n" + 
		    "        \""+filterValue.get(0).get(0)+"\"\n";
		    
		    for (int i=1;i<filterValue.get(0).size();i++)
		    {
		    	labelJson += ",       \""+filterValue.get(0).get(i)+"\"\n";
		    }
		    
		    labelJson += "      ]\n" + 
		    "    }";
		    for (int index=1;index<filterAttribute.size();index++)
			{
			  labelJson +=  ",{\n" + 
					    "      \"mode\": \"EXACT\",\n" + 
					    "      \"labelUri\": \""+filterDisplayForms.get(index)+"\",\n" + 
					    "      \"patterns\": [\n" + 
					    "        \""+filterValue.get(index).get(0)+"\"\n";
			  for (int i=1;i<filterValue.get(index).size();i++)
			  {
			    	labelJson += ",       \""+filterValue.get(index).get(i)+"\"\n";
			  }  
			  labelJson += "      ]\n" + 
			   		       "    }"; 
			}
		    labelJson +="]}";
		    logger.info(labelJson);
		    
		    HttpPost postLabels = new HttpPost("/gdc/md/"+pid+"/labels");
			postLabels.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
			postLabels.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
			StringEntity labelsEntity = new StringEntity(labelJson);
			postLabels.setEntity(labelsEntity);
			
			HttpResponse postLabelsResponse; 
			    			
		    logger.info("labels start");
			postLabelsResponse = client.execute(host,postLabels);
			logger.info("labels end");
			   
			jsonString=EntityUtils.toString(postLabelsResponse.getEntity());
			logger.info("labels: "+jsonString);

			if (postLabelsResponse.getStatusLine().getStatusCode()==200)
			{
			    //read JSON like DOM Parser
			    rootNode = objectMapper.readTree(jsonString); 
			   
			    for (JsonNode item : rootNode.withArray("elementLabelUri"))
		    	{
			    	ArrayList<String> values = new ArrayList<String>();
			    	
			    	for (JsonNode element: item.withArray("result"))
			    	{
			    	   JsonNode uri=element.findValue("uri");
			    	   if (uri==null)
			    	   {
			    	 	  elementException = new SQLException("Element \'"+(element.findValue("pattern").asText())+"\' not found.","22023",0);
			    	   }
			    	   else
			    	   {	   
		    		     values.add(uri.asText());
			    	   }
			    	}
			    	elementUri.add(values);
		    	}
			    logger.info("elementUri: "+elementUri.toString());
			    
			    if (elementUri.size()<filterAttribute.size())
			    {
			    	throw new SQLException("Some attribute element does not exist.","22023",0);
			    }
			}
			else
			{
				throw new SQLException("Attribute element does not exist.","22023",0);
			}
		}
			
/*
{"execution":{"afm":{"measures":[{"localIdentifier":"f70498c715b14c24ae14e4acb45fca48",
                                  "definition":{"measure":{"item":{"uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1284"}}},
                                  "alias":"Won"}]},
              "resultSpec":{"dimensions":[{"itemIdentifiers":["measureGroup"]},{"itemIdentifiers":[]}],"sorts":[]}}}	   
*/
		
	  // Assemble execute request
		
	  String jsonString = "{\"execution\":{\"afm\":{";
	
	  if (metrics.size()>0)
	  {
	    jsonString += "\"measures\":[";
	    
	    for (int index=0;index<metrics.size();index++)
	    {
	      if (index>0) jsonString+=",";
	      jsonString += "{\"localIdentifier\":\"M"+(index+1)+"\",\n" + 
		  	"  \"definition\":{\"measure\":{\"item\":{\"uri\":\""+metrics.get(index).getLink()+"\"}";
	      
	      if (metrics.get(index).getAggregation()!=null)
	      {
	    	  jsonString += ", \"aggregation\": \""+(metrics.get(index).getAggregation())+"\"";
	      }
	      
	      jsonString += "}},\n"; 
	      
	      String alias=metrics.get(index).getAlias();
	      if ((alias==null)&&(metrics.get(index).getAggregation()!=null))
	      {	  
	    	  String aggrLabel = metrics.get(index).getAggregation().substring(0, 1).toUpperCase()+metrics.get(index).getAggregation().substring(1).toLowerCase();
	    	  switch (aggrLabel)
	    	  {
	    	      case "Avg": aggrLabel= "Average";break;
	    	      case "Min": aggrLabel= "Minimum";break;
	    	      case "Max": aggrLabel= "Maximum";break;
	    	  }
	    	  alias=aggrLabel+" of "+metrics.get(index).getTitle();
	      }
	      else
	      {	  
	          if (alias==null) alias=metrics.get(index).getTitle();
	      }	  
    	  jsonString += "  \"alias\":\""+alias+"\"}";	
	      
	    }
	    // measure {item: {uri: "/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1144"}, aggregation: "sum"}
	    jsonString += "]";			
	  }
	  
	  if (attributes.size()>0)
	  {
		if (metrics.size()>0) jsonString+=",";
	    jsonString += "\"attributes\":[\n"; 
	    for (int index=0;index<attributes.size();index++)
	    {
	      if (index>0) jsonString+=",";
	      String alias=attributes.get(index).getAlias();
	      if (alias==null) alias=attributes.get(index).getTitle();
	      jsonString += "{\"displayForm\":{\"uri\":\""+displayForms.get(attributes.get(index).getLink())+"\"},\"alias\":\""+alias+"\",\"localIdentifier\":\"A"+(index+1)+"\"}\n";
	    }
	    jsonString += "]";
	  }
	  
	  if (filterAttribute.size()>0)
	  {
		jsonString+=",\"filters\":[\n"; 
		
		boolean firstFilter=true;
	    for (int index=0;index<filterAttribute.size();index++)
	    { 
	      if (!firstFilter) 
	      {
	    	  jsonString+=",";
	      }
	      firstFilter=false;
	      
	      if (elementUri.get(index).size()>0)
	      {	  
		     
		      jsonString += "{\""+(filterPositive.get(index)?"positive":"negative")+"AttributeFilter\":{ \"displayForm\": {\"uri\":\""+filterDisplayForms.get(index)+"\"},"
		      		+ "\""+(filterPositive.get(index)?"in":"notIn")+"\":[\""+elementUri.get(index).get(0)+"\"";
		      for (int i=1;i<elementUri.get(index).size();i++)
			  {
		    	  jsonString += ",\""+elementUri.get(index).get(i)+"\"";
			  }        
		      jsonString += "]}}\n";
	      }
	      else
	      {
	    	 if (filterPositive.get(index)) throw elementException;
	      }
	    }
	    
	    jsonString += "]";
	  }
	  
	  jsonString += "},\n";
	  jsonString +=	"\"resultSpec\":{\"dimensions\":[{\"itemIdentifiers\":[";
	  
	  boolean isFirst=true;
	  ArrayList<Boolean> isTop=listener.getIsTop();
	  
	  for (int index=0;index<attributes.size();index++)
	  {
		  if (!isTop.get(index).booleanValue())
		  {	  
	        if (!isFirst) jsonString+=",";
	        isFirst=false;
	        jsonString += "\"A"+(index+1)+"\"";
		  }
	  }
	  
	  
	  jsonString += "]}";
	  
	  jsonString += ",{\"itemIdentifiers\":[";

	  isFirst=true;
		  
	  for (int index=0;index<attributes.size();index++)
	  {
			  if (isTop.get(index).booleanValue())
			  {	  
		        if (!isFirst) jsonString+=",";
		        isFirst=false;
		        jsonString += "\"A"+(index+1)+"\"";
			  }
	  }
	  if (!isFirst) jsonString+=",";
	  
	  if (metrics.size()>0)
      {
		  jsonString += "\"measureGroup\"";
		  isFirst=false;
      }
	  
	  jsonString += "]}"; 
	  
	  jsonString += "],\"sorts\":[";
	  
	  ArrayList<String> sortIds = listener.getSortIds();
	  ArrayList<String> sortDir = listener.getSortDir();
	  
	  for (int index=0;index<sortIds.size();index++)
	  {
		  if (index>0) jsonString+=",";
		  if (sortIds.get(index).startsWith("M"))
		  {
			  // Measure
			  jsonString += "{\"measureSortItem\":{\"direction\":\""+sortDir.get(index)+"\",\"locators\":[{\"measureLocatorItem\":{\"measureIdentifier\":\""+sortIds.get(index)+"\"}}]}}";
		  }
		  else
		  {
			  // Attribute
			  jsonString += "{\"attributeSortItem\":{\"direction\":\""+sortDir.get(index)+"\",\"attributeIdentifier\":\""+sortIds.get(index)+"\"}}";
		  }
	  }
	  
	  jsonString +=	 "]"
	  		+ "}}}\n";
	  
	  // "sorts":[{"measureSortItem":{"direction":"desc","locators":[{"measureLocatorItem":{"measureIdentifier":"267baae283f049658cc9edbcdb15ca0c"}}]}}]
	  // "sorts":[{"attributeSortItem":{"direction":"asc","attributeIdentifier":"49e8d427adfb4c319da6e7235c37519c"}}]
	  
	  
	  logger.info(jsonString);
	  
	  StringEntity entity = new StringEntity(jsonString);

	  // Execute query
	  
	  HttpPost postExecute = new HttpPost("/gdc/app/projects/"+pid+"/executeAfm");
	  postExecute.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
	  postExecute.setHeader("Content-type", ContentType.APPLICATION_JSON.getMimeType());
	  postExecute.setEntity(entity);
	    
	  HttpResponse postExecuteResponse; 
	    
	  logger.info("execute start");
	  postExecuteResponse = client.execute(host,postExecute);
	  logger.info("execute end");
			
      jsonString=EntityUtils.toString(postExecuteResponse.getEntity());
    	 
      logger.info("response: "+jsonString);
  
      /*
 {"executionResponse":{"dimensions":[{"headers":[{"attributeHeader":{"name":"Region","localIdentifier":"A1","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1024","identifier":"label.owner.region","formOf":{"name":"Region","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1023","identifier":"attr.owner.region"}}},{"attributeHeader":{"name":"Owner Name","localIdentifier":"A2","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1028","identifier":"label.owner.id.name","formOf":{"name":"Sales Rep","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1025","identifier":"attr.owner.id"}}},{"attributeHeader":{"name":"Department","localIdentifier":"A3","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1027","identifier":"label.owner.department","formOf":{"name":"Department","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1026","identifier":"attr.owner.department"}}},{"attributeHeader":{"name":"Status","localIdentifier":"A4","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1094","identifier":"label.stage.status","formOf":{"name":"Status","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1093","identifier":"attr.stage.status"}}}]},
                                     {"headers":[{"measureGroupHeader":{"items":[{"measureHeaderItem":{"name":"Won","format":"$#,##0.00","localIdentifier":"M1","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1284","identifier":"afSEwRwdbMeQ"}},{"measureHeaderItem":{"name":"Lost","format":"$#,##0.00","localIdentifier":"M2","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/1283","identifier":"af2Ewj9Re2vK"}},{"measureHeaderItem":{"name":"Win Rate","format":"#,##0.0%","localIdentifier":"M3","uri":"/gdc/md/wv9vyd03yqfqyjc2301p293cceek2xtr/obj/5379","identifier":"aaX0PIUzg7nF"}}]}}]}],"links":{"executionResult":"/gdc/app/projects/wv9vyd03yqfqyjc2301p293cceek2xtr/executionResults/5626656934479199232?q=eAGtk11PwjAUhv9KU%2FRucWMDZSTGGz9CYrxAjReEi7IeYNitoz0TF8N%2F9wxwggkJwpLdtMs5fc7z%0Atl%2FcQKYNPokEeJe%2FphijAskdHmmVJ6nl3QFPAE0cPRidZ3zobJblny8%2B1iYRSJVnDafR8C48j0pt%0AniTCFLT7MgVGK6bHTCjFdFaelacxxmAZTgUyYYCNNE5ZpLQFyUQq2UKn1GVFQj3eVqs1Q0%2FShjuR%0AkZtId%2FERfhTSC4r5eF7MIj%2FwmpkfBlEE8O5%2FonH1aOY2%2FU6LL52aSDeQoxxZqvEP6KO2SNwnkAa7%0ApBul5ztKt83EKesLhBMObQdXIV9SqEYv1okKpLBpQHgpbwLp7sMkXkXwGysdKGObKVHcU%2F5HhOL5%0ALeoxEhbuFCSQ4mu%2F9%2B9kPT9wYV1ub2J5vWWmYq6mOQoyKFtqFKpUMxg6q29Y3qaq74%2BlZ6HoSvch%0Ao5JaRXXqENXeJ2obu5rpKFftw13dQiYMlrHXLeuqDlmX%2B2TtcJ9m6%2FJwW88oMLc1mwrreH%2Fh3vdX%0AMZ9kKdzz%2FobLb2p1Ny8%3D%0A&c=3a0a5e15bd8edd59f1ea326b2e3d5975&offset=0%2C0&limit=1000%2C1000&dimensions=2&totals=0%2C0"}}}
       */
      
      //create ObjectMapper instance
      ObjectMapper objectMapper = new ObjectMapper();

      //read JSON like DOM Parser
      JsonNode rootNode = objectMapper.readTree(jsonString); 
      logger.info("status code: "+postExecuteResponse.getStatusLine().getStatusCode());
      String message="";
	    if (postExecuteResponse.getStatusLine().getStatusCode()!=201)
	    {    	
		    JsonNode error = rootNode.findValue("message");
		    if (error!=null) message=error.asText();
		    throw new SQLException("Cannot execute. "+message,"07000",postExecuteResponse.getStatusLine().getStatusCode());
	    }      
      return rootNode;
	}


	
	public JsonNode getResults(String url) throws SQLException, JsonProcessingException, IOException
	{
		String jsonString="";
		
		try {
		HttpGet getResultsRequest = new HttpGet(url);
		getResultsRequest.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
    	HttpResponse getResultsResponse;
    	
		   	
	    logger.info("check response");
		
			getResultsResponse = client.execute(host,getResultsRequest);
			logger.info("returned "+getResultsResponse.getStatusLine().getStatusCode());
			jsonString=EntityUtils.toString(getResultsResponse.getEntity());
			
			while (getResultsResponse.getStatusLine().getStatusCode()==202)
			{
				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				logger.info("retry");
				
				getResultsRequest = new HttpGet(url);
				getResultsRequest.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
		    
				try {
					getResultsResponse = client.execute(host,getResultsRequest);
					logger.info("returned "+getResultsResponse.getStatusLine().getStatusCode());	
					jsonString=EntityUtils.toString(getResultsResponse.getEntity());		
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
	    String message="";
	    if (getResultsResponse.getStatusLine().getStatusCode()!=200)
	    {
	    	//create ObjectMapper instance
		    ObjectMapper objectMapper = new ObjectMapper();

		    //read JSON like DOM Parser
		    JsonNode rootNode = objectMapper.readTree(jsonString); 
		    JsonNode error = rootNode.findValue("message");
		    if (error!=null) message=error.asText();
	    }
		
		switch (getResultsResponse.getStatusLine().getStatusCode())
		{
		   case 200: break;
		   case 400: throw new SQLException("Bad request. "+message,"07000",400);
		   case 401: throw new SQLException("Unauthorized. "+message,"2F004",401);
		   case 403: throw new SQLException("Forbidden. "+message,"2F004",403); 
		   case 404: throw new SQLException("No data. "+message,"0",404); 
		   case 429: throw new SQLException("Too many data. "+message,"3B002",429); 
		   case 413: throw new SQLException("Bad query. "+message,"07000",413); 
		   case 503: throw new SQLException("Service not available. "+message,"08006",503); 
		   default: throw new SQLException("Result error ("+(getResultsResponse.getStatusLine().getStatusCode())+"). "+message,"07000",getResultsResponse.getStatusLine().getStatusCode()); 
		}
			
		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			
		}
		//create ObjectMapper instance
	    ObjectMapper objectMapper = new ObjectMapper();

	    //read JSON like DOM Parser
	    JsonNode rootNode = objectMapper.readTree(jsonString);
	    return rootNode;
	}
	
	
	
	
	
	
	
	
}
	

