/*
 * Copyright (C) 2012 The MaGDAA Project
 *
 * This file is part of the MaGDAA WInSTON Software
 *
 * MaGDAA WInSTON Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.magdaaproject.winston.services;

import android.content.Intent;
import android.os.IBinder;
import ioio.lib.util.android.IOIOService;

/**
 * 
 * the CoreService class controls access to the IOIO hardware, retrieving new weather information values, 
 * storing new values in the database and sending the relevant broadcasts
 */
public class CoreService extends IOIOService {
	
	/*
	 * private class level constants
	 */
	private static final boolean sVerboseLog = false;
	private static final String sLogTag = "CoreService";
	
	/*
	 * private class level variables
	 */
	
	
	// identify if the service is running or not
	private static boolean isRunning = false;

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * check to see if this service is running or now
	 * @return true if the service is running
	 */
	public static boolean isRunning() {
		return isRunning;
	}

}
