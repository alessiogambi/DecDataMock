grammar MiniSql;

@lexer::members {
public void emitErrorMessage(String message) {
    System.err.println(message);
    System.exit(1);
}
}

@parser::header {
import pbnj.examples.primitives.*;
}

@parser::members {

private final int getInt(String text) {
    try {
        return Integer.parseInt(text);
    } catch (NumberFormatException except) {
        System.out.println(Token.INVALID_TOKEN + "integer '" + text + "' out of range");
        return 0;
    }
}
private final float getFloat(String text) {
    try {
        return Float.parseFloat(text);
    } catch (NumberFormatException except) {
        System.out.println(Token.INVALID_TOKEN + "float '" + text + "' out of range");
        return 0;
    }
}
}

@lexer::header {
}

command returns [List<Object> cmd]:
        ( x = create_db | x = connect | x = close | x = create_tb 
        | x = insert | x = delete | x = select | x = update ) {
            cmd = x;
        };

create_db returns [List<Object> cmd = new ArrayList<Object>()]:
        CREATE DATABASE x = ident {
            $cmd.add(SqlCmd.CREATE_DB);
            $cmd.add(new Object[] {  PBJString.get($x.text) });
        };

connect returns [List<Object> cmd = new ArrayList<Object>()]:
        CONNECT x = ident {
            $cmd.add(SqlCmd.CONNECT);
            $cmd.add(new Object[] {  PBJString.get($x.text) });
        };

close returns [List<Object> cmd = new ArrayList<Object>()]:
        CLOSE DATABASE {
            $cmd.add(SqlCmd.CLOSE);
            $cmd.add(new Object[] { });
        };

create_tb returns [List<Object> cmd = new ArrayList<Object>()]:
        CREATE TABLE x = ident OPEN_PAREN 
            y = ident { List<String> cols = new ArrayList<String>(); cols.add($y.text); } 
            ( COMMA y = ident { cols.add($y.text); } ) * CLOSE_PAREN {
            int s = cols.size();
            PBJString[] vs = new PBJString[s];
            $cmd.add(SqlCmd.CREATE_TABLE);
            $cmd.add(new Object[] {  PBJString.get($x.text), new Integer(cols.size()), vs });
            for (int i = 0; i < s; i++)
                vs[i] = new PBJString(cols.get(i));
        };

insert returns [List<Object> cmd = new ArrayList<Object>()]:
        INSERT INTO x = ident VALUES OPEN_PAREN 
            y = literal { List<Literal> cols = new ArrayList<Literal>(); cols.add(y); }
            ( COMMA y = literal { cols.add(y); } ) * CLOSE_PAREN {
            $cmd.add(SqlCmd.INSERT);
            int s = cols.size();
            Literal[] vs = new Literal[s];
            $cmd.add(new Object[]{ PBJString.get($x.text), vs } );
            for (int i = 0; i < s; i++)
                vs[i] = cols.get(i);
        };

delete returns [List<Object> cmd = new ArrayList<Object>()]:
        DELETE FROM x = ident y = where {
            $cmd.add(SqlCmd.DELETE);
            $cmd.add(new Object[]{ PBJString.get($x.text), y } );
        };

select returns [List<Object> cmd = new ArrayList<Object>()]:
        SELECT STAR FROM x = ident
        y = where {
            $cmd.add(SqlCmd.SELECT);
            $cmd.add(new Object[]{ PBJString.get($x.text), y } );
        };

update returns [List<Object> cmd = new ArrayList<Object>()]:
        UPDATE x = ident SET y = ident EQUALS z = literal m = where {
            $cmd.add(SqlCmd.UPDATE);
            $cmd.add(new Object[]{ PBJString.get($x.text), PBJString.get($y.text), z, m } );
        };

where returns [ BExpr b ]:
        w = whereH { b = w; }
    ;
        
whereH returns [ BExpr b ]:
          WHERE ( c = cmpExpr
                { b = c; } )
              ( o = frmOp d = cmpExpr
                { b = new FormulaExpr(o, b, d); } ) *            
    |  { }
    ;

cmpExpr returns [ CmpExpr c ]:
        y = ident o = cmpOp n = literal 
                { c = new CmpExpr(o, PBJString.get((String) $y.text), n); }
    ;

cmpOp returns [ Op o ]:
        EQUALS
            { o = Op.EQ; }
    |   LT GT
            { o = Op.NE; }
    ;

frmOp returns [ Op o ]:
        AND
            { o = Op.AND; }
    |   OR
            { o = Op.OR; }
    ;

literal returns [ Literal l ]:
        x = num
            { l = Literal.get(getInt($x.text)); }
    |   x = num DOT y = num 
            { l = Literal.get(getFloat($x.text + "." + $y.text)); }
    |   QUOTE z = ident QUOTE
            { l = Literal.get($z.text); }
    ;


letter:            ( LOWER_CASE_LETTER | UPPER_CASE_LETTER ) ;
num:               ( ( PLUS | MINUS ) ? DIGIT+ );
ident :            ( letter ( DIGIT | letter | UNDERBAR ) * ) ;
WHITESPACE:        ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ { skip(); };

CREATE:            'create';
CONNECT:           'connect';
CLOSE:             'close';
INSERT:            'insert';
SELECT:            'select';
DELETE:            'delete';
UPDATE:            'update';
WHERE:             'where';
INTO:              'into';
FROM:              'from';
SET:               'set';
VALUES:            'values';
DATABASE:          'database';
TABLE:             'table';
UNDERBAR:          '_';
AND:               'and';
OR:                'or';
STAR:              '*';
EQUALS:            '=';
LT:                '<';
GT:                '>';
MINUS:             '-';
PLUS:              '+';
COMMA:             ',';
DOT:               '.';
OPEN_PAREN:        '(';
CLOSE_PAREN:       ')';
QUOTE:             '\'';
DIGIT:             '0'..'9';
LOWER_CASE_LETTER: 'a'..'z';
UPPER_CASE_LETTER: 'A'..'Z';
