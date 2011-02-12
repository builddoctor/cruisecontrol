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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommand;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommandline;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.Runner;

/**
 * Simply runs "accurev update" to update the current workspace. Automatic keep and synctime are
 * provided as options.
 *
 * @author <a href="mailto:jason_chown@scee.net">Jason Chown</a>
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 */
@Description(
        "Automatically updates an accurev workspace. The selected workspace must "
        + "already exist on the local filesystem.")
public class AccurevBootstrapper implements Bootstrapper {

    private static final long serialVersionUID = -8790465364314947316L;

    private boolean verbose;
    private boolean keep;
    private boolean synctime;
    private String workspace;
    private Runner runner;

    @Description("Enables detailed logging.")
    @Optional
    @Default("false")
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Description(
            "If true, the plugin runs \"accurev keep -m\" before trying to update "
            + "the workspace, to keep all modified elements.")
    @Optional
    @Default("false")
    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    @Description(
            "If true, the plugin runs \"accurev synctime\" before trying to update "
            + "the workspace, to synchronize the local clock with the Accurev server's.")
    @Optional
    @Default("false")
    public void setSynctime(boolean synctime) {
        this.synctime = synctime;
    }

    @Description("The local path containing the working copy of the desired workspace.")
    @Optional
    @Default("Current working directory")
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    private void runAccurev(AccurevCommandline cmd) throws CruiseControlException {
        if (runner != null) {
            cmd.setRunner(runner);
        }
        cmd.setWorkspaceLocalPath(workspace);
        cmd.setVerbose(verbose);
        cmd.run();
        cmd.assertSuccess();
    }

    /**
     * Runs the bootstrapper: updates the selected workspace. If required, it runs synctime and keep
     * before updating.
     */
    public void bootstrap() throws CruiseControlException {
        if (synctime) {
            runAccurev(AccurevCommand.SYNCTIME.create());
        }
        if (keep) {
            AccurevCommandline cmdKeep = AccurevCommand.KEEP.create();
            cmdKeep.selectModified();
            cmdKeep.setComment("CruiseControl automatic keep");
            runAccurev(cmdKeep);
        }
        runAccurev(AccurevCommand.UPDATE.create());
    }

    @SkipDoc
    public void setRunner(Runner runner) {
        this.runner = runner;
    }

    public void validate() throws CruiseControlException {
        // @todo Should something be validated here?
    }

}
