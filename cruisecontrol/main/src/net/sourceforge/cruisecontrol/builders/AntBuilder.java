/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.EmptyElementFilter;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * we often see builds that fail because the previous build is still holding on to some resource.
 * we can avoid this by just building in a different process which will completely die after every
 * build.
 */
public class AntBuilder extends Builder {

    protected static final String DEFAULT_LOGGER = "org.apache.tools.ant.XmlLogger";
    private static final Logger LOG = Logger.getLogger(AntBuilder.class);

    private String antWorkingDir = null;
    private String buildFile = "build.xml";
    private String target = "";
    private String tempFileName = "log.xml";
    private String antScript;
    private String antHome;
    private boolean useLogger;
    private List args = new ArrayList();
    private List properties = new ArrayList();
    private boolean useDebug = false;
    private boolean useQuiet = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private File saveLogDir = null;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    
    public void validate() throws CruiseControlException {
        super.validate();

        ValidationHelper.assertIsSet(buildFile, "buildfile", this.getClass());
        ValidationHelper.assertIsSet(target, "target", this.getClass());
        ValidationHelper.assertFalse(useDebug && useQuiet,
            "'useDebug' and 'useQuiet' can't be used together");

        if (!useLogger && (useDebug || useQuiet)) {
            LOG.warn("usedebug and usequiet are ignored if uselogger is not set to 'true'!");
        }

        if (saveLogDir != null) {
            ValidationHelper.assertTrue(saveLogDir.isDirectory(), "'saveLogDir' must exist and be a directory");
        }

        ValidationHelper.assertFalse(antScript != null && antHome != null,
            "'antHome' and 'antscript' cannot both be set");

        if (antHome != null) {
            final File antHomeFile = new File(antHome);
            ValidationHelper.assertTrue(antHomeFile.exists() && antHomeFile.isDirectory(),
                "'antHome' must exist and be a directory. Expected to find "
                + antHomeFile.getAbsolutePath());

            final File antScriptInAntHome = new File(findAntScript(Util.isWindows()));
            ValidationHelper.assertTrue(antScriptInAntHome.exists() && antScriptInAntHome.isFile(),
                "'antHome' must contain an ant execution script. Expected to find "
                + antScriptInAntHome.getAbsolutePath());

            antScript = antScriptInAntHome.getAbsolutePath();
        }

        if (antScript != null && !args.isEmpty()) {
            LOG.warn("jvmargs will be ignored if you specify anthome or your own antscript!");
        }
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {       
        
        AntScript script = new AntScript();
        script.setBuildProperties(buildProperties);
        script.setProperties(properties);
        script.setUseLogger(useLogger);
        script.setUseScript(antScript != null);
        script.setWindows(Util.isWindows());
        script.setAntScript(antScript);
        script.setArgs(args);
        script.setBuildFile(buildFile);
        script.setTarget(target);
        script.setLoggerClassName(loggerClassName);
        script.setTempFileName(tempFileName);
        script.setUseDebug(useDebug);
        script.setUseQuiet(useQuiet);
        script.setSystemClassPath(getSystemClassPath());
       
        File workingDir = antWorkingDir != null ? new File(antWorkingDir) : null;
        
        ScriptRunner scriptRunner = new ScriptRunner();
        boolean scriptCompleted = scriptRunner.runScript(workingDir, script, timeout);
                    
        File logFile = new File(antWorkingDir, tempFileName);
        Element buildLogElement;
        if (!scriptCompleted) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
            // although log file is most certainly empty, let's try to preserve it
            // somebody should really fix ant's XmlLogger
            if (logFile.exists()) {
                try {
                    buildLogElement.setText(Util.readFileToString(logFile));
                } catch (IOException likely) {
                }
            }
        } else {
            //read in log file as element, return it
            buildLogElement = getAntLogAsElement(logFile);
            saveAntLog(logFile);
            logFile.delete();
        }
        return buildLogElement;
    }


    /**
     * Set the location to which the ant log will be saved before Cruise
     * Control merges the file into its log.
     * 
     * @param dir
     *          the absolute path to the directory where the ant log will be
     *          saved or relative path to where you started CruiseControl
     */
    public void setSaveLogDir(String dir) {
        saveLogDir = null;
        
        if (dir != null && !dir.trim().equals("")) {
            saveLogDir = new File(dir.trim());
        }
    }

