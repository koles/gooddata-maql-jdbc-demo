package com.gooddata.maql.jdbc.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

class ResultSetLoggingInvocationHandler implements InvocationHandler {
	  private final static Logger logger = Logger.getGlobal();//Logger.getLogger("invocation handler");
	  
	  private final Object delegate;
		  public ResultSetLoggingInvocationHandler(final Object delegate) {
		    this.delegate = delegate;
		  }
		  @Override
		public  Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		    logger.info("method: " + method + ", args: " + args);
		    return method.invoke(delegate, args);
		  }
		}

public class ResultSet implements java.sql.ResultSet {
	private JsonNode executeResponse;
	private JsonNode rootNode;
	private ArrayNode data;
	private ArrayNode attributeItems;
	private String responseUrl;
	private String [] columns;
	private boolean [] isNumeric;
	private int position=-1;
	private int attributeColumns=0;
	private int totalRows;
	private int totalColumns;
	private int offsetRows;
	private int countRows;
	private int maxRows;
	private final static Logger logger = Logger.getGlobal();//Logger.getLogger(ResultSetTable.class.getName());
    private boolean wasnull=false;
	private GoodDataLiteClient gd;
	
	public ResultSet( GoodDataLiteClient gd, JsonNode executeResponse, int maxRows) throws SQLException {
		
		this.gd=gd;
		this.executeResponse = executeResponse;
		this.maxRows=maxRows;
		this.responseUrl=new String(executeResponse.path("executionResponse").path("links").path("executionResult").asText());
	    logger.info(responseUrl);   
	      
		
		getResults(responseUrl);
		prepareHeaders();
		refreshBuffer();
					
	}
	
	public void prepareHeaders() throws SQLException
	{
	 	  	      
	      ArrayList<String> headers = new ArrayList<String>();
	      JsonNode attributeHeader = executeResponse.get("executionResponse").get("dimensions").get(0).get("headers");
	      
	      for(int i=0;i<attributeHeader.size();i++)
	      {
	    	  headers.add(attributeHeader.get(i).get("attributeHeader").get("formOf").get("name").asText());
	      }
	      /*
	      if (executeResponse.get("executionResponse").get("dimensions").size()>1)
	      {
		      JsonNode measureHeader = executeResponse.get("executionResponse").get("dimensions").get(1).get("headers").get(0).get("measureGroupHeader").get("items");
		      
		      for(int i=0;i<measureHeader.size();i++)
		      {
		    	  headers.add(measureHeader.get(i).get("measureHeaderItem").get("name").asText());
		      }
	      }
	      */
	      if (executeResponse.get("executionResponse").get("dimensions").size()>1)
	      {
	        JsonNode topNode = rootNode.get("executionResult").withArray("headerItems").get(1);
	        int depth = topNode.size();
	        if (depth>0)
	        {
		        int topColumns = topNode.get(0).size();
		        for (int index=0;index<topColumns;index++)
		        {
		        	String label=topNode.get(depth-1).get(index).findValue("name").asText();
		        	if (depth>1)
		        	{
		        		label+=" (";
		        		for (int i=0;i<depth-1;i++) 
		        		{
		        			if (i>0) label+=",";
		        			label+=topNode.get(i).get(index).findValue("name").asText();
		        		}
		        		label+=")";
		        	}
		        	headers.add(label);
		        }
	        }
	      }
	      
	      logger.info("headers size: "+headers.size());
	      
	      columns=new String[ headers.size() ];      
	      headers.toArray(columns);
	      
	      isNumeric=new boolean[ headers.size() ];
	      for (int index=0;index<headers.size();index++)
	      {
	         if (index<attributeHeader.size())
	         {
	        	 isNumeric[index]=false;
	         }
	         else
	         {
	        	 isNumeric[index]=true;
	         }
	         
	      }
	}
	
