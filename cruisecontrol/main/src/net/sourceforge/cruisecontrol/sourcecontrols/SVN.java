/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * This class implements the SourceControl methods for a Subversion repository.
 * The call to Subversion is assumed to work without any setup. This implies
 * that either authentication data must be available or the login parameters are
 * specified in the cc configuration file.
 *
 * Note: You can also observe for changes a Subversion repository that you have
 *       not checked out locally.
 *
 * @see    <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
 * @author <a href="etienne.studer@canoo.com">Etienne Studer</a>
 */
public class SVN implements SourceControl {

    private static final Logger LOG = Logger.getLogger(SVN.class);

    /** Date format expected by Subversion */
    private static final String SVN_DATE_FORMAT_IN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** Date format returned by Subversion in XML output */
    private static final String SVN_DATE_FORMAT_OUT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private final SourceControlProperties properties = new SourceControlProperties();

    /** Configuration parameters */
    private String repositoryLocation;
    private String localWorkingCopy;
    private String userName;
    private String password;
    private boolean checkExternals = false;

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    /**
     * Sets whether externals used by the project should also be checked
     * for modifications.
     *
     * @param value true/false
     */
    public void setCheckExternals(boolean value) {
        checkExternals = value;
    }

    /**
     * Sets the repository location to use when making calls to Subversion.
     *
     * @param repositoryLocation  String indicating the url to the Subversion
     *                            repository on which to find the log history.
     */
    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    /**
     * Sets the local working copy to use when making calls to Subversion.
     *
     * @param localWorkingCopy  String indicating the relative or absolute path
     *                          to the local working copy of the Subversion
     *                          repository of which to find the log history.
     */
    public void setLocalWorkingCopy(String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }

    /**
     * Sets the username for authentication.
     * @param userName svn user
     */
    public void setUsername(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the password for authentication.
     * @param password svn password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This method validates that at least the repository location or the local
     * working copy location has been specified.
     *
     * @throws CruiseControlException  Thrown when the repository location and
     *                                 the local working copy location are both
     *                                 null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(repositoryLocation != null || localWorkingCopy != null,
                "At least 'repositoryLocation'or 'localWorkingCopy' is a required attribute on the Subversion task ");

        if (localWorkingCopy != null) {
            File workingDir = new File(localWorkingCopy);
            ValidationHelper.assertTrue(workingDir.exists() && workingDir.isDirectory(),
                    "'localWorkingCopy' must be an existing directory. Was "
                    + workingDir.getAbsolutePath());
        }
    }

    /**
     * Returns a list of modifications detailing all the changes between
     * the last build and the latest revision in the repository.
     * @return the list of modifications, or an empty list if we failed
     * to retrieve the changes.
     */
    public List getModifications(Date lastBuild, Date now) {
        HashMap directories = new HashMap();
        Commandline propCommand = new Commandline();
        // the propget command can be pretty expensive on large projects
        // so only execute if the checkExternals flag is set in the config
        if (checkExternals) {
            try {
                propCommand = buildPropgetCommand();
            } catch (CruiseControlException e) {
                LOG.error("Error building history command", e);
            }
            try {
                directories = execPropgetCommand(propCommand);
            } catch (Exception e) {
                LOG.error("Error executing svn propget command " + propCommand, e);
            }
        }

        List modifications = new ArrayList();
        Commandline command;
        String path;
        String svnURL;
        HashMap commandsAndPaths = new HashMap();
        try {
            // always check the root
            command = buildHistoryCommand(lastBuild, now, Util.isWindows());
            commandsAndPaths.put(command, null);
            for (Iterator iter = directories.keySet().iterator(); iter.hasNext();) {
                 String directory = (String) iter.next();
                 ArrayList externals =
                     (ArrayList) directories.get(directory);
                 for (Iterator eiter = externals.iterator(); eiter.hasNext();) {
                      String[] external = (String[]) eiter.next();
                      path = directory + "/" + external[0];
                      svnURL = external[1];
                      if (repositoryLocation != null) {
                          command = buildHistoryCommand(
                              lastBuild, now, Util.isWindows(), svnURL);
                          commandsAndPaths.put(command, null);
                      } else {
                          command = buildHistoryCommand(
                              lastBuild, now, Util.isWindows(), path);
                          commandsAndPaths.put(command, path);
                      }
                 }
            }
        } catch (CruiseControlException e) {
            LOG.error("Error building history command", e);
            return modifications;
        }
        try {
            for (Iterator iter = commandsAndPaths.keySet().iterator(); iter.hasNext();) {
                 command = (Commandline) iter.next();
                 path = (String) commandsAndPaths.get(command);
                 modifications.addAll(execHistoryCommand(
                     command, lastBuild, path));
            }
        } catch (Exception e) {
            LOG.error("Error executing svn log command " + command, e);
        }
        fillPropertiesIfNeeded(modifications);
        return modifications;
    }

