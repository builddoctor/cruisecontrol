@echo off

REM /********************************************************************************
REM  * CruiseControl, a Continuous Integration Toolkit                              *
REM  * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
REM  * 651 W Washington Ave. Suite 500                                              *
REM  * Chicago, IL 60661 USA                                                        *
REM  *                                                                              *
REM  * This program is free software; you can redistribute it and/or                *
REM  * modify it under the terms of the GNU General Public License                  *
REM  * as published by the Free Software Foundation; either version 2               *
REM  * of the License, or (at your option) any later version.                       *
REM  *                                                                              *
REM  * This program is distributed in the hope that it will be useful,              *
REM  * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
REM  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
REM  * GNU General Public License for more details.                                 *
REM  *                                                                              *
REM  * You should have received a copy of the GNU General Public License            *
REM  * along with this program; if not, write to the Free Software                  *
REM  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
REM  ********************************************************************************/

set CCDIR=d:\projects\cruisecontrol
set CCLIB=%CCDIR%\lib
set CRUISE_PATH=%CCDIR%\dist\cruisecontrol.jar;%CCLIB%\ant.jar;%CCLIB%\xerces.jar;%CCLIB%\mail.jar;%CCLIB%\optional.jar;%CCLIB%\junit.jar;%CCLIB%\activation.jar;.

set EXEC=java -cp %CRUISE_PATH% -Dccdir=%CCDIR% net.sourceforge.cruisecontrol.MasterBuild %1 %2 %3 %4 %5 %6
echo %EXEC%
%EXEC%