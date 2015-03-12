/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.util.ArrayList;
import java.util.List;

public class VagvalAgentProcessingLog {

	boolean isRefreshRequested = false;
	boolean isRefreshSuccessful = false; 
    List<String> logBuffer = null;    

    public VagvalAgentProcessingLog() {
        logBuffer = new ArrayList<String>();
    }

    public void addLog(String log) {
        logBuffer.add(log);
    }

    public List<String> getLog() {
        return logBuffer;
    }
}
