/**
 * Define a grammar called Hello
 */
grammar MaqlLite; 

/*
 * Parser Rules
 */
 
query : SELECT ( columns | allColumns ) (FROM DATA)? where_clause? having_clause? order_by_clause? #select
	  | SELECT NUMBER #number
	  | SELECT function '(' NUMBER ')' #number
	  ;

data : ID | TITLE ;

allColumns : '*' ;

columns : column 
        | columns ',' column ;

column : columnObject pivot? alias?
       | function '(' columnObject ')' pivot? alias?
       | function '(' '1' ')' alias?
       ;

columnObject : (DATA '.')? ID          #columnId
             | (DATA '.') TITLE       #columnTitle
             | IDENTIFIER  #columnIdentifier
             | OID         #columnOid
             ;    
       
function : ID ;

pivot : ON ROWS
      | ON COLUMNS
      ; 

alias : AS ID | AS TITLE ;

where_clause : WHERE filters ;

filters : filter 
        | filters AND filter ;

filter : attribute '=' STRING          #filterIs
       | attribute '!=' STRING         #filterIsNot
       | attribute IN '(' list ')'     #filterIn
       | attribute NOT IN '(' list ')' #filterNotIn
       ;

attribute : ID         #attributeId
          | TITLE      #attributeTitle
          | IDENTIFIER #attributeIdentifier
          | OID        #attributeOid
          ;

list : STRING
     | list ',' STRING ;
     
having_clause : HAVING having_filter ;
              
              
having_filter : COUNT '(' '1' ')' '>' '0'
              | '(' having_filter ')'; 

order_by_clause : ORDER BY orderFields;

orderFields : orderFields ',' orderField
            | orderField
            ;

orderField  : ID direction?    #orderFieldId
            | TITLE direction? #orderFieldTitle
            | IDENTIFIER direction? #orderFieldIdentifier
            | OID direction? #orderFieldOid
            | function '(' ID ')' direction?     #orderFieldId
            | function '(' TITLE ')' direction?  #orderFieldTitle
            | function '(' IDENTIFIER ')' direction? #orderFieldIdentifier
            | function '(' OID ')' direction? #orderFieldOid
            ;

direction : ASC | DESC ;
   
keyword : ASC |
          DESC|
          ON |
          ROWS |
          COLUMNS |
          AS |
          SELECT |
          FROM |
          DATA |
          WHERE |
          AND |
          IN |
          NOT |
          ORDER |
          BY |
          ID |
          QUOTE |
          DOUBLEQUOTE |
          TITLE |
          IDENTIFIER |
          OID |
          STRING |
          HAVING |
          COUNT ;

/*
 * Lexer Rules
 */

fragment S          : ('S'|'s') ;
fragment E          : ('E'|'e') ;
fragment L          : ('L'|'l') ;
fragment C          : ('C'|'c') ;
fragment T          : ('T'|'t') ;
fragment F          : ('F'|'f') ;
fragment R          : ('R'|'r') ;
fragment O          : ('O'|'o') ;
fragment M          : ('M'|'m') ;
fragment D          : ('D'|'d') ;
fragment A          : ('A'|'a') ;
fragment W          : ('W'|'w') ;
fragment H          : ('H'|'h') ;
fragment N          : ('N'|'n') ;
fragment I          : ('I'|'i') ;
fragment B          : ('B'|'b') ;
fragment Y          : ('Y'|'y') ;
fragment U          : ('U'|'u') ;
fragment V          : ('V'|'v') ;
fragment G          : ('G'|'g') ;


DATA :  ( G O O D ) D A T A | '"' ( G O O D ) D A T A '"';

ASC : A S C ;

DESC: D E S C ;

ON: O N ;

ROWS: R O W S ;

COLUMNS: C O L U M N S ;

AS : A S ;

SELECT : S E L E C T ;

FROM : F R O M ;



WHERE : W H E R E ;

AND : A N D ;

IN : I N ;

NOT : N O T ;

ORDER : O R D E R ;

BY : B Y ;

HAVING: H A V I N G ;

COUNT : C O U N T ;

ID : [a-zA-Z][a-zA-Z0-9_.]* ;  

NUMBER : '0' 'x' [0-9]+ ;

QUOTE : '\'' ;

DOUBLEQUOTE : '"' ;

TITLE :  DOUBLEQUOTE (~["])* DOUBLEQUOTE ; // double quoted string 

IDENTIFIER :  '{' (~[}])* '}' ; 

OID : '[' (~[\]])* ']' ;

STRING : QUOTE (~['])* QUOTE ; 

OUTERSQL : [.]+ ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines
