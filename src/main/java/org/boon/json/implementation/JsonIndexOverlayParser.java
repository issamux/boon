package org.boon.json.implementation;

import org.boon.core.reflection.Reflection;
import org.boon.json.JsonException;
import org.boon.json.JsonParser;
import org.boon.json.internal.*;
import org.boon.primitive.CharBuf;
import org.boon.primitive.Chr;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.boon.Exceptions.die;

/**
 * Converts an input JSON String into Java objects works with String or char array
 * as input. Produces an Object which can be any of the basic JSON types mapped
 * to Java.
 */
public class JsonIndexOverlayParser implements JsonParser {

    private char[] charArray;
    private int __index;
    private char __currentChar;


    private static ValueBase EMPTY_LIST = new ValueBase ( Collections.EMPTY_LIST );



    private final boolean useValues;

    public JsonIndexOverlayParser () {
        useValues = false;

    }


    public JsonIndexOverlayParser ( boolean useValues ) {
        this.useValues = useValues;

    }





    @SuppressWarnings( "unchecked" )
    public Object decode( char[] cs ) {
        __index = 0;
        charArray = cs;
        return decodeValue ().toValue ();
    }


    public Object decode( String cs ) {
        __index = 0;
        this.charArray =  Reflection.toCharArray ( cs );
        return decodeValue ().toValue ();
    }


    public Object decode( byte[] bytes ) {
        __index = 0;
        this.charArray =  Reflection.toCharArray ( bytes );
        return decodeValue ().toValue ();
    }

    private final boolean hasMore() {
        return __index + 1 < charArray.length;
    }

    private final char nextChar() {

        try {
            if ( __index + 1 < charArray.length ) {
                __index++;
                return __currentChar = charArray[__index];
            } else {
                return '\u0000';
            }
        } catch ( Exception ex ) {
            throw new JsonException (  exceptionDetails ( "unable to advance character"), ex);
        }
    }



    private String exceptionDetails( String message ) {
        CharBuf buf = CharBuf.create ( 255 );

        buf.addLine ( message );

        buf.addLine ( "" );
        buf.addLine ( "The current character read is " + charDescription ( __currentChar ) );


        buf.addLine ( message );

        int line = 0;
        int lastLineIndex = 0;

        for ( int i = 0; i < __index; i++ ) {
            if ( charArray[i] == '\n' ) {
                line++;
                lastLineIndex = i + 1;
            }
        }

        int count = 0;

        for ( int i = lastLineIndex; i < charArray.length; i++, count++ ) {
            if ( charArray[i] == '\n' ) {
                break;
            }
        }


        buf.addLine ( "line number " + line + 1 );
        buf.addLine ( "index number " + __index );


        try {
            buf.addLine ( new String ( charArray, lastLineIndex, count ) );
        } catch ( Exception ex ) {

            try {
                int index = ( __index - 10 < 0 ) ? 0 : __index - 10;

                buf.addLine ( new String ( charArray, index, __index ) );
            } catch ( Exception ex2 ) {
                buf.addLine ( new String ( charArray, 0, charArray.length ) );
            }
        }
        for ( int i = 0; i < ( __index - lastLineIndex ); i++ ) {
            buf.add ( '.' );
        }
        buf.add ( '^' );

        return buf.toString ();
    }

    private void skipWhiteSpace() {


        label:
        for (; __index < this.charArray.length; __index++ ) {
            __currentChar = charArray[__index];
            switch ( __currentChar ) {
                case '\n':
                    continue label;

                case '\r':
                    continue label;

                case ' ':
                    continue label;

                case '\t':
                    continue label;

                default:
                    break label;

            }
        }

    }

    private final Value decodeJsonObject() {

        if ( __currentChar == '{' )
            this.nextChar ();

        JsonMap map = null;
        JsonValueMap valueMap = null;
        Value value;
        if ( useValues ) {
            valueMap = new JsonValueMap ();
            value = new ValueBase ( ( Map ) valueMap );
        } else {
            map = new JsonMap ();
            value = new ValueBase ( map );
        }


        for (; __index < this.charArray.length; __index++ ) {

            skipWhiteSpace ();


            if ( __currentChar == '"' ) {
                Value key = decodeString ();
                skipWhiteSpace ();

                if ( __currentChar != ':' ) {

                    complain ( "expecting current character to be " + charDescription ( __currentChar ) + "\n" );
                }
                this.nextChar (); // skip past ':'

                Value item = decodeValue ();

                skipWhiteSpace ();


                MapItemValue miv = new MapItemValue ( key, item );


                if ( useValues ) {
                    valueMap.add ( miv );
                } else {
                    map.add ( miv );
                }


            }
            if ( __currentChar == '}' ) {
                __index++;
                break;
            } else if ( __currentChar == ',' ) {
                continue;
            } else {
                complain (
                        "expecting '}' or ',' but got current char " + charDescription ( __currentChar ) );

            }
        }
        return value;
    }

