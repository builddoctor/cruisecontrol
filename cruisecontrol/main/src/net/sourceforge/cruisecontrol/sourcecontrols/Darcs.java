/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Source Control implementation for Darcs. Provides a means of executing the darcs changes command and parsing the xml
 * output to determine if there have been any changes. The modifications are parsed and used for cruisecontrol build
 * reports which allow the patch names associated with a build to be displayed. Currently the darcs xml-output does not
 * display which files have changed.
 * 
 * Large parts of this implementation were based on the {@link net.sourceforge.cruisecontrol.sourcecontrols.SVN} source
 * control implementation.
 */
public class Darcs implements SourceControl {

    private static final long serialVersionUID = 7976081409836256093L;
    private static final Logger LOGGER = Logger.getLogger(Darcs.class);
    private static final DateFormat DARCS_DATE_FORMAT_IN = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String workingDir;
    private String repositoryLocation;
    private SourceControlProperties properties = new SourceControlProperties();

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(repositoryLocation != null || workingDir != null,
                "At least 'repositoryLocation'or 'workingDir' is a required attribute on the Darcs task ");

        if (workingDir != null) {
            File workingDirFile = new File(workingDir);
            ValidationHelper.assertTrue(workingDirFile.exists() && workingDirFile.isDirectory(),
                    "'workingDir' must be an existing directory. Was " + workingDirFile.getAbsolutePath());
        }
    }

    public List getModifications(Date lastBuild, Date now) {
        try {
            Commandline command = buildChangesCommand(lastBuild, now);
            List modifications = execChangesCommand(command);
            fillPropertiesIfNeeded(modifications);
            return modifications;
        } catch (Exception e) {
            LOGGER.error("Failed to execute darcs changes command", e);
        }
        return Collections.EMPTY_LIST;
    }

    Commandline buildChangesCommand(Date lastBuild, Date checkTime) throws CruiseControlException {

        Commandline command = new Commandline();
        command.setExecutable("darcs");

        if (workingDir != null) {
            command.setWorkingDirectory(workingDir);
        }

        command.createArgument("changes");
        command.createArgument("--xml-output");
        command.createArgument("--match");
        command.createArgument("--matches");
        command.createArgument("date \"" + DARCS_DATE_FORMAT_IN.format(lastBuild) + "/"
                + DARCS_DATE_FORMAT_IN.format(checkTime) + "\"");

        LOGGER.debug("Executing command: " + command);

        return command;
    }

    private List execChangesCommand(Commandline command) throws InterruptedException, IOException, ParseException,
            JDOMException {

        Process p = command.execute();

        Thread stderr = logErrorStream(p);
        InputStream darcsStream = p.getInputStream();
        List modifications = parseStream(darcsStream);
        p.waitFor();
        stderr.join();
        IO.close(p);

        return modifications;
    }

    private List parseStream(InputStream darcsStream) throws ParseException, JDOMException, IOException {
        InputStreamReader reader = new InputStreamReader(new BufferedInputStream(darcsStream), "UTF-8");
        return DarcsXmlParser.parse(reader);
    }

    private Thread logErrorStream(Process p) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOGGER, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    void fillPropertiesIfNeeded(List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();
        }
    }

    static final class DarcsXmlParser {
        private static final DateFormat DARCS_DATE_FORMAT_OUT = new SimpleDateFormat("yyyyMMddHHmmss");

        private DarcsXmlParser() { /* helper class, no instances */
        }

        static List parse(Reader reader) throws ParseException, JDOMException, IOException {

            SAXBuilder builder = new SAXBuilder(false);
            Document document = builder.build(reader);
            return parseDOMTree(document);
        }

        private static List parseDOMTree(Document document) throws ParseException {
            List modifications = new ArrayList();
            Element rootElement = document.getRootElement();
            List patches = rootElement.getChildren("patch");
            if (patches != null) {
                for (Iterator iterator = patches.iterator(); iterator.hasNext();) {
                    Element patch = (Element) iterator.next();
                    modifications.add(parsePatch(patch));
                }
            }
            return modifications;
        }

        private static Modification parsePatch(Element patch) throws ParseException {
            Modification modification = new Modification("darcs");
            modification.modifiedTime = DARCS_DATE_FORMAT_OUT.parse(patch.getAttributeValue("date"));
            String email = patch.getAttributeValue("author");
            modification.userName = parseUser(email);
            modification.emailAddress = email;
            modification.comment = patch.getChildText("name");
            return modification;
        }

        private static String parseUser(String email) {
            return email.substring(0, email.indexOf('@'));
        }
    }

}
