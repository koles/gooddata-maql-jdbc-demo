package com.gooddata.maql.jdbc.driver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class AfmCreatorListener extends MaqlLiteBaseListener {	
	private ArrayList<MdObject> catalog;

	ArrayList<MdObject> metrics = new ArrayList<MdObject>();
	ArrayList<MdObject> attributes = new ArrayList<MdObject>();
	ArrayList<String> sortIds = new ArrayList<String>();
	ArrayList<String> sortDir = new ArrayList<String>();
	private final static Logger logger = Logger.getGlobal();//Logger.getLogger(Connection.class.getName());
	String error=null;
	ArrayList<MdObject> filterAttribute = new ArrayList<MdObject>();
	ArrayList<ArrayList<String>> filterValue = new ArrayList<ArrayList<String>>();
	ArrayList<Boolean> filterPositive = new ArrayList<Boolean>();
	ArrayList<Boolean> isTop=new ArrayList<Boolean>();
	ArrayList<String> list=null;
	String aggregation = null;
	MdObject lastObj = null;
	private boolean isNumber = false;
	private String selectedNumber = null;
	private boolean isAllColumns = false;
	
	AfmCreatorListener(ArrayList<MdObject> catalog)
	{
		 this.catalog=catalog;
	}
	
	public ArrayList<MdObject> getMetrics()
	{
		return metrics;
	}
	
	public ArrayList<MdObject> getAttributes()
	{
		return attributes;
	}
		
	public ArrayList<MdObject> getFilterAttribute()
	{
		return filterAttribute;
	}

	public ArrayList<ArrayList<String>> getFilterValue()
	{
		return filterValue;
	}
	
	public ArrayList<Boolean> getFilterPositive()
	{
	    return filterPositive;
	}
	
	public ArrayList<String> getSortIds()
	{
		return sortIds;
	}
	
	public ArrayList<String> getSortDir()
	{
		return sortDir;
	}
	
	public ArrayList<Boolean> getIsTop()
	{
		return isTop;
	}
	
	public boolean getIsNumber()
	{
		return isNumber;
	}
	
	public String getSelectedNumber()
	{
		return selectedNumber;
	}

	public boolean getIsAllColumns()
	{
		return isAllColumns;
	}
	public String getError()
	{
		return error;
	}

	
	public boolean addColumn(MdObject obj)
	{
		if ((aggregation != null)&&(obj.isFact()||obj.isAttribute()))
    	{
    		if ((aggregation.toLowerCase().equals("count")) && obj.isFact())
    		{
    			error=new String("Aggregation function count does not work with fact "+obj.getTitle());
    			lastObj=null;
    			return true;
    		}
    		if ((! aggregation.toLowerCase().equals("count")) && obj.isAttribute())
		    		{
		    			error=new String("Aggregation function "+aggregation+" count does not work with attribute "+obj.getTitle());
		    			lastObj=null;
		    			return true;
		    		}
    		MdObject clone=new MdObject(obj.getTitle(),obj.getCategory(),obj.getLink(),obj.getIdentifier());
    		clone.setAggregation(aggregation.toLowerCase());
    		metrics.add(clone);
    		aggregation = null;
    		lastObj=clone;
    		return true;
    	}
    	if (obj.isMetric() /*&& (aggregation==null)*/) 
    	{
    		MdObject clone=new MdObject(obj.getTitle(),obj.getCategory(),obj.getLink(),obj.getIdentifier());
    		metrics.add(clone);
    		lastObj=clone;
    		if (aggregation!=null)
    		{
    			lastObj.setAggregation(aggregation.toLowerCase());
    		}
    		return true;
    	}
    	if (obj.isAttribute()) 
    	{
    		MdObject clone=new MdObject(obj.getTitle(),obj.getCategory(),obj.getLink(),obj.getIdentifier());
    		attributes.add(clone);
    		lastObj=clone;
    		return true;
    	}
    	if (obj.isFact())
    	{	
    		error=new String("Missing aggregation function for fact "+obj.getTitle());
    		lastObj=null;
    		return true;	
    	}
		return false;
	}
	
	public void addColumnTitle(String title)
	{		
		for (MdObject obj : catalog) {
		    if (obj.getTitle().equals(title)) {
		    	logger.info("match "+title+" category: "+obj.getCategory()+" aggregation:"+aggregation);
		    	if (addColumn(obj)) return;
		    }
		}

		error = new String(title+" does not exist.");
		aggregation = null;
		
	}
	
	public void addColumnIdentifier(String identifier)
	{		
		for (MdObject obj : catalog) {
		    if (obj.getIdentifier().equals(identifier)) {
		    	logger.info("match {"+identifier+"} category: "+obj.getCategory()+" aggregation:"+aggregation);
		    	if (addColumn(obj)) return;
		    }
		}

		error = new String("Identifier "+identifier+" does not exist.");
		aggregation = null;
		
	}

	public void addColumnOid(String oid)
	{		
		for (MdObject obj : catalog) {
		    if (obj.getLink().equals(oid)) {
		    	logger.info("match ["+oid+"] category: "+obj.getCategory()+" aggregation:"+aggregation);
		    	if (addColumn(obj)) return;
		    }
		}

		error = new String("Link "+oid+" does not exist.");
		aggregation = null;
		
	}
	
    public void exitFunction(MaqlLiteParser.FunctionContext ctx) 
    { 
    	aggregation = ctx.ID().getText();
    }

	public void exitColumn(MaqlLiteParser.ColumnContext ctx) 
	{
		if (lastObj==null) return;
		
		if (lastObj.isAttribute())
		{
			MaqlLiteParser.PivotContext pivot=ctx.pivot();
			if ((pivot!=null)&&(pivot.COLUMNS()!=null))
			{
				logger.info("columns");
				isTop.add(new Boolean(true));
			}
			else
			{
				isTop.add(new Boolean(false));
				logger.info("rows");
			}
		}
		if (ctx.alias()!=null)
		{
			if (ctx.alias().ID()!=null)
			{
			  lastObj.setAlias(ctx.alias().ID().getText());
			}
			if (ctx.alias().TITLE()!=null)
			{
			  lastObj.setAlias(ctx.alias().TITLE().getText().replaceFirst("^\"(.*)\"$", "$1"));
			}
			isTop.add(new Boolean(false));
		}
	}

	@Override public void exitAllColumns(MaqlLiteParser.AllColumnsContext ctx) 
	{ 
		for (MdObject obj : catalog) {		 
			     MdObject clone=new MdObject(obj.getTitle(),obj.getCategory(),obj.getLink(),obj.getIdentifier());
			     if (obj.isAttribute())
			     { 
			         attributes.add(clone);
			     }
			     else
			     {
			    	 metrics.add(clone);
			     }
			     isTop.add(new Boolean(false));
			     
		}	
		isAllColumns = true;
	}

    public void exitColumnId(MaqlLiteParser.ColumnIdContext ctx) 
	{
		addColumnTitle(ctx.ID().getText());
	}

	public void exitColumnTitle(MaqlLiteParser.ColumnTitleContext ctx)
	{
		addColumnTitle(ctx.TITLE().getText().replaceFirst("^\"(.*)\"$", "$1"));
	}
	
	public void exitColumnIdentifier(MaqlLiteParser.ColumnIdentifierContext ctx)
	{
		addColumnIdentifier(ctx.IDENTIFIER().getText().replaceFirst("^\\{(.*)\\}$", "$1"));
	}
	public void exitColumnOid(MaqlLiteParser.ColumnOidContext ctx)
	{
		addColumnOid(ctx.OID().getText().replaceFirst("^\\[(.*)\\]$", "$1"));
	}
	
	public void addFilterTitle(String title)
	{
		for (MdObject obj : catalog) {
		    if (obj.getTitle().equals(title) && obj.getCategory().equals("attribute")) {
		    	 filterAttribute.add(obj);
		    	
		        return;
		    }
		}		
		error = new String(title+" is not attribute.");
	}
	
	public void addFilterIdentifier(String identifier)
	{
		for (MdObject obj : catalog) {
		    if (obj.getIdentifier().equals(identifier) && obj.getCategory().equals("attribute")) {
		    	 filterAttribute.add(obj);
		    	
		        return;
		    }
		}		
		error = new String(identifier+" is not attribute identifier.");
	}
	
	public void addFilterOid(String oid)
	{
		for (MdObject obj : catalog) {
		    if (obj.getLink().equals(oid) && obj.getCategory().equals("attribute")) {
		    	 filterAttribute.add(obj);
		    	
		        return;
		    }
		}		
		error = new String(oid+" is not attribute link.");
	}
	
	public void exitAttributeId(MaqlLiteParser.AttributeIdContext ctx)
	{
		addFilterTitle(ctx.ID().getText());
	}

	public void exitAttributeTitle(MaqlLiteParser.AttributeTitleContext ctx)
	{
		addFilterTitle(ctx.TITLE().getText().replaceFirst("^\"(.*)\"$", "$1"));
	}
	
	public void exitAttributeIdentifier(MaqlLiteParser.AttributeIdentifierContext ctx)
	{
		addFilterIdentifier(ctx.IDENTIFIER().getText().replaceFirst("^\\{(.*)\\}$", "$1"));
	}
	
	public void exitAttributeOid(MaqlLiteParser.AttributeOidContext ctx)
	{
		addFilterOid(ctx.OID().getText().replaceFirst("^\\[(.*)\\]$", "$1"));
	}

	public void exitFilterIs(MaqlLiteParser.FilterIsContext ctx)
	{
		ArrayList<String> values=new ArrayList<String>();
		values.add(ctx.STRING().getText().replaceFirst("^\'(.*)\'$", "$1"));
		filterValue.add(values);		
		filterPositive.add(new Boolean(true));
	}
	public void exitFilterIsNot(MaqlLiteParser.FilterIsNotContext ctx)
	{
		ArrayList<String> values=new ArrayList<String>();
		values.add(ctx.STRING().getText().replaceFirst("^\'(.*)\'$", "$1"));
		filterValue.add(values);
		filterPositive.add(new Boolean(false));
	}

	@Override public void exitList(MaqlLiteParser.ListContext ctx) 
	{
		list.add(ctx.STRING().getText().replaceFirst("^\'(.*)\'$", "$1"));	
	}
	
	@Override public void enterFilterIn(MaqlLiteParser.FilterInContext ctx) 
	{ 
		list = new ArrayList<String>();		
	}
	
	@Override public void exitFilterIn(MaqlLiteParser.FilterInContext ctx) 
	{
		filterValue.add(list);
		filterPositive.add(new Boolean(true));
	}
	
	@Override public void enterFilterNotIn(MaqlLiteParser.FilterNotInContext ctx) 
	{
		list = new ArrayList<String>();
	}
	
	@Override public void exitFilterNotIn(MaqlLiteParser.FilterNotInContext ctx) 
	{
		filterValue.add(list);
		filterPositive.add(new Boolean(false));
	}
		
	public void exitQuery(MaqlLiteParser.QueryContext ctx)
	{	
	    logger.info("filterAttribute: "+filterAttribute.toString()+
	    		    " filterValue: "+filterValue.toString());
	}
	@Override public void exitNumber(MaqlLiteParser.NumberContext ctx)
	{
		isNumber = true;
		selectedNumber = ctx.NUMBER().getText();
	}

	
	public void addOrderTitle(String title, String direction)
	{
		for (int index=0;index<metrics.size();index++)
		{
		    if ((metrics.get(index).getTitle().equals(title)) &&
		    	((aggregation==null && metrics.get(index).getAggregation()==null)||
		         (aggregation!=null && aggregation.equals(metrics.get(index).getAggregation()))
		        ) 
		       )
		    {
		    	sortIds.add("M"+(index+1));
		    	sortDir.add(direction);
		    	return;
		    }
		}  
		
		for (int index=0;index<attributes.size();index++)
		{
		    if (attributes.get(index).getTitle().equals(title)) 
		    {
		    	sortIds.add("A"+(index+1));
		    	sortDir.add(direction);	
		    	return;
		    }
		} 
		
		if (aggregation==null)
		{
		   error = new String(title+" is not a selected column.");
		}
		else
		{
		   error = new String(aggregation+"("+title+") is not a selected column.");
		}
	}
	
	public void addOrderIdentifier(String identifier, String direction)
	{
		for (int index=0;index<metrics.size();index++)
		{
		    if ((metrics.get(index).getIdentifier().equals(identifier)) &&
		    	((aggregation==null && metrics.get(index).getAggregation()==null)||
		         (aggregation!=null && aggregation.equals(metrics.get(index).getAggregation()))
		        ) 
		       )
		    {
		    	sortIds.add("M"+(index+1));
		    	sortDir.add(direction);
		    	return;
		    }
		}  
		
		for (int index=0;index<attributes.size();index++)
		{
		    if (attributes.get(index).getIdentifier().equals(identifier)) 
		    {
		    	sortIds.add("A"+(index+1));
		    	sortDir.add(direction);	
		    	return;
		    }
		} 
		
		if (aggregation==null)
		{
		   error = new String("Identifier "+identifier+" is not a selected column.");
		}
		else
		{
		   error = new String(aggregation+"( {"+identifier+"} ) is not a selected column.");
		}
	}
	
	public void addOrderOid(String oid, String direction)
	{
		for (int index=0;index<metrics.size();index++)
		{
		    if ((metrics.get(index).getLink().equals(oid)) &&
		    	((aggregation==null && metrics.get(index).getAggregation()==null)||
		         (aggregation!=null && aggregation.equals(metrics.get(index).getAggregation()))
		        ) 
		       )
		    {
		    	sortIds.add("M"+(index+1));
		    	sortDir.add(direction);
		    	return;
		    }
		}  
		
		for (int index=0;index<attributes.size();index++)
		{
		    if (attributes.get(index).getLink().equals(oid)) 
		    {
		    	sortIds.add("A"+(index+1));
		    	sortDir.add(direction);	
		    	return;
		    }
		} 
		
		if (aggregation==null)
		{
		   error = new String("Link "+oid+" is not a selected column.");
		}
		else
		{
		   error = new String(aggregation+"( ["+oid+"] ) is not a selected column.");
		}
	}
	
	@Override public void exitOrderFieldId(MaqlLiteParser.OrderFieldIdContext ctx) 
	{
		if (ctx.direction()!=null) 
	    {
			addOrderTitle(ctx.ID().getText(),ctx.direction().getText().toLowerCase());
	    }
		else
	    {
			addOrderTitle(ctx.ID().getText(),"asc");
	    }
		aggregation=null;
		
	}

	@Override public void exitOrderFieldTitle(MaqlLiteParser.OrderFieldTitleContext ctx) 
	{
		if (ctx.direction()!=null) 
	    {
			addOrderTitle(ctx.TITLE().getText().replaceFirst("^\"(.*)\"$", "$1"),ctx.direction().getText().toLowerCase());
	    }
		else
	    {
			addOrderTitle(ctx.TITLE().getText().replaceFirst("^\"(.*)\"$", "$1"),"asc");
	    }
		aggregation=null;
	}
	
	@Override public void exitOrderFieldIdentifier(MaqlLiteParser.OrderFieldIdentifierContext ctx) 
	{
		if (ctx.direction()!=null) 
	    {
			addOrderIdentifier(ctx.IDENTIFIER().getText().replaceFirst("^\\{(.*)\\}$", "$1"),ctx.direction().getText().toLowerCase());
	    }
		else
	    {
			addOrderIdentifier(ctx.IDENTIFIER().getText().replaceFirst("^\\{(.*)\\}$", "$1"),"asc");
	    }
		aggregation=null;
	}
	
	@Override public void exitOrderFieldOid(MaqlLiteParser.OrderFieldOidContext ctx) 
	{
		if (ctx.direction()!=null) 
	    {
			addOrderOid(ctx.OID().getText().replaceFirst("^\\[(.*)\\]$", "$1"),ctx.direction().getText().toLowerCase());
	    }
		else
	    {
			addOrderOid(ctx.OID().getText().replaceFirst("^\\[(.*)\\]$", "$1"),"asc");
	    }
		aggregation=null;
	}
}
