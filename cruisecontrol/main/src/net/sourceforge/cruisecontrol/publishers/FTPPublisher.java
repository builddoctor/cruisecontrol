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
import java.util.Vector;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * Publishes the XML log file and the published artifacts via FTP to
 * a remote host.  The ArtifactsPublisher must be listed before this
 * task in order to publish the artifacts.
 *
 * @author <a href="groboclown@users.sourceforge.net">Matt Albrecht</a>
 */
public class FTPPublisher extends AbstractFTPClass implements Publisher {

    private static final Logger LOG = Logger.getLogger(FTPPublisher.class);
    
    private String destdir;
    private String srcdir;
    private boolean deleteArtifacts = false;
    
    
    /**
     * The remote directory to put the artifacts into.
     */
    public void setDestDir(String dir) {
        this.destdir = dir;
    }
    
    
    public void setSrcDir(String dir) {
        this.srcdir = dir;
    }
    
    
    public void setDeleteArtifacts(String val) {
        if (val == null) {
            return;
        }
        val = val.toLowerCase();
        if ("true".equals(val) || "yes".equals(val)
                || "on".equals(val)) {
            this.deleteArtifacts = true;
        } else {
            this.deleteArtifacts = false;
        }
    }
    
    
    public void validate() throws CruiseControlException {
        if (destdir == null) {
            throw new CruiseControlException(
                "'destdir' is required for FTPPublisher");
        }
        if (srcdir == null) {
            throw new CruiseControlException(
                "'srcdir' is required for FTPPublisher");
        }
        super.validate();
    }
    
    
    
    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {

        // put the log files
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        String uniqueDir = helper.getBuildTimestamp();
        
        File logDir = new File(srcdir + File.separator + uniqueDir);
        
        Vector knownDirs = new Vector();
        FTPClient ftp = null;
        try {
            ftp = openFTP();
            
            // maybe text, maybe bin.  Set it to bin!
            setBinary(ftp);
            
            // get the log file
            String lFile1 = "log" + uniqueDir + ".xml";
            String lFile2 = "log" + uniqueDir + "L.xml";
            String lname = destdir + File.separator + lFile1;
            File lf = new File(srcdir + File.separator + lFile1);
            if (!lf.exists()) {
                lf = new File(srcdir + File.separator + lFile2);
            }
            if (lf.exists()) {
                makeDirsForFile(ftp, lname, knownDirs);
                sendFile(ftp, lf, lname);
                if (deleteArtifacts) {
                    lf.delete();
                }
            } else {
                LOG.info("Could not find build log file " + lf + ".");
            }
            
            // prevent exceptions if the directory doesn't exist.
            if (logDir.exists()) {
                LOG.info("Sending log dir " + logDir + ".");
                ftpDir(logDir, ftp, destdir, knownDirs);
                LOG.info("Done sending log dir " + logDir + ".");
            } else {
                LOG.info("Could not find artifacts directory " + logDir + ".");
            }
        } catch (CruiseControlException e) {
            LOG.error("FTPPublisher.publish()", e);
        } finally {
            closeFTP(ftp);
        }
    }
    
    
    private void ftpDir(File basedir, FTPClient ftp, String destdir,
            Vector knownDirs)
            throws CruiseControlException {
        String[] fileList = basedir.list();
        if (fileList == null) {
            return;
        }
        for (int i = 0; i < fileList.length; ++i) {
            String fname = destdir + File.separator + fileList[i];
            File f = new File(basedir, fileList[i]);
            if (f.exists()) {
                if (f.isDirectory()) {
                    // recursively add the file.
                    // Since we delete after this, removing the directory
                    // should work.
                    ftpDir(f, ftp, fname, knownDirs);
                } else  {
                    makeDirsForFile(ftp, fname, knownDirs);
                    sendFile(ftp, f, fname);
                }
                if (deleteArtifacts) {
                    f.delete();
                }
            }
        }
    }
}