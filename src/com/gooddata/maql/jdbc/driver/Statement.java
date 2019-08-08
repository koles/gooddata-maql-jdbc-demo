package com.gooddata.maql.jdbc.driver;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.http.client.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;


class LoggingInvocationHandler implements InvocationHandler {
  private final static Logger logger = Logger.getGlobal();//Logger.getLogger("invocation handler");
  
  private final Object delegate;
	  public LoggingInvocationHandler(final Object delegate) {
	    this.delegate = delegate;
	  }
	  @Override
	public  Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    logger.info("method: " + method + ", args: " + args);
	    return method.invoke(delegate, args);
	  }
	}


public class Statement implements java.sql.Statement {
	private final static Logger logger = Logger.getGlobal();//Logger.getLogger(Statement.class.getName());

	private GoodDataLiteClient gd;
    private String sql;
    private JsonNode executeResponse=null;
    private int maxRows=0;
    private boolean hasData=false;
    private boolean isClosedValue=false;

	public Statement(GoodDataLiteClient gd)
	{
		logger.info("jdbc4maql: statement constructor");
		this.gd=gd;		
	}
	
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement unwrap");

		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public java.sql.ResultSet executeQuery(String sql) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement executeQuery sql:"+sql);
        execute(sql);
		return getResultSet();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub
        isClosedValue=true;
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		
        logger.info("setMaxRows "+max);
        maxRows=max;
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancel() throws SQLException {
		// TODO Auto-generated method stub

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
	public void setCursorName(String name) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean execute(String sql) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement execute sql:"+sql);
		
		this.sql=sql;
		
		//if (sql.matches("(?i)[ \t\n]*select.*"))
		//{	
			try {
				executeResponse=gd.execute(sql);
				hasData=(executeResponse!=null)/*&&(executeResponse.has("executionResponse"))*/;
				logger.info("has data: "+hasData);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.info("exception");
				e.printStackTrace();
			}
		/*}
		else
		{
			throw new SQLException("SQLValidation failure",null,0);
		}*/

        //throw new SQLException("No data","02000",404);
		return true;
	}

	@Override
	public java.sql.ResultSet getResultSet() throws SQLException {
		
        logger.info("jdbc4maql: statement getResultSet");		
        
        if ((executeResponse!=null)&&( executeResponse.has("headers")))
        {
        	JsonNode responseHeaders = executeResponse.get("headers");
        	JsonNode responseIsNumber = executeResponse.get("isnumber");
        	
        	String [][] table=new String[0][0];
        	String [] header=new String[responseHeaders.size()];
            boolean [] isNumeric=new boolean[responseIsNumber.size()];       
        	
        	for (int i=0;i<responseHeaders.size();i++)
        	{
        		header[i]=responseHeaders.get(i).asText();
        		isNumeric[i]=responseIsNumber.get(i).asBoolean();
        	}
        	
            
            hasData=false;
            return new ResultSetTable(table,header,isNumeric);
        }
        
        if ((executeResponse!=null)&&(! executeResponse.has("executionResponse")))
        {
        	String selectedNumber = executeResponse.asText();
        	String [][] table=new String[][]{{selectedNumber}};
            String [] header=new String[] {selectedNumber};
            boolean [] isNumeric=new boolean[] {true};
            hasData=false;
            return new ResultSetTable(table,header,isNumeric);
        }
			
		ResultSet rs = new ResultSet(gd, executeResponse, maxRows);
		
		java.sql.ResultSet loggingrs =
				(java.sql.ResultSet) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                 new Class[] {java.sql.ResultSet.class},
                 new LoggingInvocationHandler (rs));
		hasData=false;
		return loggingrs;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement getUpdateCount");

		return -1;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement getMoreResults");

		return hasData;
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
	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int[] executeBatch() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement getConnection");

		return null;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement getMoreResults2");

		return false;
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement execute2 sql:"+sql);
        execute(sql);
		return false;
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		logger.info("jdbc4maql: statement execute3 sql:"+sql);
		// TODO Auto-generated method stub
        execute(sql);
		return false;
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: statement execute4 sql:"+sql);
		execute(sql);
		return false;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return isClosedValue;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
