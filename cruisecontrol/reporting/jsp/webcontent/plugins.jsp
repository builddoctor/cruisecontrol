<%--********************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2005 ThoughtWorks, Inc.
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
********************************************************************************--%>
<%@ page errorPage="/error.jsp" %>
<%@ taglib uri="webwork" prefix="ww" %>
<html>
<head>
    <title><ww:property value="project"/> <ww:property value="pluginType"/></title>
    <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
    <script type="text/javascript" language="JavaScript" src="prototype.js"></script>
    <script type="text/javascript" language="JavaScript" src="cc-config.js"></script>
</head>

<body>
<h3><ww:property value="listType"/>:</h3>
<ul>
<ww:iterator value="plugins">
    <li><a href="<ww:url value="details!default.jspa?pluginName=%{name}"/>"
       onclick="loadPlugin(this.href); return false;">
        <ww:property value="name"/></a></li>
</ww:iterator>
</ul>
</body>
</html>
