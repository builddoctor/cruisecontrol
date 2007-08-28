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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import org.apache.commons.lang.StringUtils;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class BuildDetailTest extends BaseFunctionalTest {

    protected void onSetUp() throws Exception {
        setConfigFileAndSubmitForm(DataUtils.getConfigXmlOfWebApp().getPath());
    }

    public void testShouldShowArtifactsForFailedBuild() {
        tester.beginAt("/build/detail/project1/" + DataUtils.FAILING_BUILD_XML);
        tester.assertTextPresent("Artifacts");
        tester.assertTextPresent("Merged Check Style");
        tester.assertTextPresent("Line has trailing spaces.");
        tester.assertLinkPresentWithExactText("#123");
    }

    public void testShouldNotShowArtifactsIfNoPublishersInConfigFile() throws Exception {
        tester.beginAt("/build/detail/projectWithoutPublishers/log20060704155755Lbuild.490.xml");
        tester.assertLinkNotPresentWithText("artifact.txt");
    }

    public void testShouldNotShowArtifactsForProjectsWithoutConfiguration() throws Exception {
        tester.beginAt("/build/detail/projectWithoutConfiguration/log20060704155710Lbuild.489.xml");
        tester.assertLinkNotPresentWithText("artifact.txt");
    }

    public void testShouldBeAbleToOpenProjectNameWithSpace() throws Exception {
        tester.beginAt("/build/detail/project%20space");
        tester.assertTextPresent("project space");
    }

    public void testShouldShowDurationFromLastSuccessfulBuild() throws Exception {
        tester.beginAt("/build/detail/projectWithoutConfiguration/log20060704160010.xml");
        tester.assertTextPresent("Last successful build");
        tester.assertTextPresent("minutes");
        tester.assertTextPresent("ago");
    }

    public void testShouldShowErrorsAndWarningTabAndBuildErrorMessages() throws Exception {
        tester.beginAt("/build/detail/project1/log20051209122104.xml");
        tester.assertTextPresent("Errors and Warnings");
        tester.assertTextPresent("Detected OS: Windows XP");
        tester.assertTextPresent("Cannot find something");
        tester.assertTextPresent("Build Error Message");
        tester.assertTextPresent("This is my error message");

        tester.assertTextPresent("Stacktrace");
        tester.assertTextPresent("This is my stacktrace");
    }

    public void testShouldEscapeSpecialCharacterInJUnitMessages() throws Exception {
        tester.beginAt("/build/detail/project1/log20051209122104.xml");
        assertTrue(StringUtils.contains(tester.getPageSource(), "expected:&lt;1&gt; but was:&lt;2&gt;"));
    }

    public void testShouldEscapeSpecialCharacterinCommitMessages() throws Exception {
        tester.beginAt("/build/detail/project1/log20051209122104.xml");
        assertTrue(StringUtils.contains(tester.getPageSource(),
                "project name changed to &lt;b&gt;cache&lt;/b&gt;"));
    }

    public void testShouldShowDefaultMessagewhenNoErrors() throws Exception {
        tester.beginAt("/build/detail/project1/log20051209122103Lbuild.489.xml");
        tester.assertTextPresent("No error message");
        tester.assertTextPresent("No stacktrace");
        tester.assertTextPresent("No errors or warnings");
    }

    public void testShouldListGZippedLogs() throws Exception {
        tester.beginAt("/build/detail/project3");
        tester.assertTextPresent("9 Dec 2007 12:21 GMT");
        tester.assertTextPresent("9 Dec 2006 12:21 GMT");
        tester.assertTextPresent("build.489");
        tester.assertTextPresent("9 Nov 2006 12:21 GMT");
        tester.assertTextPresent("9 Dec 2005 12:21 GMT");
    }
}