    private void complain( String complaint ) {
        throw new JsonException ( exceptionDetails ( complaint ) );
    }


    private Value decodeValue() {

        label:
        for (; __index < this.charArray.length; __index++ ) {
            __currentChar = charArray[__index];

            switch ( __currentChar ) {

            case '\n':
                continue label;

            case '\r':
                continue label;

            case ' ':
                continue label;

            case '\t':
                continue label;


            case '"':
                return  decodeString ();


            case 't':
                return decodeTrue ();

            case 'f':
                return decodeFalse ();


            case 'n':
                return decodeNull ();

            case '[':
                return  decodeJsonArray ();

            case '{':
                return decodeJsonObject ();

            case '1':
                return decodeNumber ();

            case '2':
                return decodeNumber ();

            case '3':
                return decodeNumber ();

            case '4':
                return decodeNumber ();

            case '5':
                return decodeNumber ();

            case '6':
                return decodeNumber ();

            case '7':
                return decodeNumber ();

            case '8':
                return decodeNumber ();

            case '9':
                return decodeNumber ();

            case '0':
                return decodeNumber ();

            case '-':
                return decodeNumber ();


                default :

                    throw new JsonException ( exceptionDetails ( "Unable to determine the " +
                            "current character, it is not a string, number, array, or object" ) );



            }
        }

        throw new JsonException ( exceptionDetails ( "Unable to determine the " +
                "current character, it is not a string, number, array, or object" ) );

    }


    private Value decodeNumber() {

        int startIndex = __index;

        boolean doubleFloat = false;

        int index;

        loop:
        for ( index = __index; index < charArray.length; index++ ) {
            __currentChar = charArray[index];

            switch ( __currentChar ) {
                case ' ':
                    __index = index + 1;
                    break loop;

                case '\t':
                    __index = index + 1;
                    break loop;

                case '\n':
                    __index = index + 1;
                    break loop;

                case '\r':
                    __index = index + 1;
                    break loop;

                case ',':
                    break loop;

                case ']':
                    break loop;

                case '}':
                    break loop;

                case '1':
                    continue loop;

                case '2':
                    continue loop;

                case '3':
                    continue loop;

                case '4':
                    continue loop;

                case '5':
                    continue loop;

                case '6':
                    continue loop;

                case '7':
                    continue loop;

                case '8':
                    continue loop;

                case '9':
                    continue loop;

                case '0':
                    continue loop;

                case '-':
                    continue loop;


                case '+':
                    doubleFloat = true;
                    continue loop;

                case 'e':
                    doubleFloat = true;
                    continue loop;

                case 'E':
                    doubleFloat = true;
                    continue loop;

                case '.':
                    doubleFloat = true;
                    continue loop;

            }

            complain ( "expecting number char but got current char " + charDescription ( __currentChar ) );
        }

        __index = index;

        ValueInCharBuf value = new ValueInCharBuf ();
        value.buffer = this.charArray;
        value.startIndex = startIndex;
        value.endIndex = __index;

        if ( doubleFloat ) {
            value.type = Type.DOUBLE;
        } else {
            value.type = Type.INTEGER;
        }

        skipWhiteSpace ();

        return value;

    }


    private static char[] NULL = Chr.chars ( "null" );


    private Value decodeNull() {

        if ( __index + NULL.length <= charArray.length ) {
            if ( charArray[__index] == 'n' &&
                    charArray[++__index] == 'u' &&
                    charArray[++__index] == 'l' &&
                    charArray[++__index] == 'l' ) {
                nextChar ();
                return Value.NULL;
            }
        }
        throw new JsonException ( exceptionDetails ( "null not parse properly" ) );
    }

    private static char[] TRUE = Chr.chars ( "true" );


