/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;

import org.apache.tools.ant.Project;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * @author Jeffrey Fredrick
 */
public class ArtifactsPublisherTest extends TestCase {
    
    private ArtifactsPublisher publisher;
    private File tempFile;
    
    protected void setUp() throws Exception {
        publisher = new ArtifactsPublisher();
        tempFile = File.createTempFile("temp", ".tmp");
    }

    protected void tearDown() throws Exception {
        publisher = null;
        if (tempFile.exists()) {
            tempFile.delete();
        }
        tempFile = null;
    }    
    
    public void testPublishDirectory() {
        File tempDir = tempFile.getParentFile();
        Project project = new Project();
        publisher.setDir(tempFile.getAbsolutePath());
        try {
            publisher.publishDirectory(project, tempDir);
            fail();
        } catch (CruiseControlException expected) {
            String message = expected.getMessage();
            assertTrue(message.startsWith("target directory "));
        }
    }

    public void testPublishFile() {
        File tempDir = tempFile.getParentFile();
        publisher.setFile(tempFile.getName());
        try {
            publisher.publishFile(tempDir);
            fail();
        } catch (CruiseControlException expected) {
            String message = expected.getMessage();
            assertTrue(message.startsWith("target file "));
        }
    }

    public void testValidate() {
        try {
            publisher.validate();
            fail();
        } catch (CruiseControlException expected) {
            assertNotNull(expected);
        }
       
        publisher.setDest("foo");
        publisher.setDir("bar");
        publisher.setFile("baz");
        
        try {
            publisher.validate();
            fail();
        } catch (CruiseControlException expected) {
            assertNotNull(expected);
        }
        
        publisher.setFile(null);
        try {
            publisher.validate();
        } catch (CruiseControlException notExpected) {
            fail();
        }

        publisher.setDir(null);
        publisher.setFile("baz");
        try {
            publisher.validate();
        } catch (CruiseControlException notExpected) {
            fail();
        }

        publisher.setDest(null);
        try {
            publisher.validate();
            fail();
        } catch (CruiseControlException expected) {
            assertNotNull(expected);
        }        
    }
}