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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 *@author  Robert Watkins
 *@author  <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class CVSTest extends TestCase {

    private Date createDate(String dateString) throws ParseException {
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
        return formatter.parse(dateString);
    }

    public void testValidate() throws CruiseControlException, IOException {
        CVS cvs = new CVS();

        try {
            cvs.validate();
            fail("CVS should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        cvs.setCvsRoot("cvsroot");

        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("CVS should not throw exceptions when required fields are set.");
        }

        cvs = new CVS();
        File tempFile = File.createTempFile("temp", "txt");
        cvs.setLocalWorkingCopy(tempFile.getParent());

        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("CVS should not throw exceptions when required fields are set.");
        }

        String badDirName = "z:/foo/foo/foo/bar";
        cvs.setLocalWorkingCopy(badDirName);
        try {
            cvs.validate();
            fail("CVS.validate should throw exception on non-existant directory.");
        } catch (CruiseControlException e) {
        }
    }

    public void testParseStream() throws IOException, ParseException {
        CVS cvs = new CVS();
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("alden", "alden@users.sourceforge.net");
        emailAliases.put("tim", "tim@tim.net");
        cvs.setMailAliases(emailAliases);

        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11.txt");
        //System.out.println(testLog.getAbsolutePath());
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals(
                "Should have returned 5 modifications.",
                5,
                modifications.size());

        Modification mod1 = new Modification();
        mod1.type = "modified";
        mod1.fileName = "log4j.properties";
        mod1.folderName = "";
        mod1.revision = "1.2";
        mod1.modifiedTime = createDate("2002/03/13 13:45:50 GMT-6:00");
        mod1.userName = "alden";
        mod1.comment =
                "Shortening ConversionPattern so we don't use up all of the available screen space.";
        mod1.emailAddress = "alden@users.sourceforge.net";

        Modification mod2 = new Modification();
        mod2.type = "modified";
        mod2.fileName = "build.xml";
        mod2.folderName = "main";
        mod2.revision = "1.41";
        mod2.modifiedTime = createDate("2002/03/13 19:56:34 GMT-6:00");
        mod2.userName = "alden";
        mod2.comment = "Added target to clean up test results.";
        mod2.emailAddress = "alden@users.sourceforge.net";

        Modification mod3 = new Modification();
        mod3.type = "modified";
        mod3.fileName = "build.xml";
        mod3.folderName = "main";
        mod3.revision = "1.42";
        mod3.modifiedTime = createDate("2002/03/15 13:20:28 GMT-6:00");
        mod3.userName = "alden";
        mod3.comment = "enabled debug info when compiling tests.";
        mod3.emailAddress = "alden@users.sourceforge.net";

        Modification mod4 = new Modification();
        mod4.type = "deleted";
        mod4.fileName = "kungfu.xml";
        mod4.folderName = "main";
        mod4.revision = "1.2";
        mod4.modifiedTime = createDate("2002/03/13 13:45:42 GMT-6:00");
        mod4.userName = "alden";
        mod4.comment = "Hey, look, a deleted file.";
        mod4.emailAddress = "alden@users.sourceforge.net";

        Modification mod5 = new Modification();
        mod5.type = "deleted";
        mod5.fileName = "stuff.xml";
        mod5.folderName = "main";
        mod5.revision = "1.4";
        mod5.modifiedTime = createDate("2002/03/13 13:38:42 GMT-6:00");
        mod5.userName = "alden";
        mod5.comment = "Hey, look, another deleted file.";
        mod5.emailAddress = "alden@users.sourceforge.net";

        assertEquals(mod5, modifications.get(0));
        assertEquals(mod4, modifications.get(1));
        assertEquals(mod1, modifications.get(2));
        assertEquals(mod2, modifications.get(3));
        assertEquals(mod3, modifications.get(4));
    }

    public void testParseStreamBranch() throws IOException, ParseException {
        CVS cvs = new CVS();
        Hashtable emailAliases = new Hashtable();
        emailAliases.put("alden", "alden@users.sourceforge.net");
        cvs.setMailAliases(emailAliases);

        cvs.setTag("BRANCH_TEST_BUILD");
        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11branch.txt");
        //System.out.println(testLog.getAbsolutePath());
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        List modifications = cvs.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals(
                "Should have returned 4 modifications.",
                4,
                modifications.size());

        Modification mod1 = new Modification();
        mod1.type = "modified";
        mod1.fileName = "test.version";
        mod1.folderName = "";
        mod1.revision = "1.1.2.4";
        mod1.modifiedTime = createDate("2002/10/03 16:05:23 GMT");
        mod1.userName = "tim";
        mod1.comment = "Test commit once more";

        Modification mod2 = new Modification();
        mod2.type = "modified";
        mod2.fileName = "test.version";
        mod2.folderName = "";
        mod2.revision = "1.1.2.3";
        mod2.modifiedTime = createDate("2002/10/03 14:24:17 GMT");
        mod2.userName = "tim";
        mod2.comment = "Test commit";

        Modification mod3 = new Modification();
        mod3.type = "modified";
        mod3.fileName = "test.version";
        mod3.folderName = "";
        mod3.revision = "1.1.2.2";
        mod3.modifiedTime = createDate("2002/10/02 21:54:44 GMT");
        mod3.userName = "tim";
        mod3.comment = "Update parameters for test";

        Modification mod4 = new Modification();
        mod4.type = "modified";
        mod4.fileName = "test.version";
        mod4.folderName = "";
        mod4.revision = "1.1.2.1";
        mod4.modifiedTime = createDate("2002/10/02 21:49:31 GMT");
        mod4.userName = "tim";
        mod4.comment = "Add parameters for test";

        assertEquals(mod4, modifications.get(0));
        assertEquals(mod3, modifications.get(1));
        assertEquals(mod2, modifications.get(2));
        assertEquals(mod1, modifications.get(3));
    }

    public void testGetProperties() throws IOException, ParseException {
        CVS cvs = new CVS();
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");
        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11.txt");
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be two properties.", 2, table.size());

        assertTrue("Property was not set.", table.containsKey("property"));
        assertTrue(
                "PropertyOnDelete was not set.",
                table.containsKey("propertyOnDelete"));

        //negative test
        CVS cvs2 = new CVS();
        cvs2.setMailAliases(new Hashtable());
        input = new BufferedInputStream(new FileInputStream(testLog));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesNoModifications()
            throws IOException, ParseException {
        CVS cvs = new CVS();
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");
        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11noMods.txt");
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesOnlyModifications()
            throws IOException, ParseException {
        CVS cvs = new CVS();
        cvs.setMailAliases(new Hashtable());
        cvs.setProperty("property");
        cvs.setPropertyOnDelete("propertyOnDelete");
        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11mods.txt");
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be one property.", 1, table.size());
        assertTrue("Property was not set.", table.containsKey("property"));

        //negative test
        CVS cvs2 = new CVS();
        cvs2.setMailAliases(new Hashtable());
        cvs2.setPropertyOnDelete("propertyOnDelete");
        input = new BufferedInputStream(new FileInputStream(testLog));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testGetPropertiesOnlyDeletions()
            throws IOException, ParseException {
        CVS cvs = new CVS();
        cvs.setMailAliases(new Hashtable());
        cvs.setPropertyOnDelete("propertyOnDelete");
        File testLog =
                new File("test/net/sourceforge/cruisecontrol/sourcecontrols/cvslog1-11del.txt");
        BufferedInputStream input =
                new BufferedInputStream(new FileInputStream(testLog));
        cvs.parseStream(input);
        input.close();

        Hashtable table = cvs.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Should be one property.", 1, table.size());
        assertTrue(
                "PropertyOnDelete was not set.",
                table.containsKey("propertyOnDelete"));

        //negative test
        CVS cvs2 = new CVS();
        cvs2.setMailAliases(new Hashtable());
        input = new BufferedInputStream(new FileInputStream(testLog));
        cvs2.parseStream(input);
        input.close();

        table = cvs2.getProperties();
        assertNotNull("Table of properties shouldn't be null.", table);

        assertEquals("Shouldn't be any properties.", 0, table.size());
    }

    public void testBuildHistoryCommand() throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setLocalWorkingCopy(".");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "log",
                    "-d>" + CVS.formatCVSDate(lastBuildTime),
                    "-b"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime).getCommandline();

        assertEquals(
                "Mismatched lengths!",
                expectedCommand.length,
                actualCommand.length);
        for (int i = 0; i < expectedCommand.length; i++) {
            assertEquals(expectedCommand[i], actualCommand[i]);
        }
    }

    public void testBuildHistoryCommandWithTag()
            throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setLocalWorkingCopy(".");
        element.setTag("sometag");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "log",
                    "-d>" + CVS.formatCVSDate(lastBuildTime),
                    "-rsometag"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime).getCommandline();

        assertEquals(
                "Mismatched lengths!",
                expectedCommand.length,
                actualCommand.length);
        for (int i = 0; i < expectedCommand.length; i++) {
            assertEquals(expectedCommand[i], actualCommand[i]);
        }
    }

    public void testHistoryCommandNullLocal() throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot("cvsroot");
        element.setLocalWorkingCopy(null);

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-d",
                    "cvsroot",
                    "-q",
                    "log",
                    "-d>" + CVS.formatCVSDate(lastBuildTime),
                    "-b"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime).getCommandline();

        assertEquals(
                "Mismatched lengths!",
                expectedCommand.length,
                actualCommand.length);
        for (int i = 0; i < expectedCommand.length; i++) {
            assertEquals(expectedCommand[i], actualCommand[i]);
        }
    }

    public void testHistoryCommandNullCVSROOT() throws CruiseControlException {
        Date lastBuildTime = new Date();

        CVS element = new CVS();
        element.setCvsRoot(null);
        element.setLocalWorkingCopy(".");

        String[] expectedCommand =
                new String[]{
                    "cvs",
                    "-q",
                    "log",
                    "-d>" + CVS.formatCVSDate(lastBuildTime),
                    "-b"};

        String[] actualCommand =
                element.buildHistoryCommand(lastBuildTime).getCommandline();
        assertEquals(
                "Mismatched lengths!",
                expectedCommand.length,
                actualCommand.length);
        for (int i = 0; i < expectedCommand.length; i++) {
            assertEquals(expectedCommand[i], actualCommand[i]);
        }
    }

    public void testParseLogDate() throws ParseException {
        TimeZone tz = TimeZone.getDefault();
        Date may18SixPM2001 = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals(may18SixPM2001, CVS.LOGDATE.parse("2001/05/18 18:00:00 "
                + tz.getDisplayName(tz.inDaylightTime(may18SixPM2001), TimeZone.SHORT)));
    }

    public void testFormatCVSDateGMTPlusZero() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
        Date mayEighteenSixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals(
                "2001-05-18 18:00:00 GMT",
                CVS.formatCVSDate(mayEighteenSixPM2001));
    }

    public void testFormatCVSDateGMTPlusTen() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date mayEighteenSixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals(
                "2001-05-18 08:00:00 GMT",
                CVS.formatCVSDate(mayEighteenSixPM2001));
        Date may18EightAM2001 =
                new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
        assertEquals(
                "2001-05-17 22:00:00 GMT",
                CVS.formatCVSDate(may18EightAM2001));
    }

    public void testFormatCVSDateGMTMinusTen() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));
        Date may18SixPM2001 =
                new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
        assertEquals(
                "2001-05-19 04:00:00 GMT",
                CVS.formatCVSDate(may18SixPM2001));
        Date may18EightAM2001 =
                new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
        assertEquals(
                "2001-05-18 18:00:00 GMT",
                CVS.formatCVSDate(may18EightAM2001));
    }

    public void testAddAliasToMap() {
        CVS cvs = new CVS();
        Hashtable aliasMap = new Hashtable();
        cvs.setMailAliases(aliasMap);
        String userline = "roberto:'Roberto DaMana <damana@cs.unipr.it>'";
        cvs.addAliasToMap(userline);
        userline = "hill:hill@cs.unipr.it";
        cvs.addAliasToMap(userline);
        userline = "zolo:zolo";
        cvs.addAliasToMap(userline);
        assertEquals("'Roberto DaMana <damana@cs.unipr.it>'", aliasMap.get("roberto"));
        assertEquals("hill@cs.unipr.it", aliasMap.get("hill"));
        assertEquals("zolo", aliasMap.get("zolo"));

        userline = "me";
        cvs.addAliasToMap(userline);
        assertNull(aliasMap.get("me"));
    }

}
