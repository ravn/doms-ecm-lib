package dk.statsbiblioteket.doms.central.connectors.fedora.methods;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.fedora.Fedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Method;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.doms.util.EncodingType;
import dk.statsbiblioteket.doms.util.Parameter;
import dk.statsbiblioteket.doms.util.ReplaceTools;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: abr Date: 9/13/12 Time: 10:58 AM To change this template use File | Settings | File
 * Templates.
 */
public class MethodsImpl implements Methods {


    Fedora fedora;
    private String thisLocation;
    private Unmarshaller jaxb;


    public MethodsImpl(Fedora fedora,
                       String thisLocation)
            throws
            JAXBException {
        this.fedora = fedora;
        this.thisLocation = thisLocation;
        jaxb =
                JAXBContext.newInstance("dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated")
                           .createUnmarshaller();
    }

    /**
     * Invoke the method
     *
     * @param pid        The pid of the content model or object defining the method.
     * @param methodName The name of the method.
     * @param parameters Parameters for the method, as a map from name list of values.
     * @param asOfTime   Use the methods defined at this time (unix time in ms), or null for now.
     *
     * @return
     * @throws BackendInvalidResourceException
     *
     * @throws BackendInvalidCredsException
     * @throws BackendMethodFailedException
     */
    @Override
    public String invokeMethod(String pid,
                               String methodName,
                               Map<String, List<String>> parameters,
                               Long asOfTime)
            throws
            BackendInvalidResourceException,
            BackendInvalidCredsException,
            BackendMethodFailedException {


        List<Method> methods = getStaticMethods(pid, asOfTime);

        boolean staticMethod = true;

        // Find method
        Method chosenMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                chosenMethod = method;
                break;
            }
        }
        ObjectProfile profile = fedora.getObjectProfile(pid, asOfTime);

        if (chosenMethod == null) {
            staticMethod = false;
            methods = getDynamicMethods(profile, asOfTime);
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    chosenMethod = method;
                    break;
                }
            }
        }
        if (chosenMethod == null) {
            throw new BackendInvalidResourceException("Failed to find specified method");
        }

        String command = chosenMethod.getCommand();


        List<Parameter> declaredParameters = new ArrayList<Parameter>();

        //Get the parameters declared in the method def
        declaredParameters = convertFromJaxB(chosenMethod.getParameters().getParameter());


        Map<Parameter, List<String>> actualParameters = ReplaceTools.constructValuesMap(declaredParameters, parameters);
        ReplaceTools.setDefaultParameters(declaredParameters, actualParameters, profile, fedora, thisLocation);
        ReplaceTools.setContextParameters(declaredParameters, actualParameters);
        command = ReplaceTools.fillInParameters(actualParameters, command, declaredParameters, EncodingType.SHELL);


        // Run command
        List<String> commandList = new ArrayList<String>();
        commandList.add("/bin/bash");
        commandList.add("-c");
        commandList.add(command);
        ProcessBuilder procesBuilder = new ProcessBuilder(commandList);
        try {
            Process process = procesBuilder.start();
            int returnCode = process.waitFor();
            StringWriter writer = new StringWriter();
            IOUtils.copy(process.getInputStream(), writer);

            if (returnCode == 0) {
            } else {
                IOUtils.copy(process.getErrorStream(), writer);
            }
            return writer.toString();
        } catch (Exception e) {
            throw new BackendMethodFailedException("Failed to run command " + commandList.toString(), e);
        }
    }

    private static List<Parameter> convertFromJaxB(List<dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Parameter> methodDefParams) {
        List<Parameter> declaredParameters = new ArrayList<Parameter>();

        for (dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Parameter methodDefParam : methodDefParams) {
            declaredParameters.add(new Parameter(methodDefParam.getName(),
                                                 methodDefParam.getParameterprefix(),
                                                 methodDefParam.isRequired(),
                                                 methodDefParam.isRepeatable(),
                                                 methodDefParam.getType(),
                                                 methodDefParam.getConfig(),
                                                 methodDefParam.getDefault(),
                                                 true));
        }
        return declaredParameters;
    }


    public List<Method> getDynamicMethods(String objpid,
                                          Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {
        ObjectProfile profile = fedora.getObjectProfile(objpid, null);
        return getDynamicMethods(profile, asOfTime);
    }

    public List<Method> getDynamicMethods(ObjectProfile profile,
                                          Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {
        List<Method> result = new ArrayList<Method>();
        for (String contentModelPid : profile.getContentModels()) {
            List<Method> methods = getMethods(contentModelPid, asOfTime);
            for (Method method : methods) {
                if (method.getType().equals("dynamic")) {
                    result.add(method);
                }
            }
        }
        return result;
    }


    private List<Method> getMethods(String cmpid,
                                    Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {
        String methodsXml = null;
        try {
            //TODO check that the model is in fact a content model??
            methodsXml = fedora.getXMLDatastreamContents(cmpid, "METHODS", asOfTime);
        } catch (BackendInvalidResourceException e) {
            return new ArrayList<Method>();
        }

        try {
            JAXBElement<dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Methods>
                    parsed =
                    (JAXBElement<dk.statsbiblioteket.doms.central.connectors.fedora.methods.generated.Methods>) jaxb.unmarshal(
                            new StringReader(methodsXml));
            return parsed.getValue().getMethod();
        } catch (JAXBException e) {
            throw new BackendMethodFailedException("failed to parse Methods definition", e);
        }
    }


    @Override
    public List<Method> getStaticMethods(String cmpid,
                                         Long asOfTime)
            throws
            BackendInvalidCredsException,
            BackendMethodFailedException,
            BackendInvalidResourceException {
        List<Method> result = new ArrayList<Method>();
        List<Method> methods = getMethods(cmpid, asOfTime);
        for (Method method : methods) {
            if (method.getType().equals("static")) {
                result.add(method);
            }
        }
        return result;

    }

}