    /**
     * Generates the command line for the svn propget command.
     *
     * For example:
     *
     * 'svn propget -R svn:externals repositoryLocation'
     * @return new command line object
     * @throws net.sourceforge.cruisecontrol.CruiseControlException if working directory is invalid
     */
    Commandline buildPropgetCommand() throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("propget");
        command.createArgument("-R");
        command.createArgument("--non-interactive");
        command.createArgument("svn:externals");
        
        if (repositoryLocation != null) {
            command.createArgument(repositoryLocation);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }

    /**
     * Generates the command line for the svn log command.
     *
     * For example:
     *
     * 'svn log --non-interactive --xml -v -r "{lastbuildTime}":"{checkTime}" repositoryLocation'
     * @return history command
     * @param lastBuild date
     * @param checkTime checkTime
     * @param isWindows os
     * @throws net.sourceforge.cruisecontrol.CruiseControlException exception
     */
    Commandline buildHistoryCommand(Date lastBuild, Date checkTime, boolean isWindows)
        throws CruiseControlException {
        return buildHistoryCommand(lastBuild, checkTime, isWindows, null);
    }

    Commandline buildHistoryCommand(Date lastBuild, Date checkTime, boolean isWindows, String path)
        throws CruiseControlException {

        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument("log");
        command.createArgument("--non-interactive");
        command.createArgument("--xml");
        command.createArgument("-v");
        command.createArgument("-r");
        if (isWindows) {
            command.createArgument("\"{" + formatSVNDate(lastBuild) + "}\"" + ":"
                    + "\"{" + formatSVNDate(checkTime) + "}\"");
        } else {
            command.createArgument("{" + formatSVNDate(lastBuild) + "}" + ":"
                    + "{" + formatSVNDate(checkTime) + "}");
        }

        if (userName != null) {
            command.createArguments("--username", userName);
        }
        if (password != null) {
            command.createArguments("--password", password);
        }
        if (path != null) {
            command.createArgument(path);
        } else if (repositoryLocation != null) {
            command.createArgument(repositoryLocation);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }


    static String formatSVNDate(Date lastBuild) {
        DateFormat f = new SimpleDateFormat(SVN_DATE_FORMAT_IN);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(lastBuild);
    }

    private static HashMap execPropgetCommand(Commandline command)
        throws InterruptedException, IOException {

        Process p = command.execute();

        logErrorStream(p);
        InputStream svnStream = p.getInputStream();

        HashMap directories = new HashMap(); 
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(svnStream, "UTF8"));

        String line;
        String currentDir = null;

        while ((line = reader.readLine()) != null) {
            String[] split = line.split(" - ");
            // the directory containing the externals
            if (split.length > 1) {
                currentDir = split[0];
                directories.put(currentDir, new ArrayList());
                line = split[1];
            }
            split = line.split(" ");
            if (!split[0].equals("")) {
                ArrayList externals = (ArrayList) directories.get(currentDir);
                // split contains: [externalPath, externalSvnURL]
                externals.add(split);
            }
        }

        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return directories;
    }

    private static List execHistoryCommand(Commandline command, Date lastBuild,
                                    String externalPath)
        throws InterruptedException, IOException, ParseException, JDOMException {

        Process p = command.execute();

        Thread stderr = logErrorStream(p);
        InputStream svnStream = p.getInputStream();
        List modifications = parseStream(svnStream, lastBuild, externalPath);

        p.waitFor();
        stderr.join();
        IO.close(p);

        return modifications;
    }

    private static Thread logErrorStream(Process p) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    private static List parseStream(InputStream svnStream, Date lastBuild,
                             String externalPath)
        throws JDOMException, IOException, ParseException {

        InputStreamReader reader = new InputStreamReader(svnStream, "UTF-8");
        return SVNLogXMLParser.parseAndFilter(reader, lastBuild, externalPath);
    }

