#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * LICENSE_PLACEHOLDER
 */
package ${package}.domain;

/**
 *
 * TODO Description
 *
 * @author TODO
 *
 */
public class Greeting {

    private final String content_;

    public Greeting(String pName) {
        this.content_ = "Hello " + pName;
    }

    public String getContent() {
        return content_;
    }
}