    private Value decodeTrue() {

        if ( __index + TRUE.length <= charArray.length ) {
            if ( charArray[__index] == 't' &&
                    charArray[++__index] == 'r' &&
                    charArray[++__index] == 'u' &&
                    charArray[++__index] == 'e' ) {

                nextChar ();
                return Value.TRUE;

            }
        }

        throw new JsonException ( exceptionDetails ( "true not parsed properly" ) );
    }


    private static char[] FALSE = Chr.chars ( "false" );

    private Value decodeFalse() {

        if ( __index + FALSE.length <= charArray.length ) {
            if ( charArray[__index] == 'f' &&
                    charArray[++__index] == 'a' &&
                    charArray[++__index] == 'l' &&
                    charArray[++__index] == 's' &&
                    charArray[++__index] == 'e' ) {
                nextChar ();
                return Value.FALSE;
            }
        }
        throw new JsonException ( exceptionDetails ( "false not parsed properly" ) );
    }

    private Value decodeString() {
        ValueInCharBuf value = new ValueInCharBuf ( Type.STRING );


        __currentChar = charArray[__index];

        if ( __index < charArray.length && __currentChar == '"' ) {
            __index++;
        }

        final int startIndex = __index;


        boolean escape = false;

        boolean encoded = false;

        done:
        for (; __index < this.charArray.length; __index++ ) {
            __currentChar = charArray[__index];
            switch ( __currentChar ) {

                case '"':
                    if ( !escape )  {
                        break done;
                    } else {
                        escape = false;
                        continue;
                    }


                case '\\':
                    encoded = true;
                    escape = true;
                    continue;

            }
            escape = false;
        }

        value.startIndex = startIndex;
        value.endIndex = __index;
        value.buffer = charArray;
        value.decodeStrings = encoded;

        if ( __index < charArray.length ) {
            __index++;
        }

        return value;
    }



    private Value decodeJsonArray() {
        if ( __currentChar == '[' ) {
            this.nextChar ();
        }

        skipWhiteSpace ();




        /* the list might be empty  */
        if ( __currentChar == ']' ) {
            this.nextChar ();
            return EMPTY_LIST;
        }


        List<Object> list = null;

        if ( useValues ) {
            list = new ArrayList<> ();
        } else {
            list = new JsonList ();
        }

        Value value = new ValueBase ( list );

        int arrayIndex = 0;

        do {
            Value arrayItem = decodeValue ();

            if ( arrayItem == null ) {
                list.add ( ValueBase.NULL ); //JSON null detected
            } else {
                list.add ( arrayItem );
            }

            arrayIndex++;

            skipWhiteSpace ();

            char c = __currentChar;

            if ( c == ',' ) {
                this.nextChar ();
                continue;
            } else if ( c == ']' ) {
                this.nextChar ();
                break;
            } else {
                String charString = charDescription ( c );

                complain (
                        String.format ( "expecting a ',' or a ']', " +
                                " but got \nthe current character of  %s " +
                                " on array index of %s \n", charString, arrayIndex )
                );

            }
        } while ( this.hasMore () );
        return value;
    }

    private String charDescription( char c ) {
        String charString;
        if ( c == ' ' ) {
            charString = "[SPACE]";
        } else if ( c == '\t' ) {
            charString = "[TAB]";

        } else if ( c == '\n' ) {
            charString = "[NEWLINE]";

        } else {
            charString = "'" + c + "'";
        }

        charString = charString + " with an int value of " + ( ( int ) c );
        return charString;
    }




    @Override
    public <T> T parse( Class<T> type, String str ) {
        return (T) this.decode ( str  );
    }

    @Override
    public <T> T parse( Class<T> type, byte[] bytes ) {
        return (T) this.decode ( bytes  );
    }

    @Override
    public <T> T parse( Class<T> type, CharSequence charSequence ) {
        return parse(type, charSequence.toString ());
    }

    @Override
    public <T> T parse( Class<T> type, char[] chars ) {
        return (T) this.decode ( chars );
    }

    @Override
    public <T> T parse( Class<T> type, Reader reader ) {

        die("you are using the wrong class");
        return null;
    }

    @Override
    public <T> T parse( Class<T> type, InputStream input ) {
        die("you are using the wrong class");
        return null;
    }

    @Override
    public <T> T parse( Class<T> type, InputStream input, Charset charset ) {
        die("you are using the wrong class");
        return null;
    }

}