    void saveAntLog(File logFile) {
        if (saveLogDir == null) {
            return;
        }

        try {
            File newAntLogFile = new File(saveLogDir, tempFileName);
            newAntLogFile.createNewFile();

            FileInputStream in = new FileInputStream(logFile);
            FileOutputStream out = new FileOutputStream(newAntLogFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException ioe) {
            LOG.error(ioe);
            LOG.error("Unable to create file: " + new File(saveLogDir, tempFileName));
        }
  }

    /**
     * Set the working directory where Ant will be invoked. This parameter gets
     * set in the XML file via the antWorkingDir attribute. The directory can
     * be relative (to the cruisecontrol current working directory) or absolute.
     * 
     * @param dir
     *          the directory to make the current working directory.
     */
    public void setAntWorkingDir(String dir) {
        antWorkingDir = dir;
    }

    /**
     * Sets the Script file to be invoked (in place of calling the Ant class
     * directly).  This is a platform dependent script file.
     *
     * @param antScript the name of the script file
     */
    public void setAntScript(String antScript) {
        this.antScript = antScript;
    }
    
    /**
     * If set CC will use the platform specific script provided by Ant
     * 
     * @param antHome the path to ANT_HOME
     */
    public void setAntHome(String antHome) {
        this.antHome = antHome;
    }

    /**
     * If the anthome attribute is set, then this method returns the correct shell script
     * to use for a specific environment.
     */
    protected String findAntScript(boolean isWindows) throws CruiseControlException {
        if (antHome == null) {
            throw new CruiseControlException("anthome attribute not set.");
        }

        if (isWindows) {
            return antHome + "\\bin\\ant.bat";
        } else {
            return antHome + "/bin/ant";
        }
    }

    /**
     * Set the name of the temporary file used to capture output.
     *
     * @param tempFileName
     */
    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    /**
     * Set the Ant target(s) to invoke.
     *
     * @param target the target(s) name.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Used to invoke the builder via JMX with a different target.
     */
    protected void overrideTarget(String target) {
        setTarget(target);    
    }

    /**
     * Sets the name of the build file that Ant will use.  The Ant default is
     * build.xml, use this to override it.
     *
     * @param buildFile the name of the build file.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * Sets whether Ant will use the custom loggers.
     *
     * @param useLogger
     */
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

    protected String getSystemClassPath() {
      return System.getProperty("java.class.path");
    }

    protected static Element getAntLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath() + " does not exist.");
        } else if (file.length() == 0) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath()
                    + " is empty. Your build probably failed. Check your CruiseControl logs.");
        }

        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

            // old Ant-versions contain a bug in the XmlLogger that outputs
            // an invalid PI containing the target "xml:stylesheet" 
            // instead of "xml-stylesheet": fix this
            XMLFilter piFilter = new XMLFilterImpl() {
                public void processingInstruction(String target, String data) throws SAXException {
                    if (target.equals("xml:stylesheet")) { target = "xml-stylesheet"; }
                    super.processingInstruction(target, data);
                }
            };

            // get rid of empty <task>- and <message>-elements created by Ant's XmlLogger
            XMLFilter emptyTaskFilter = new EmptyElementFilter("task");
            emptyTaskFilter.setParent(piFilter);
            XMLFilter emptyMessageFilter = new EmptyElementFilter("message");
            emptyMessageFilter.setParent(emptyTaskFilter);
            builder.setXMLFilter(emptyMessageFilter);
            return builder.build(file).getRootElement();
        } catch (Exception ee) {
            if (ee instanceof CruiseControlException) {
                throw (CruiseControlException) ee;
            }
            File saveFile = new File(file.getParentFile(), System.currentTimeMillis() + file.getName());
            file.renameTo(saveFile);
            throw new CruiseControlException("Error reading : " + file.getAbsolutePath()
                    + ".  Saved as : " + saveFile.getAbsolutePath(), ee);
        }
    }

    public void setUseDebug(boolean debug) {
        useDebug = debug;
    }

    public void setUseQuiet(boolean quiet) {
        useQuiet = quiet;
    }

    public String getLoggerClassName() {
        return loggerClassName;
    }

    public void setLoggerClassName(String string) {
        loggerClassName = string;
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
    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
