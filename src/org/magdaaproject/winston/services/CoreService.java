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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.magdaaproject.utils.FileUtils;
import org.magdaaproject.utils.SensorUtils;
import org.magdaaproject.winston.R;

import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
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
	private static final boolean sVerboseLog = true;
	private static final String sLogTag = "CoreService";

	private static final int sTransmitPin = 5;
	private static final int sReceivePin  = 6;

	private static final int sBaudRate = 19200;
	
	private static final int sDataPacketLength = 100; // bytes
	private static final String sLoopCommand = "LOOP 1\n";
	
	// sleep time between checking for new data
	private static final int sSleepTime = 1000;

	/*
	 * private class level variables
	 */

	// identify if the service is running or not
	private static boolean isRunning = false;
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onCreate() called");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		super.onStartCommand(intent,  flags, startId);

		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onStartCommand() called");
		}
		
		// set the is running flag
		isRunning = true;

		// return the start sticky flag
		return android.app.Service.START_STICKY;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		// output verbose debug log info
		if(sVerboseLog) {
			Log.v(sLogTag, "service onDestroy() called");
		}
		
		// set the is running flag
		isRunning = false;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * check to see if this service is running or now
	 * @return true if the service is running
	 */
	public static boolean isRunning() {
		return isRunning;
	}

	/*
	 * ioio related code
	 */

	/*
	 * a private looper inner class for responding to button events
	 */
	private class Looper extends BaseIOIOLooper {

		/*
		 * define class level variables
		 */
		private Uart uartModule;
		private InputStream  inputStream;
		private OutputStream outputStream;
		
		private byte[] loopDataPacket;
		
		/*
		 * (non-Javadoc)
		 * @see ioio.lib.util.BaseIOIOLooper#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {

			try {

				// setup the uart connection
				uartModule = ioio_.openUart(sReceivePin, sTransmitPin, sBaudRate, Uart.Parity.NONE, Uart.StopBits.ONE);
				
				inputStream = uartModule.getInputStream();
				outputStream = uartModule.getOutputStream();

				// TODO send the sensor status broadcast
				//sendSensorStatusBroadcast(true);

			} catch (ConnectionLostException e) {

				//TODO send the sensor status broadcast
				//sendSensorStatusBroadcast(false);

				Log.e(sLogTag, "connection to ioio lost during setup", e);
				throw e;
			} 

			// output verbose debug log info
			if(sVerboseLog) {
				Log.v(sLogTag, "ioio looper setup() called");
			}
		}

		/*
		 * (non-Javadoc)
		 * @see ioio.lib.util.BaseIOIOLooper#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException {
			
			// output some debug info
			if(sVerboseLog) {
				Log.v(sLogTag, "started the loop");
			}
			
			try {
				
				// wake up the console
				outputStream.write(new String("\n").getBytes("ASCII"));
				outputStream.flush();

				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "written the wakeup");
				}
				
				// let the console process the command
				Thread.sleep(sSleepTime * 2);
				
				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "finished snooze");
				}
				
				// read the response
				byte[] mBytes = new byte[2];
				int mRead = 0;
				
				while(mRead < mBytes.length) {
					int returnVal = inputStream.read(mBytes, mRead, mBytes.length - mRead);
					
					// output some debug info
					if(sVerboseLog) {
						Log.v(sLogTag, "return val: '" + returnVal + "'");
					}
					
					if(returnVal < 0) {
						break;
					} else {
						// output some debug info
						if(sVerboseLog) {
							for(int i = mRead; i < mRead+returnVal; i++) {
								Log.v(sLogTag, "in loop byte: " + i + " = '" + (mBytes[i]&0xFF) + "'");
							}
						}
						mRead += returnVal;
					}
				}
				
				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "result of command: '" + new String(mBytes, "ASCII") + "'");
				}
				
//				// send the test command
//				outputStream.write(new String("TEST\n").getBytes("ASCII"));
//				outputStream.flush();
//				
//				// debug code
//				Log.v(sLogTag, "written the test command");
//				
//				// let the console process the command
//				Thread.sleep(sSleepTime * 2);
//				
//				Log.v(sLogTag, "finished snooze");
//				
//				mBytes = new byte[8];
//				mRead = 0;
//				
//				while(mRead < mBytes.length) {
//					int returnVal = inputStream.read(mBytes, mRead, mBytes.length - mRead);
//					
//					Log.v(sLogTag, "return val: '" + returnVal + "'");
//					
//					if(returnVal < 0) {
//						break;
//					} else {
//						for(int i = mRead; i < mRead+returnVal; i++) {
//							Log.v(sLogTag, "in loop byte: " + i + " = '" + (mBytes[i]&0xFF) + "'");
//						}
//						mRead += returnVal;
//					}
//				}
//				
//				// output the result of the command
//				Log.v(sLogTag, "result of command: '" + new String(mBytes, "ASCII") + "'");
//				
				// send the test command
				outputStream.write(new String(sLoopCommand).getBytes("ASCII"));
				outputStream.flush();
				
				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "written the loop command");
				}
				
				// let the console process the command
				Thread.sleep(sSleepTime * 2);
				
				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "finished snooze");
				}
				
				// reset the variables and read the returned data
				loopDataPacket = new byte[sDataPacketLength];
				mRead = 0;
				
				while(mRead < mBytes.length) {
					int returnVal = inputStream.read(loopDataPacket, mRead, loopDataPacket.length - mRead);
					
					// output some debug info
					if(sVerboseLog) {
						Log.v(sLogTag, "return val: '" + returnVal + "'");
					}
					
					if(returnVal < 0) {
						break;
					} else {
						// output some debug info
						if(sVerboseLog) {
							// log contents of array at this point
							for(int i = mRead; i < mRead+returnVal; i++) {
								Log.v(sLogTag, "in loop byte: " + i + " = '" + (loopDataPacket[i]&0xFF) + "'");
							}
						}
						mRead += returnVal;
					}
				}
				
				// output some debug info
				if(sVerboseLog) {
					Log.v(sLogTag, "data packet received");
					try {
						String mOutputPath = Environment.getExternalStorageDirectory().getPath();
						mOutputPath += getString(R.string.system_file_path_debug_output);
								
						Log.v(sLogTag, "data packet saved to: '" + FileUtils.writeTempFile(loopDataPacket, mOutputPath) + "'");
					} catch (IOException e) {
						Log.v(sLogTag, "unable to write temp file", e);
					}
					
				}
				
				// TODO process the data packet
				
				// debug code
				stopSelf();
			} catch (IOException e) {
				Log.e(sLogTag, "IOException thrown when waking up the console");
				return;
			} 
			catch (InterruptedException e) {
				Log.w(sLogTag, "thread interrupted while sleeping during console wakeup");
				return;
			}

		}

		/*
		 * (non-Javadoc)
		 * @see ioio.lib.util.BaseIOIOLooper#disconnected()
		 */
		@Override
		public void disconnected() {
			// TODO send the sensor status broadcast
			//sendSensorStatusBroadcast(false);
			
			if(sVerboseLog) {
				Log.v(sLogTag, "disconnected from IOIO");
			}
		}

	}
	
	/*
	 * (non-Javadoc)
	 * @see ioio.lib.util.android.IOIOService#createIOIOLooper()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

}
