/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.*;
import org.apache.tools.ant.*;

/**
 * This abstract class defines behavior required by ModificationSet.java when
 * gathering information about the changes made to whatever source control tool
 * that you choose.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">Alden Almagro</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */

//(PENDING) pull up buildCommandLine
public abstract class SourceControlElement {

	/**
	 *  Get a Set of email addresses. Depends on the source control tool. StarTeam
	 *  has a field for email addresses, so we would return a set of full email
	 *  addresses here. SourceSafe doesn't have the same functionality, so we'll
	 *  just return the usernames here (which should correspond to email IDs).
	 *  We'll tack on the suffix, e.g., "@apache.org" in MasterBuild.java before 
     *  mailing results of the build.
	 *
	 *@return
	 *
	 */
	public abstract Set getEmails();

	/**
	 *  Get a List of Modifications detailing all the changes between now and
	 *  the last build.
	 *
	 *@param  lastBuild
	 *@param  now
	 *@param  quietPeriod
	 *@return
	 */
	public abstract List getHistory(Date lastBuild, Date now, long quietPeriod);

	/**
	 *  Get the last modified time for this set of files
	 *
	 *@return
	 */
	public abstract long getLastModified();    

}
