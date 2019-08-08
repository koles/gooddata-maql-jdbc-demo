package com.gooddata.maql.jdbc.driver;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;


class StatementLoggingInvocationHandler implements InvocationHandler {
	  private final static Logger logger = Logger.getGlobal();//Logger.getLogger("invocation handler");
	  
	  private final Object delegate;
		  public StatementLoggingInvocationHandler(final Object delegate) {
		    this.delegate = delegate;
		  }
		  @Override
		public  Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		    logger.info("method: " + method + ", args: " + args);
		    return method.invoke(delegate, args);
		  }
		}

public class Connection implements java.sql.Connection {

	private Driver driver;
	private String url;
	private Properties properties;
	private String user;
	private String password;
	private String server;
	private String pid;
	private boolean autoCommit=false;
	GoodDataLiteClient gd;
	   
	private final static Logger logger = Logger.getGlobal();//Logger.getLogger(Connection.class.getName());

	
	public Connection()  {		
    	logger.info("jdbc4maql: connection constructor default"); 	
	}
	
	public Connection(final Driver driver, final String url,
			final Properties properties) throws SQLException,ClientProtocolException, IOException  {
		
		this.properties = properties;
		this.driver = driver;
		this.url = url;
    	logger.info("jdbc4maql: connection constructor url:"+url);
    	
    	this.user=properties.getProperty("user").toString();
    	this.password=properties.getProperty("password").toString();
    	
    	logger.info("jdbc4maql: user:"+user);
    	
    	String [] params=url.replace("jdbc:maql://", "").split("/gdc/projects/");
    	
    	if (params.length!=2) throw new SQLException("Wrong URL format",null,0);
    	
    	this.pid = params[1]; //new String("wv9vyd03yqfqyjc2301p293cceek2xtr");
    	this.server = params[0]; //new String("secure.gooddata.com");
    	logger.info("jdbc4maql: pid:"+pid);
    	logger.info("jdbc4maql: server:"+server);
        	
    	logger.info("gd init begin");
    	gd = new GoodDataLiteClient(user,password,server,pid);
    	logger.info("readCatalog");
    	gd.readCatalog();
    	logger.info("gd init end");
        	 
	}
	
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection unwrap");
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection iswrapperfor");
		return false;
	}

	@Override
	public java.sql.Statement createStatement() throws SQLException {
		logger.info("jdbc4maql: connection createstatement");
				
		Statement s = new Statement(gd);
		
		java.sql.Statement loggings = (java.sql.Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                 new Class[] {java.sql.Statement.class},
                 new StatementLoggingInvocationHandler (s));
		
		return loggings;
	}
	

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException,SQLFeatureNotSupportedException {
    	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		logger.info("jdbc4maql: connection nativesql");
		return new String("");
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	   	this.autoCommit=autoCommit;
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		return autoCommit;
	}

	@Override
	public void commit() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public void rollback() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public void close() throws SQLException {		
	   	logger.info("jdbc4maql: connection method");
	   	
	}

	@Override
	public boolean isClosed() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		return false;
	}

	@Override
	public java.sql.DatabaseMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
	   	logger.info("jdbc4maql: connection method");
		return new DatabaseMetaData(gd);
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public boolean isReadOnly() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		return true;
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public String getCatalog() throws SQLException {
	   	logger.info("jdbc4maql: connection getCatalog");
		return null;
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		return 0;
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException,SQLFeatureNotSupportedException {
		logger.info("jdbc4maql: connection createstatement2");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException,SQLFeatureNotSupportedException {
		
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection getTypeMap");

		return null;
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setHoldability(int holdability) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getHoldability() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob createClob() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob createBlob() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob createNClob() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection isValid");

		return true;
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		// TODO Auto-generated method stub
	   	logger.info("jdbc4maql: connection method");

	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		// TODO Auto-generated method stub
	   	logger.info("jdbc4maql: connection method");

	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection getClientInfo");

		return null;
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		// TODO Auto-generated method stub
		logger.info("jdbc4maql: connection getClientInfo");
		return null;
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException,SQLFeatureNotSupportedException {
	   	logger.info("jdbc4maql: connection method");
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setSchema(String schema) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
	}

	@Override
	public String getSchema() throws SQLException {
		logger.info("jdbc4maql: connection getSchema");
		return null;
	}

	@Override
	public void abort(Executor executor) throws SQLException {
	   	logger.info("jdbc4maql: connection method");
		close();

	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		// TODO Auto-generated method stub
	   	logger.info("jdbc4maql: connection method");

	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		// TODO Auto-generated method stub
	   	logger.info("jdbc4maql: connection method");
		return 0;
	}

}
