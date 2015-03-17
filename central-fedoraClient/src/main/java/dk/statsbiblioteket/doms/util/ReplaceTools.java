package dk.statsbiblioteket.doms.util;

import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA. User: abr Date: 3/21/13 Time: 12:41 PM To change this template use File | Settings | File
 * Templates.
 */
public class ReplaceTools {

    public EncodingType encodingAnEnum;


    public static String fillInParameters(Map<Parameter, List<String>> parameterNameToValues,
                                          String command,
                                          List<Parameter> declaredParameters,
                                          EncodingType encoding) {
        //replace parameter values
        for (Parameter declaredParameter : declaredParameters) {
            String name = declaredParameter.getName();
            List<String> parameterValues = parameterNameToValues.get(declaredParameter);
            //Get defaults
            if (parameterValues == null) {
                parameterValues = Arrays.asList(declaredParameter.getDefaultValue());
                parameterNameToValues.put(declaredParameter, parameterValues);

            }

            String parameterString = "";
            String parameterprefix = declaredParameter.getParameterprefix();
            if (parameterprefix == null) {
                parameterprefix = "";
            }
            for (Iterator<String> iterator = parameterValues.iterator(); iterator.hasNext(); ) {
                String value = iterator.next();
                //shellescape value and prepend parameter prefix
                value = parameterprefix + (declaredParameter.isEncode() ? encoding.encode(value) : value);
                parameterString = parameterString + value;
                if (iterator.hasNext()) {
                    //make ready for next value
                    parameterString = parameterString + encoding.getSeparatorChar();
                }
            }
            command = replaceParameter(command, name, parameterString);
        }
        return command;
    }

    private static String replaceParameter(String command,
                                           String name,
                                           String parameterString) {
        command = command.replaceAll(Pattern.quote("{" + name + "}"), parameterString);
        command = command.replaceAll(Pattern.quote("%%" + name + "%%"), parameterString);
        return command;
    }


    public static void setDefaultParameter(String parameterName,
                                           String parameterValue,
                                           Map<Parameter, List<String>> parameters,
                                           List<Parameter> declaredParameters) {
        Parameter parameter = new Parameter(parameterName);
        setDefaultParameter(parameter, parameterValue, parameters, declaredParameters);
    }

    public static void setDefaultParameter(Parameter parameter,
                                           String parameterValue,
                                           Map<Parameter, List<String>> parameters,
                                           List<Parameter> declaredParameters) {
        List<String> list = parameters.get(parameter);
        if (list == null) {
            list = Arrays.asList(parameterValue);
            declaredParameters.add(parameter);
        } else {
            list.add(parameterValue);
        }
        parameters.put(parameter, list);
    }


    public static void setContextParameters(List<Parameter> declaredParameters,
                                            Map<Parameter, List<String>> parameters) {


        Properties properties = ConfigCollection.getProperties();
        for (String key : properties.stringPropertyNames()) {
            Parameter parameter = new Parameter(key, false);
            setDefaultParameter(parameter, properties.getProperty(key), parameters, declaredParameters);
        }
    }


    public static void setDefaultParameters(List<Parameter> declaredParameters,
                                            Map<Parameter, List<String>> parameters,
                                            ObjectProfile profile,
                                            Fedora fedora,
                                            String domsLocation) {
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        setDefaultParameter("objectId", profile.getPid().replaceAll("^.*:", ""), parameters, declaredParameters);
        setDefaultParameter("domsPid", profile.getPid(), parameters, declaredParameters);
        setDefaultParameter("domsUser", fedora.getUsername(), parameters, declaredParameters);
        setDefaultParameter("domsPassword", fedora.getPassword(), parameters, declaredParameters);
        setDefaultParameter("domsLocation", domsLocation, parameters, declaredParameters);

        setDefaultParameter("label", profile.getLabel(), parameters, declaredParameters);
        setDefaultParameter("owner", profile.getOwnerID(), parameters, declaredParameters);
        setDefaultParameter("state", profile.getState(), parameters, declaredParameters);
        setDefaultParameter("createdISO",
                            isoFormat.format(profile.getObjectCreatedDate()),
                            parameters,
                            declaredParameters);
        setDefaultParameter("lastModifiedISO",
                            isoFormat.format(profile.getObjectLastModifiedDate()),
                            parameters,
                            declaredParameters);
        setDefaultParameter("createdUnixMillis",
                            "" + profile.getObjectCreatedDate().getTime(),
                            parameters,
                            declaredParameters);
        setDefaultParameter("lastModifiedUnixMillis",
                            "" + profile.getObjectLastModifiedDate().getTime(),
                            parameters,
                            declaredParameters);
    }

    public static Map<Parameter, List<String>> constructValuesMap(List<Parameter> declaredParameters,
                                                                  Map<String, List<String>> parameters) {
        HashMap<Parameter, List<String>> result = new HashMap<Parameter, List<String>>();
        for (Parameter declaredParameter : declaredParameters) {
            result.put(declaredParameter, parameters.get(declaredParameter.getName()));
        }
        return result;
    }
}
