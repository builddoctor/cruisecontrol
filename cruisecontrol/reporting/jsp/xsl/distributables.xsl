<?xml version="1.0"?>
<!--********************************************************************************
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
 ********************************************************************************-->

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>
    <xsl:variable name="tasklist" select="//target/task"/>
    <xsl:variable name="jar.tasklist" select="$tasklist[@name='Jar']"/>
    <xsl:variable name="war.tasklist" select="$tasklist[@name='War']"/>
    <xsl:variable name="dist.count" select="count($jar.tasklist) + count($war.tasklist)"/>

    <xsl:template match="/">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">

            <xsl:if test="$dist.count > 0">
                <tr>
                    <tr><td colspan="10">&#160;</td></tr>
                    <td bgcolor="#000066" colspan="10">
                        <font face="arial" size="2" color="#FFFFFF">
                            &#160;Deployments by this build:&#160;(
                             <xsl:value-of select="$dist.count"/>)
                        </font>
                    </td>
                </tr>
                <xsl:apply-templates select="$jar.tasklist | $war.tasklist" />
            </xsl:if>

        </table>
    </xsl:template>


    <!-- jar and war template -->
    <xsl:template match="task[@name='Jar'] | task[@name='War']">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>
            </xsl:if>
            <td colspan="10">
                <nobr>
                    <font face="arial" size="1"><xsl:value-of select="message"/></font>
                </nobr>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