	void refreshBuffer() throws SQLException
	{
		if (((position>=(this.countRows+this.offsetRows))||(position<this.offsetRows)) &&
		  (position>=0) && (position<totalRows))
		{
			this.offsetRows=position;
			logger.info("refresh offset: "+this.offsetRows);
			// &offset=0%2C0&limit=1000%2C1000&dimensions=2&totals=0%2C0
			String[] urlArray = responseUrl.split("&offset");
			String url = urlArray[0] + "&offset="+this.offsetRows+"%2C0&limit=1000%2C1000&dimensions=2&totals=0%2C0";
			logger.info("refresh url: "+url);
			getResults(url);
		}
		else
		{
			logger.info("no refresh needed");
		}
	}
	
	void getResults(String responseUrl) throws SQLException
	{
		try {
			this.rootNode = gd.getResults(responseUrl);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	   	//jsonString="{\"executionResult\":{\"data\":[[\"4742\",\"10009\",\"8748\"],[null,null,\"118360\"]],
		//\"paging\":{\"count\":[2,3],\"offset\":[0,0],\"total\":[2,3]},\"headerItems\":[[[{\"measureHeaderItem\":{\"name\":\"KD Users\",\"order\":0}},{\"measureHeaderItem\":{\"name\":\"Sum of SDK Request\",\"order\":1}}]],[[{\"attributeHeaderItem\":{\"name\":\"2016\",\"uri\":\"/gdc/md/budtwmhq7k94ve7rqj49j3620rzsm3u1/obj/914/elements?id=2016\"}},{\"attributeHeaderItem\":{\"name\":\"2017\",\"uri\":\"/gdc/md/budtwmhq7k94ve7rqj49j3620rzsm3u1/obj/914/elements?id=2017\"}},{\"attributeHeaderItem\":{\"name\":\"2018\",\"uri\":\"/gdc/md/budtwmhq7k94ve7rqj49j3620rzsm3u1/obj/914/elements?id=2018\"}}]]]}}";

		this.data = (ArrayNode) rootNode.get("executionResult").withArray("data");
		
		if (rootNode.get("executionResult").has("headerItems"))
		{
		  this.attributeColumns = rootNode.get("executionResult").withArray("headerItems").get(0).size();
		  this.attributeItems = (ArrayNode) rootNode.get("executionResult").withArray("headerItems").get(0);
		}
		else
		{
		  this.attributeColumns = 0;
		  this.attributeItems = null;
		}

		/*
		  "paging" : {
	      "count" : [ 100, 1 ],
	      "offset" : [ 0, 0 ],
	      "total" : [ 691, 1 ]
	      }
		 */	
		
		this.totalRows = rootNode.get("executionResult").get("paging").get("total").get(0).asInt();
		
		if ((this.totalRows>this.maxRows)&&(this.maxRows>0)) this.totalRows=this.maxRows;
		
		this.totalColumns = (rootNode.get("executionResult").get("paging").get("total").size()==2)?
				             rootNode.get("executionResult").get("paging").get("total").get(1).asInt():0;
	    this.offsetRows = rootNode.get("executionResult").get("paging").get("offset").get(0).asInt();
	    this.countRows = rootNode.get("executionResult").get("paging").get("count").get(0).asInt();
		logger.info("totalRows:"+totalRows+" totalColumns:"+totalColumns+" offsetRows:"+offsetRows+" countRows:"+countRows);
		
	}


	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean next() throws SQLException {
		logger.info("jdbc4maql: resultsettable next position:"+position);

		return relative(1);
	}

	@Override
	public void close() throws SQLException {
	}

	@Override
	public boolean wasNull() throws SQLException {
		logger.info("was null:"+wasnull);
		return wasnull;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		if ((position<0) || (position >= totalRows)) return null;
		
		if (columnIndex>attributeColumns)
		{	
		
			logger.info("jdbc4maql: resultsettable getString :"+data.get(position-offsetRows).get(columnIndex-1-attributeColumns).asText());
			if (data.get(position-offsetRows).get(columnIndex-1-attributeColumns).isNull())
			{
				wasnull=true;
				logger.info("null");
				return null;
			}
			logger.info("not null");
			wasnull=false;
			return data.get(position-offsetRows).get(columnIndex-1-attributeColumns).asText();
		}
		else
		{
			logger.info("jdbc4maql: resultsettable getString :"+attributeItems.get(columnIndex-1).get(position-offsetRows).get("attributeHeaderItem").get("name").asText());
			if (attributeItems.get(columnIndex-1).get(position-offsetRows).get("attributeHeaderItem").get("name").isNull())
			{
				wasnull=true;
				logger.info("null");
				return null;
			}
			logger.info("not null");
			wasnull=false;
			return attributeItems.get(columnIndex-1).get(position-offsetRows).get("attributeHeaderItem").get("name").asText();
		}
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return false;
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return 0;
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return getBigDecimal(columnIndex);
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		
		return getBoolean(findColumn(columnLabel));
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return getByte(findColumn(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return getShort(findColumn(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return getInt(findColumn(columnLabel));
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return getLong(findColumn(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return getFloat(findColumn(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return getDouble(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return getBytes(findColumn(columnLabel));
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return getDate(findColumn(columnLabel));
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return getTime(findColumn(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return getTimestamp(findColumn(columnLabel));
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return getAsciiStream(findColumn(columnLabel));
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return getUnicodeStream(findColumn(columnLabel));
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return getBinaryStream(findColumn(columnLabel));
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCursorName() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.sql.ResultSetMetaData getMetaData() throws SQLException {
		
		logger.info("jdbc4maql: getMetaData");
		logger.info("columns: "+columns.toString()+" isNumeric:"+isNumeric.toString());
		
		//return new ResultSetTableMetaData(columns,isNumeric);
		
		ResultSetTableMetaData rs = new ResultSetTableMetaData(columns,isNumeric);
		
		java.sql.ResultSetMetaData loggingrs =
				(java.sql.ResultSetMetaData) Proxy.newProxyInstance(ResultSetTableMetaData.class.getClassLoader(),
                 new Class[] {java.sql.ResultSetMetaData.class},
                 new ResultSetLoggingInvocationHandler (rs));
		
		return loggingrs;
		
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
        return getString(columnIndex);
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		for (int i=0;i<columns.length;i++) if (columns[i].equals(columnLabel)) return i;
		return 0;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		if ((position<0) || (position >= totalRows)) return null;
		
		if (columnIndex>attributeColumns)
		{	

			logger.info("jdbc4maql: resultsettable getBigDecimal: "+data.get(position-offsetRows).get(columnIndex-1-attributeColumns).asText());
			if (! data.get(position-offsetRows).get(columnIndex-1-attributeColumns).isNull())
			{	
				logger.info("not null");
			   wasnull=false;
			   return new BigDecimal(data.get(position-offsetRows).get(columnIndex-1-attributeColumns).asText());
			}
			else
			{
				logger.info("is null");
				wasnull=true;
				return null;
			}
		}
		else 
		{
			return null;
		}
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return position < 0;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return position >= totalRows;
	}

	@Override
	public boolean isFirst() throws SQLException {

		return position==0;
	}

	@Override
	public boolean isLast() throws SQLException {
		
		return position == (totalRows - 1);
	}

	@Override
	public void beforeFirst() throws SQLException {
		position = -1;
	}

	@Override
	public void afterLast() throws SQLException {
		position = totalRows;

	}

	@Override
	public boolean first() throws SQLException {
		position = 0;
		refreshBuffer();
		return totalRows>0;
	}

	@Override
	public boolean last() throws SQLException {
		position = totalRows-1;
		refreshBuffer();
		return totalRows>0;
	}

	@Override
	public int getRow() throws SQLException {
		return position;
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		position = row;
		refreshBuffer();
		return (row>=0) && (row<totalRows);
	}

	@Override
	public boolean relative(int rowsIncrement) throws SQLException {
		position += rowsIncrement;
		refreshBuffer();
		return (position>=0) && (position<totalRows);
	}

	@Override
	public boolean previous() throws SQLException {
		return relative(-1);
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getType() throws SQLException {
		// TODO Auto-generated method stub
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void insertRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveToInsertRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public Statement getStatement() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
