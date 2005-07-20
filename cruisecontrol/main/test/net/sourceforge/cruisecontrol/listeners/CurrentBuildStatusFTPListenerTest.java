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
package net.sourceforge.cruisecontrol.listeners;

import junit.framework.TestCase;
import junit.framework.Assert;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author jerome@coffeebreaks.org
 */
public class CurrentBuildStatusFTPListenerTest extends TestCase {
    private static final String TEST_DIR = "tmp";
    private final List filesToClear = new ArrayList();
    private MockCurrentBuildStatusFTPListener listener;

    class MockCurrentBuildStatusFTPListener extends CurrentBuildStatusFTPListener {
        private String text;
        private String expectedText;
        private String path;
        private String expectedPath;

        public void setExpectedText(String expectedText) {
            this.expectedText = expectedText;
        }

        public void setExpectedPath(String expectedPath) {
            this.expectedPath = expectedPath;
        }

        protected void sendFileToFTPPath(String text, String path) throws CruiseControlException {
            this.text = text;
            this.path = path;
        }

        void ensureFileSent() {
            Assert.assertEquals(expectedPath, path);
            Assert.assertEquals(expectedText, text);
        }
    }

    protected void setUp() throws Exception {
        listener = new MockCurrentBuildStatusFTPListener();
    }

    protected void tearDown() {
        listener = null;
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
        filesToClear.clear();
    }

    public void testValidate() throws CruiseControlException {
        try {
            listener.validate();
            fail("'file' should be a required attribute");
        } catch (CruiseControlException cce) {
        }

        listener.setFile("somefile");
        try {
            listener.validate();
            fail("'destdir' should be a required attribute");
        } catch (CruiseControlException cce) {
        }

        listener.setDestDir("destdir");
        listener.validate();
    }

    public void testWritingStatus() throws CruiseControlException, IOException {
        final String fileName = TEST_DIR + File.separator + "_testCurrentBuildStatus.txt";
        listener.setFile(fileName);
        listener.setDestDir("/pub");
        filesToClear.add(new File(fileName));

        checkResultForState(fileName, ProjectState.WAITING);
        checkResultForState(fileName, ProjectState.IDLE);
        checkResultForState(fileName, ProjectState.QUEUED);
        checkResultForState(fileName, ProjectState.BOOTSTRAPPING);
        checkResultForState(fileName, ProjectState.MODIFICATIONSET);
        checkResultForState(fileName, ProjectState.BUILDING);
        checkResultForState(fileName, ProjectState.MERGING_LOGS);
        checkResultForState(fileName, ProjectState.PUBLISHING);
        checkResultForState(fileName, ProjectState.PAUSED);
        checkResultForState(fileName, ProjectState.STOPPED);
    }

    private void checkResultForState(final String fileName, ProjectState state)
            throws CruiseControlException, IOException {
        // This should be equivalent to the date used in listener at seconds precision
        Date date = new Date();
        listener.handleEvent(new ProjectStateChangedEvent("projName", state));
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        final String dateString = formatter.format(date);
        final String description = state.getDescription();
        String expected = "<span class=\"link\">" + description + " since<br>" + dateString + "</span>";
        assertEquals(expected, readFileToString(fileName));

        listener.setExpectedPath("/pub" + File.separator + listener.getFileName());
        listener.setExpectedText(expected);
        listener.ensureFileSent();
    }

    // FIXME refactor into util Cf. FTPListener and ListenerTest
    private String readFileToString(String fileName) throws IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                contents.append(line);
                line = br.readLine();
            }
            return contents.toString();
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

}