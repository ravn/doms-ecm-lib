package dk.statsbiblioteket.doms.util;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/21/13 Time: 1:04 PM To change this template use File | Settings | File
 * Templates.
 */
public class Parameter {

    protected final String name;
    protected final String parameterprefix;
    protected final boolean required;
    protected final boolean repeatable;
    protected final String type;
    protected final String config;
    protected final String defaultValue;
    protected final boolean encode;

    public String getDefaultValue() {
        return defaultValue;
    }

    public Parameter(String name) {
        this.name = name;
        parameterprefix = "";
        required = false;
        repeatable = false;
        type = "";
        config = "";
        defaultValue = "";
        encode = true;
    }

    public Parameter(String name,
                     boolean encode) {
        this.name = name;
        parameterprefix = "";
        required = false;
        repeatable = false;
        type = "";
        config = "";
        defaultValue = "";
        this.encode = encode;
    }

    public Parameter(String name,
                     String parameterprefix,
                     boolean required,
                     boolean repeatable,
                     String type,
                     String config,
                     String defaultValue,
                     boolean encode) {
        this.name = name;
        this.parameterprefix = parameterprefix;
        this.required = required;
        this.repeatable = repeatable;
        this.type = type;
        this.config = config;
        this.defaultValue = defaultValue;
        this.encode = encode;
    }

    public String getName() {
        return name;
    }

    public String getParameterprefix() {
        return parameterprefix;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public String getType() {
        return type;
    }

    public String getConfig() {
        return config;
    }

    public boolean isEncode() {
        return encode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Parameter)) {
            return false;
        }

        Parameter parameter = (Parameter) o;

        if (!name.equals(parameter.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
