package com.gooddata.maql.jdbc.driver;


import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.http.client.ClientProtocolException;



public class Driver implements java.sql.Driver {

	public static void main(final String[] args)  {
		
		final Driver driver = new Driver();
		System.out.println("JDBC 4 MAQL Driver "+driver.getMajorVersion()+"."+driver.getMinorVersion());
		System.out.println("URL Format:  jdbc:maql://secure.gooddata.com/gdc/projects/{pid}");
		System.out.println();
		
		//-begin test
		/*
		logger.info("test");
		
		Properties props=new Properties();
		props.setProperty("user", "jakub.sterba+maql@gooddata.com");
		props.setProperty("password", "jdbc4maql");
		
		try {
			Connection connection= (Connection) driver.connect("jdbc:maql://secure.gooddata.com/gdc/projects/wv9vyd03yqfqyjc2301p293cceek2xtr", props);
			java.sql.Statement statement = connection.createStatement();
			//statement.execute("SELECT Won where Region not in ('West Coast','East Coast') and Department not in ('Direct Sales')");
			//statement.execute("SELECT Department, Won, Lost, \"# of Activities\" from data where Region='West Coast' and Department!='Direct Sales'");
			//statement.execute("select Account,\"Sales Rep\",Region,Department,Status,Won,Lost,\"Win Rate\"");
			//statement.execute("select \"Date (Created)\",Won");
		    //statement.execute("select \"Sales Rep\",sum(Amount), Amount, count(Region)");
			//statement.execute("select \"Sales Rep\", Won order by Won desc");
			//statement.execute("select Region on rows,Department on columns,Won,Lost");
			//statement.execute("select {attr.owner.id},Won");
			//statement.execute("select * \n\rFROM\n\r(\n\rselect Region, Won) x limit 1000");
			statement.execute("SELECT SUM(\"DATA\".\"Won\") AS \"sum_Won_ok\"\n" + 
					"FROM \"DATA\"\n" + 
					"HAVING (COUNT(1) > 0)"); 
			statement.getResultSet();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//-end test
		
	}

	private final static Logger logger = Logger.getGlobal();//.getLogger(Driver.class.getName());
	
	 static {		 
	        try {
	        	logger.info("MAQL JDBC Driver started");
	            DriverManager.registerDriver(new Driver());
	        } catch (SQLException e) {
	            throw new RuntimeException(e);
	        }
	    }
	 
    public Driver() {
/*
	    FileHandler fh;  

	    try {  

	        // This block configure the logger with handler and formatter  
	        fh = new FileHandler("/Users/Jakub/jdbc4maql.log");  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
	        logger.info("pokus");


	    } catch (SecurityException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }  
	    */
    }	 

	
	@Override
	public java.sql.Connection connect(String url, Properties info) throws SQLException {
		if (url.startsWith("jdbc:maql:")) {
        	if (info.getProperty("debug", "false").equals("true"))
        	{
        	    logger.setLevel(Level.INFO);
        	}
        	else
        	{
        		logger.setLevel(Level.WARNING);
        	}

        	logger.info("jdbc4maql: connect url:"+url);

			try {
				return new Connection(this,new String(url), info);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.info("jdbc4maql: returning null");
		return null;
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {		
    	logger.info("jdbc4maql: acceptsURL url:"+url);
		return url.startsWith("jdbc:maql:");
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		logger.info("jdbc4maql: getPropertyInfo");
		return new DriverPropertyInfo[0];
	}

	@Override
	public int getMajorVersion() {
		logger.info("jdbc4maql: getMajorVersion");
		return 0;
	}

	@Override
	public int getMinorVersion() {
		logger.info("jdbc4maql: getMinorVersion");
		
		return 5;
	}

	@Override
	public boolean jdbcCompliant() {
		logger.info("jdbc4maql: jdbcCompliant");
		
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		logger.info("jdbc4maql: getParentLogger");
		
		throw new SQLFeatureNotSupportedException();
	}

}
