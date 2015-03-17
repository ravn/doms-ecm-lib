package dk.statsbiblioteket.doms.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/21/13 Time: 1:35 PM To change this template use File | Settings | File
 * Templates.
 */
public enum EncodingType {

    URL, SHELL;

    String getSeparatorChar() {
        switch (this) {
            case SHELL:
                return " ";
            case URL:
                return "&amp;";
        }
        return "";
    }


    public String encode(String value) {
        try {
            switch (this) {
                case URL:
                    return URLEncoder.encode(value, "UTF-8");
                case SHELL:
                    return "'" + value.replaceAll("\'", "\'\\\\\'\'") + "'";
            }
            return value;
        } catch (UnsupportedEncodingException e) {
            throw new Error("UTF-8 not known");
        }
    }
}
