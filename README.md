# gooddata-maql-jdbc-demo
A proof of concept code to demonstrate integration of GoodData analytical engine with 3rd party reporting or visualization tool. For demo and education purposes only.

## Limitations
* The driver is not production ready and does not implement robust exception handling.
* The syntax is limited. The driver implements only a subset of capabilities of the executeAfm API
* The driver supports referencing metrics and attributes by title which may be not unique. In case of conflict it randomly selects one of the objects with given title. Objects can be referenced also by identifier (IDENTIFIER) or link (OID). See examples below.
* If an attribute has multiple display forms (a.k.a. labels) the default display form is used (or the first one if the default display form is not defined)


## Getting started

1. Run `mvn clean antlr4:antlr4 compile assembly:single`. It will build the `jdbc4maql-0.1-jar-with-dependencies.jar` file under the `target` folder
2. Include the built jar file in the Java classpath of your SQL client or your reporting tool
3. Use the following parameters when setting up your JDBC connection:
  - Driver class name: `com.gooddata.maql.jdbc.driver.Driver`
  - Connection string: `jdbc:maql://{hostname}/gdc/projects/{workspace_id}` (example: `jdbc:maql://secure.gooddata.com/gdc/projects/budtwmhq7k94ve7rqj49j3620rzsm3u1`)

## Examples

Note: these queries were tested against the GoodSales Demo workspace

* select "Month/Year (Closed)", Won, Lost 
* SELECT Region, Won FROM DATA
* select "Sales Rep", Won where Region='West Coast' and Department = 'Direct Sales'
* select Department,Region,"Sales Rep", Won, Lost, "# of Activities" from data where "Year (Created)"='2011'
* select "Month/Year (Closed)", sum("Amount"), runsum(Amount), max("Days to Close") where Status='Won'
* select Won where Region in ("West Coast")
* select "Sales Rep",Won,Lost order by Won desc
* select Region on rows,Department on columns,Won,Lost
* select "Year (Date)" as Year,"Month (Date)" as Month,"Won" as Revenue
* select {attr.owner.id},{attr.owner.region},{attr.owner.department},{afSEwRwdbMeQ} as Won
* select [/gdc/md/pid/obj/1284] as Won

## Screenshots

A query result:
https://raw.githubusercontent.com/koles/gooddata-maql-jdbc-demo/documentation/maql4jdbc-query.png

A list of metrics:
https://raw.githubusercontent.com/koles/gooddata-maql-jdbc-demo/documentation/maql4jdbc-introspection.png

## Grammar

```
query  : SELECT columns (FROM DATA)? where_clause? order_by_clause?;      
 
columns : column pivot? alias?| columns ',' column pivot? alias?;
 
column : columnObject | function '(' columnObject ')' ;
 
columnObject : ID | TITLE | IDENTIFIER | OID ;
 
function : ID ;
 
where_clause : WHERE filters ;
 
filters : filter | filters AND filter ;
 
filter : attribute '=' STRING  
       | attribute '!=' STRING 
       | attribute IN '(' list ')'
       | attribute NOT IN '(' list ')';
 
 
list : STRING | list ',' STRING ;
 
attribute : ID | TITLE | IDENTIFIER | OID;
 
 
order_by_clause : ORDER BY sort_columns;
 
 
sort_columns : column | sort_columns ',' column ;
 
 
alias : AS ID | AS ID TITLE ;
 
 
pivot : ON ROWS | ON COLUMNS ;
```

## Credits

Developed by Jakub Sterba
