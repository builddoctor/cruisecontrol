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
package net.sourceforge.cruisecontrol.jmx;

import net.sourceforge.cruisecontrol.CruiseControlException;

public interface ProjectMBean {

    /**
     * Pauses the controlled project.
     */
    public void pause();

    /**
     * Resumes the controlled project.
     */
    public void resume();

    /**
     * Runs a build now
     */
    public void build();

    /**
     * Serialize the project
     */
    public void serialize();


//    /**
//     * Returns the duration the managed project process has been executing.
//     *
//     * @return Execution duration.
//     */
//    public long getUpTime();
//
//    /**
//     * Returns the number of successful builds performed by the managed
//     * project.
//     *
//     * @return Successful build count.
//     */
//    public long getSuccessfulBuildCount();

    /**
     * Is the project paused?
     * 
     * @return Pause state
     */
    public boolean isPaused();

    /**
     * Change the name of the config file that the Project should read
     * configuration information from
     *
     * @param fileName the name of the new config file
     */
    public void setConfigFileName(String fileName);

    public String getConfigFileName();

    /**
     * Change the Project label
     *
     * @param label a new label; should be valid for the current
     * LabelIncrementer
     */
    public void setLabel(String label);

    public String getLabel();

    /**
     * Change the Project label incrementer; will only succeed if the existing
     * label is valid for the new label incrementer
     *
     * @param classname fully qualified class name of the new label incrementer
     */
    public void setLabelIncrementer(String classname);

    /**
     * @return name of the current label incrementer
     */
    public String getLabelIncrementer();

    /**
     * Change the last built date.  This can be used to manipulate whether
     * builds will be initiated.
     *
     * @param date date string in the form yyyyMMddHHmmss
     */
    public void setLastBuild(String date) throws CruiseControlException;

    public String getLastBuild();

    /**
     * Change the last built date.  This can be used to manipulate whether
     * builds will be initiated.
     *
     * @param date date string in the form yyyyMMddHHmmss
     */
    public void setLastSuccessfulBuild(String date) throws CruiseControlException;

    public String getLastSuccessfulBuild();

    /**
     * Change the directory where CruiseControl logs are kept
     *
     * @param logdir Relative or absolute path to the log directory
     */
    public void setLogDir(String logdir) throws CruiseControlException;

    public String getLogDir();

    /**
     * Change the project name.  May cause problems if configuration file is
     * not also changed
     */
    public void setProjectName(String name);

    public String getProjectName();

    /**
     * Change the interval between builds
     *
     * @param buildInterval Build interval in milliseconds
     */
    public void setBuildInterval(long buildInterval);

    public long getBuildInterval();
    
    /**
     * Gets the human-readable version of the project status
     * @return
     */
    public String getStatus();
}