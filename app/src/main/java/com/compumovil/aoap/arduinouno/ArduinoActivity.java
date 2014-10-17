package com.compumovil.aoap.arduinouno;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class ArduinoActivity extends Activity {

	private static final int VID = 0x2341;
	private static final int PID = 0x0043;
	private static ControladorUsb sControladorUsb;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if(sControladorUsb == null){
	        sControladorUsb = new ControladorUsb(this, mConnectionHandler, VID, PID);
        }
        ((SeekBar)(findViewById(R.id.nivel
        ))).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser){
					if(sControladorUsb != null){
						sControladorUsb.send((byte)(progress&0xFF));
					}
				}
			}
		});
        ((Button)findViewById(R.id.conectar)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(sControladorUsb == null)
					sControladorUsb = new ControladorUsb(ArduinoActivity.this, mConnectionHandler, VID, PID);
				else{
					sControladorUsb.stop();
					sControladorUsb = new ControladorUsb(ArduinoActivity.this, mConnectionHandler, VID, PID);
				}
			}
		});
        
    }

	private final ConexionInterface mConnectionHandler = new ConexionInterface() {

		@Override
		public void onDeviceNotFound() {
			if(sControladorUsb != null){
				sControladorUsb.stop();
				sControladorUsb = null;
			}
		}
	};
}