package com.compumovil.aoap.arduinouno;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;

public class ControladorUsb {

	private final Context mApplicationContext;
	private final UsbManager mUsbManager;
	private final ConexionInterface mConnectionHandler;
	private final int VID;
	private final int PID;
	protected static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";

	public ControladorUsb(Activity parentActivity, ConexionInterface connectionHandler, int vid, int pid) {
		mApplicationContext = parentActivity.getApplicationContext();
		mConnectionHandler = connectionHandler;
		mUsbManager = (UsbManager) mApplicationContext.getSystemService(Context.USB_SERVICE);
		VID = vid;
		PID = pid;
		init();
	}

	private void init() {
		enumerate(new IPermissionListener() {
			@Override
			public void onPermissionDenied(UsbDevice d) {
				UsbManager usbman = (UsbManager) mApplicationContext
						.getSystemService(Context.USB_SERVICE);
				PendingIntent pi = PendingIntent.getBroadcast(
						mApplicationContext, 0, new Intent(
								ACTION_USB_PERMISSION), 0);
				mApplicationContext.registerReceiver(mPermissionReceiver,
						new IntentFilter(ACTION_USB_PERMISSION));
				usbman.requestPermission(d, pi);
			}
		});
	}

	public void stop() {
		mStop = true;
		synchronized (sSendLock) {
			sSendLock.notify();
		}
		try {
			if(mUsbThread != null)
				mUsbThread.join();
		} catch (InterruptedException ignored) {}
		mStop = false;
		mLoop = null;
		mUsbThread = null;
		
		try{
			mApplicationContext.unregisterReceiver(mPermissionReceiver);
		}catch(IllegalArgumentException ignored){}
    }

	private UsbRunnable mLoop;
	private Thread mUsbThread;

	private void startHandler(UsbDevice d) {
		if (mLoop != null) {
			return;
		}
		mLoop = new UsbRunnable(d);
		mUsbThread = new Thread(mLoop);
		mUsbThread.start();
	}

	public void send(byte data) {
		mData = data;
		synchronized (sSendLock) {
			sSendLock.notify();
		}
	}

	private void enumerate(IPermissionListener listener) {
		HashMap<String, UsbDevice> devlist = mUsbManager.getDeviceList();
        for (UsbDevice d : devlist.values()) {
            if (d.getVendorId() == VID && d.getProductId() == PID) {
                if (!mUsbManager.hasPermission(d))
                    listener.onPermissionDenied(d);
                else {
                    startHandler(d);
                    return;
                }
                break;
            }
        }
		mConnectionHandler.onDeviceNotFound();
	}

	private class PermissionReceiver extends BroadcastReceiver {
		private final IPermissionListener mPermissionListener;

		public PermissionReceiver(IPermissionListener permissionListener) {
			mPermissionListener = permissionListener;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			mApplicationContext.unregisterReceiver(this);
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				if (!intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					mPermissionListener.onPermissionDenied((UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE));
				} else {
					UsbDevice dev = intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (dev != null) {
						if (dev.getVendorId() == VID
								&& dev.getProductId() == PID) {
							startHandler(dev);
						}
					}
				}
			}
		}

	}


	private static final Object[] sSendLock = new Object[]{};
	private boolean mStop = false;
	private byte mData = 0x00;

	private class UsbRunnable implements Runnable {
		private final UsbDevice mDevice;
	
		UsbRunnable(UsbDevice dev) {
			mDevice = dev;
		}
	
		@Override
		public void run() {
			UsbDeviceConnection conn = mUsbManager.openDevice(mDevice);
			if (!conn.claimInterface(mDevice.getInterface(1), true)) {
				return;
			}

			conn.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
			conn.controlTransfer(0x21, 32, 0, 0, new byte[] { (byte) 0x80,
					0x25, 0x00, 0x00, 0x00, 0x00, 0x08 }, 7, 0);

            UsbEndpoint epOUT = null;
	
			UsbInterface usbIf = mDevice.getInterface(1);
			for (int i = 0; i < usbIf.getEndpointCount(); i++) {
				if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT){
                        epOUT = usbIf.getEndpoint(i);
                    }
				}
			}
	
			for (;;) {
				synchronized (sSendLock) {
					try {
						sSendLock.wait();
					} catch (InterruptedException e) {
						if (mStop) {
							return;
						}
						e.printStackTrace();
					}
				}
                if (epOUT != null) {
                    conn.bulkTransfer(epOUT, new byte[] { mData }, 1, 0);
                }

                if (mStop) {
					return;
				}
			}
		}
	}

	private BroadcastReceiver mPermissionReceiver = new PermissionReceiver(
			new IPermissionListener() {
				@Override
				public void onPermissionDenied(UsbDevice d) {}
			});

	private static interface IPermissionListener {
		void onPermissionDenied(UsbDevice d);
	}

}
