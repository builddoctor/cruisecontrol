/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.jdom.Element;

public class ArtifactsPublisher implements Publisher {

    private Copy copier = new Copy();

    private String destDir;
    private String targetDirectory;
    private String targetFile;

    public void setDest(String dir) {
        destDir = dir;
    }

    public void setDir(String pDir) {
        targetDirectory = pDir;
    }

    public void setFile(String file) {
        targetFile = file;
    }

    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        Project project = new Project();
        String uniqueDir = helper.getBuildTimestamp();
        File uniqueDest = new File(destDir, uniqueDir);
        
        if (targetDirectory != null) {
            publishDirectory(project, uniqueDest);
        }
        if (targetFile != null) {
            publishFile(uniqueDest);
        }

    }

    void publishFile(File uniqueDest) throws CruiseControlException {
        File file = new File(targetFile);
        if (!file.exists()) {
            throw new CruiseControlException("target file " + file.getAbsolutePath() + " does not exist");
        }
        FileUtils utils = FileUtils.newFileUtils();
        try {
            utils.copyFile(file, new File(uniqueDest, targetFile));
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    void publishDirectory(Project project, File uniqueDest) throws CruiseControlException {
        File directory = new File(targetDirectory);
        if (!directory.exists()) {
            throw new CruiseControlException("target directory " + directory.getAbsolutePath() + " does not exist");
        }
        if (!directory.isDirectory()) {
            throw new CruiseControlException("target directory " + directory.getAbsolutePath() + " is not a directory");
        }
        FileSet set = new FileSet();
        set.setDir(directory);
        copier.addFileset(set);
        copier.setTodir(uniqueDest);
        copier.setProject(project);
        try {
            copier.execute();
        } catch (Exception e) {
            throw new CruiseControlException(e);
        }
    }

    public void validate() throws CruiseControlException {
        if (destDir == null) {
            throw new CruiseControlException("'destdir' not specified in configuration file.");
        }

        if (targetDirectory == null && targetFile == null) {
            throw new CruiseControlException("'dir' or 'file' must be specified in configuration file.");
        }
        
        if (targetDirectory != null && targetFile != null) {
            throw new CruiseControlException("only one of 'dir' or 'file' may be specified.");
        }
    }
}