    void fillPropertiesIfNeeded(List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();
            int maxRevision = 0;
            for (int i = 0; i < modifications.size(); i++) {
                Modification modification = (Modification) modifications.get(i);
                maxRevision = Math.max(maxRevision, Integer.parseInt(modification.revision));
                Modification.ModifiedFile file = (Modification.ModifiedFile) modification.files.get(0);
                if (file.action.equals("deleted")) {
                    properties.deletionFound();
                }
            }
            properties.put("svnrevision", "" + maxRevision);
        }
    }

    public static DateFormat getOutDateFormatter() {
        DateFormat f = new SimpleDateFormat(SVN_DATE_FORMAT_OUT);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f;
    }

    static final class SVNLogXMLParser {

        private SVNLogXMLParser() {
        }

        static List parseAndFilter(Reader reader, Date lastBuild)
                throws ParseException, JDOMException, IOException {
            return parseAndFilter(reader, lastBuild, null);
        }

        static List parseAndFilter(Reader reader, Date lastBuild, String externalPath)
                throws ParseException, JDOMException, IOException {
            Modification[] modifications = parse(reader, externalPath);
            return filterModifications(modifications, lastBuild);
        }

        static Modification[] parse(Reader reader)
                throws ParseException, JDOMException, IOException {
            return parse(reader, null);
        }

        static Modification[] parse(Reader reader, String externalPath)
                throws ParseException, JDOMException, IOException {

            SAXBuilder builder = new SAXBuilder(false);
            Document document = builder.build(reader);
            return parseDOMTree(document, externalPath);
        }

        static Modification[] parseDOMTree(Document document, String externalPath)
                throws ParseException {
            List modifications = new ArrayList();

            Element rootElement = document.getRootElement();
            List logEntries = rootElement.getChildren("logentry");
            for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
                Element logEntry = (Element) iterator.next();

                Modification[] modificationsOfRevision =
                    parseLogEntry(logEntry, externalPath);
                modifications.addAll(Arrays.asList(modificationsOfRevision));
            }

            return (Modification[]) modifications.toArray(new Modification[modifications.size()]);
        }

        static Modification[] parseLogEntry(Element logEntry, String externalPath)
                throws ParseException {
            List modifications = new ArrayList();

            Element logEntryPaths = logEntry.getChild("paths");
            if (logEntryPaths != null) {
                List paths = logEntryPaths.getChildren("path");
                for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
                    Element path = (Element) iterator.next();

                    Modification modification = new Modification("svn");

                    modification.modifiedTime = convertDate(logEntry.getChildText("date"));
                    modification.userName = logEntry.getChildText("author");
                    modification.comment = logEntry.getChildText("msg");
                    modification.revision = logEntry.getAttributeValue("revision");

                    Modification.ModifiedFile modfile = modification.createModifiedFile(path.getText(), null);
                    // modfile.folderName seems to add too many /'s
                    if (externalPath != null) {
                        modfile.fileName = "/" + externalPath + ":" + modfile.fileName;
                    }
                    modfile.action = convertAction(path.getAttributeValue("action"));
                    modfile.revision = modification.revision;

                    modifications.add(modification);
                }
            }

            return (Modification[]) modifications.toArray(new Modification[modifications.size()]);
        }

        /**
         * Converts the specified SVN date string into a Date.
         * @param date with format "yyyy-MM-dd'T'HH:mm:ss.SSS" + "...Z"
         * @return converted date
         * @throws ParseException if specified date doesn't match the expected format
         */
        static Date convertDate(String date) throws ParseException {
            final int zIndex = date.indexOf('Z');
            if (zIndex - 3 < 0) {
                throw new ParseException(date
                        + " doesn't match the expected subversion date format", date.length());
            }
            String withoutMicroSeconds = date.substring(0, zIndex - 3);

            return getOutDateFormatter().parse(withoutMicroSeconds);
        }

        static String convertAction(String action) {
            if (action.equals("A")) {
                return "added";
            }
            if (action.equals("M")) {
                return "modified";
            }
            if (action.equals("D")) {
                return "deleted";
            }
            return "unknown";
        }

        /**
         * Unlike CVS, Subversion maps dates to revisions which leads to a
         * different behavior when using the svn log command in conjunction with
         * dates, e.g., a date maps to a revision but the revision may have been
         * created earlier than the specified date. Therefore, if we are only
         * interested in changes that took place after the last build date, we
         * have to filter the modifications returned from the log command and
         * omit modifications that are older than the last build date.
         *
         * @see <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
         * @return subset of modifications
         * @param modifications source
         * @param lastBuild last build date
         */
        static List filterModifications(Modification[] modifications, Date lastBuild) {
            List filtered = new ArrayList();
            for (int i = 0; i < modifications.length; i++) {
                Modification modification = modifications[i];
                if (modification.modifiedTime.getTime() > lastBuild.getTime()) {
                    filtered.add(modification);
                }
            }
            return filtered;
        }
    }
}
