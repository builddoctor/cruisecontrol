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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.AntBuilder;

public class PluginRegistryTest extends TestCase {

    public void testCreatingRegistry() {
        PluginRegistry registry = new PluginRegistry();

        //The registry should be empty...
        assertFalse(registry.isPluginRegistered("foo"));
        assertFalse(registry.isPluginRegistered("ant"));
        assertFalse(registry.isPluginRegistered("labelincrementer"));
        assertFalse(registry.isPluginRegistered("cvs"));
    }

    public void testGettingPluginClass() throws Exception {
        PluginRegistry registry = PluginRegistry.getDefaultPluginRegistry();

        assertNotNull(registry.getPluginClass("ant"));
    }

    public void testRegisteringPluginNoClass() {
        PluginRegistry registry = new PluginRegistry();

        final String nonExistentClassname =
                "net.sourceforge.cruisecontrol.Foo" + System.currentTimeMillis();
        registry.register("foo", nonExistentClassname);
        try {
            registry.getPluginClass("foo");
            fail("Expected an exception when getting a plugin"
                    + " class that isn't loadable.");
        } catch (CruiseControlException expected) {
        }
    }

    public void testAddingPlugin() throws Exception {
        PluginRegistry registry = new PluginRegistry();

        //Add a plugin with a classname that exists
        final Class antBuilderClass = AntBuilder.class;
        final String antBuilderClassname = antBuilderClass.getName();
        registry.register("ant", antBuilderClassname);

        //It should be registered
        assertTrue(registry.isPluginRegistered("ant"));
        assertEquals(antBuilderClassname, registry.getPluginClassname("ant"));
        assertEquals(antBuilderClass, registry.getPluginClass("ant"));
    }

    public void testCaseInsensitivityPluginNames() throws Exception {
        //Plugin names are treated as case-insensitive by CruiseControl, so
        //  a plugin named Ant and ant are the same.
        PluginRegistry registry = new PluginRegistry();

        //Add a plugin with an all lowercase name
        final String antBuilderClassname = AntBuilder.class.getName();
        registry.register("ant", antBuilderClassname);

        //It should be registered
        assertTrue(registry.isPluginRegistered("ant"));

        //If we ask if other case versions are registered, it should
        //  say "yes"
        assertTrue(registry.isPluginRegistered("ANT"));
        assertTrue(registry.isPluginRegistered("Ant"));
        assertTrue(registry.isPluginRegistered("aNT"));
        assertTrue(registry.isPluginRegistered("aNt"));
    }

    public void testPluginRegistry() throws Exception {

        verifyPluginClass(
                "currentbuildstatusbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper");
        verifyPluginClass(
                "cvsbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.CVSBootstrapper");
        verifyPluginClass(
                "p4bootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.P4Bootstrapper");
        verifyPluginClass(
                "vssbootstrapper",
                "net.sourceforge.cruisecontrol.bootstrappers.VssBootstrapper");
        verifyPluginClass("clearcase", "net.sourceforge.cruisecontrol.sourcecontrols.ClearCase");
        verifyPluginClass("cvs", "net.sourceforge.cruisecontrol.sourcecontrols.CVS");
        verifyPluginClass("filesystem", "net.sourceforge.cruisecontrol.sourcecontrols.FileSystem");
        verifyPluginClass("httpfile", "net.sourceforge.cruisecontrol.sourcecontrols.HttpFile");
        verifyPluginClass("mks", "net.sourceforge.cruisecontrol.sourcecontrols.MKS");
        verifyPluginClass("p4", "net.sourceforge.cruisecontrol.sourcecontrols.P4");
        verifyPluginClass("pvcs", "net.sourceforge.cruisecontrol.sourcecontrols.PVCS");
        // skipped because not everyone has starteam api jar
        // verifyPluginClass("starteam", "net.sourceforge.cruisecontrol.sourcecontrols.StarTeam");
        verifyPluginClass("vss", "net.sourceforge.cruisecontrol.sourcecontrols.Vss");
        verifyPluginClass("vssjournal", "net.sourceforge.cruisecontrol.sourcecontrols.VssJournal");
        verifyPluginClass("compound", "net.sourceforge.cruisecontrol.sourcecontrols.Compound");
        verifyPluginClass("triggers", "net.sourceforge.cruisecontrol.sourcecontrols.Triggers");
        verifyPluginClass("targets", "net.sourceforge.cruisecontrol.sourcecontrols.Targets");
        verifyPluginClass("ant", "net.sourceforge.cruisecontrol.builders.AntBuilder");
        verifyPluginClass("maven", "net.sourceforge.cruisecontrol.builders.MavenBuilder");
        verifyPluginClass("pause", "net.sourceforge.cruisecontrol.PauseBuilder");
        verifyPluginClass(
                "labelincrementer",
                "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer");
        verifyPluginClass(
                "artifactspublisher",
                "net.sourceforge.cruisecontrol.publishers.ArtifactsPublisher");
        verifyPluginClass(
                "currentbuildstatuspublisher",
                "net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher");
        verifyPluginClass("email", "net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher");
        verifyPluginClass(
                "htmlemail",
                "net.sourceforge.cruisecontrol.publishers.HTMLEmailPublisher");
        verifyPluginClass("execute", "net.sourceforge.cruisecontrol.publishers.ExecutePublisher");
        verifyPluginClass("scp", "net.sourceforge.cruisecontrol.publishers.SCPPublisher");
        verifyPluginClass("modificationset", "net.sourceforge.cruisecontrol.ModificationSet");
        verifyPluginClass("schedule", "net.sourceforge.cruisecontrol.Schedule");
        verifyPluginClass("buildstatus", "net.sourceforge.cruisecontrol.sourcecontrols.BuildStatus");
        verifyPluginClass("clearcasebootstrapper", "net.sourceforge.cruisecontrol.bootstrappers.ClearCaseBootstrapper");
        verifyPluginClass("xsltlogpublisher", "net.sourceforge.cruisecontrol.publishers.XSLTLogPublisher");
    }

    static void verifyPluginClass(String pluginName, String expectedName)
            throws Exception {
        PluginRegistry registry = PluginRegistry.getDefaultPluginRegistry();

        assertTrue(registry.isPluginRegistered(pluginName));

        String className = registry.getPluginClassname(pluginName);
        assertEquals(expectedName, className);

        Class pluginClass = Class.forName(className);
        pluginClass.getConstructor(null).newInstance(null);
    }
}
