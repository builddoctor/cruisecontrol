/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *  we often see builds that fail because the previous build is still holding on to some resource.
 *  we can avoid this by just building in a different process which will completely die after every
 *  build.
 */
public class AntBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(AntBuilder.class);

    private String buildFile;
    private String target;
    private String tempFileName = "log.xml";
    private String antScript;
    private boolean useLogger;
    private List args = new ArrayList();
    private List properties = new ArrayList();

    public void validate() throws CruiseControlException {
        super.validate();

        if (buildFile == null) {
            throw new CruiseControlException("'buildfile' is a required attribute on AntBuilder");
        }

        if (target == null) {
            throw new CruiseControlException("'target' is a required attribute on AntBuilder");
        }
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        Process p = null;
        try {
            p =
                Runtime.getRuntime().exec(
                    getCommandLineArgs(
                        buildProperties,
                        useLogger,
                        antScript != null,
                        isWindows()));
        } catch (IOException e) {
            throw new CruiseControlException(
                "Encountered an IO exception while attempting to execute Ant."
                    + " CruiseControl cannot continue.",
                e);
        }

        StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
        StreamPumper outPumper = new StreamPumper(p.getInputStream());
        new Thread(errorPumper).start();
        new Thread(outPumper).start();

        try {
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (InterruptedException e) {
            LOG.info(
                "Was interrupted while waiting for Ant to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } catch (IOException ie) {
            LOG.info("Exception trying to close Process streams.", ie);
        }

        outPumper.flush();
        errorPumper.flush();

        //read in log file as element, return it
        File logFile = new File(tempFileName);
        if (!logFile.exists()) {
            LOG.error("Ant logfile cannot be found");
        }
        Element buildLogElement = getAntLogAsElement(logFile);
        logFile.delete();

        return buildLogElement;
    }

    public void setAntScript(String antScript) {
        this.antScript = antScript;
    }

    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    public Object createJVMArg() {
        JVMArg arg = new JVMArg();
        args.add(arg);
        return arg;
    }

    public Property createProperty() {
        Property property = new Property();
        properties.add(property);
        return property;
    }

    protected boolean isWindows() {
        return System.getProperty("os.name").indexOf("Windows") > 0;
    }

    /**
     *  construct the command that we're going to execute.
     *  @param buildProperties Map holding key/value pairs of arguments to the build process
     *  @return String[] holding command to be executed
     */
    protected String[] getCommandLineArgs(
        Map buildProperties,
        boolean useLogger,
        boolean useScript,
        boolean isWindows) {
        List al = new ArrayList();

        if (useScript) {
            if (isWindows) {
                al.add("cmd.exe");
                al.add("/C");
                al.add(antScript);
            } else {
                al.add(antScript);
            }
        } else {
            al.add("java");
            Iterator argsIterator = args.iterator();
            while (argsIterator.hasNext()) {
                String arg = ((JVMArg) argsIterator.next()).getArg();
                // empty args may break the command line
                if (arg != null && arg.length() > 0) {
                    al.add(arg);
                }
            }
            al.add("-classpath");
            al.add(System.getProperty("java.class.path"));
            al.add("org.apache.tools.ant.Main");
        }

        if (useLogger) {
            al.add("-logger");
            al.add("org.apache.tools.ant.XmlLogger");
            al.add("-logfile");
            al.add(tempFileName);
        } else {
            al.add("-listener");
            al.add("org.apache.tools.ant.XmlLogger");
            al.add("-DXmlLogger.file=" + tempFileName);
        }

        Iterator propertiesIterator = buildProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String key = (String) propertiesIterator.next();
            al.add("-D" + key + "=" + buildProperties.get(key));
        }

        Iterator antPropertiesIterator = properties.iterator();
        while (antPropertiesIterator.hasNext()) {
            Property property = (Property) antPropertiesIterator.next();
            al.add("-D" + property.getName() + "=" + property.getValue());
        }

        if (LOG.isDebugEnabled()) {
            al.add("-debug");
            al.add("-verbose");
        }

        al.add("-buildfile");
        al.add(buildFile);
        al.add(target);

        StringBuffer sb = new StringBuffer();
        sb.append("Executing Command: ");
        Iterator argIterator = al.iterator();
        while (argIterator.hasNext()) {
            String arg = (String) argIterator.next();
            sb.append(arg);
            sb.append(" ");
        }
        LOG.debug(sb.toString());

        return (String[]) al.toArray(new String[al.size()]);
    }

    /**
     *  JDOM doesn't like the <?xml:stylesheet ?> tag.  we don't need it, so we'll skip it.
     *  TO DO: make sure that we are only skipping this string and not something else
     */
    protected static Element getAntLogAsElement(File f)
        throws CruiseControlException {
        try {
            Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 150; i++) {
                sb.append((char) r.read());
            }
            String beginning = sb.toString();
            int skip = beginning.lastIndexOf("<build");

            BufferedReader bufferedReader =
                new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), "UTF-8"));
            bufferedReader.skip(skip);
            SAXBuilder builder =
                new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            return builder.build(bufferedReader).getRootElement();
        } catch (Exception ee) {
            throw new CruiseControlException(
                "Error reading : " + f.getAbsolutePath(),
                ee);
        }
    }

    public class JVMArg {

        private String arg;

        public void setArg(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
        
    }

    public class Property {

        private String name;
        private String value;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
        
    }
    
}