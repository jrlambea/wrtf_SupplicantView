package com.wardogstaskforce.supplicantview;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/***
 BEGIN LICENSE

 Copyright (C) 2014 Jose Ramón Lambea <jr_lambea@yahoo.com>
 This program is free software: you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License version 3, as published
 by the Free Software Foundation.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranties of
 MERCHANTABILITY, SATISFACTORY QUALITY, or FITNESS FOR A PARTICULAR
 PURPOSE.  See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program.  If not, see <http://www.gnu.org/licenses/>

 END LICENSE
 ***/

// principal class
public class MyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        populateListView();
        registerClickCallback();

    }

    // populate list of essids
    private void populateListView() {

        String[] ESSID = {};
        int i=0;

        try {
            /* copy the /data/misc/wifi/wpa_supplicant.conf with sudo to app internal storage and
               set read permissions.                                                              */
            try {
                Process p = Runtime.getRuntime().exec( new String[] {"/system/bin/su","-c","cp -p /data/misc/wifi/wpa_supplicant.conf " + getFilesDir() + "/"});
                p.waitFor();

                p = Runtime.getRuntime () .exec(new String[]{"/system/bin/su", "-c", "chmod 666 " + getFilesDir() + "/wpa_supplicant.conf"});
                p.waitFor ();

            } catch (IOException ex) {
                Toast.makeText (this,ex.getMessage () .toString() ,Toast.LENGTH_LONG) .show();
            } catch (InterruptedException ex) {
                Toast.makeText (this,ex.getMessage () .toString() ,Toast.LENGTH_LONG) .show();
            }

            // open copied wpa_supplicant.conf file.
            FileInputStream instream = openFileInput ("wpa_supplicant.conf");

            // if file is available for reading
            if (instream != null) {
                // prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader (instream);
                BufferedReader buffreader = new BufferedReader (inputreader);

                String line;

                // reads every line of file into the line-variable, one line at time
                do {
                    line = buffreader.readLine();

                    // if line contains "<tab>ssid=" obtain the essid
                    if ( line.contains("\tssid=") ){
                        ESSID = Arrays.copyOf(ESSID, ESSID.length + 1);
                        ESSID[i] = line.split("=")[1].substring(1,line.split("=")[1].length() -1);
                        i++;
                    }

                    // if line contains "<tab>key_mgmt=" obtain the encryption method
                    if ( line.contains("\tkey_mgmt=") ){
                        ESSID [i-1] = ESSID [i-1] + " [" + line.split("=")[1] + "]";
                    }

                } while (line != null);

                Toast.makeText(this, getString(R.string.str_loaded) + " " + Integer.toString(ESSID.length) + " ESSID.",Toast.LENGTH_LONG).show();

                // close the file.
                instream.close();

            }
        } catch (Exception ex) {
            // print stack trace.
            Toast.makeText(this, getString(R.string.str_loaded) + " " + Integer.toString(ESSID.length) + " ESSID.",Toast.LENGTH_LONG).show();
        } finally {

        }

        // Build Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.da_item, ESSID);


        // Configure the listview
        ListView list = (ListView) findViewById(R.id.listView);
        list.setAdapter(adapter);

    }

    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.listView);

        // action when clicks on an item of the list.
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                // determine which item has been clicked and show/copy the password to clipboard
                TextView textView = (TextView) viewClicked;
                String PASSW = getPASSWD(position);
                String message = getString(R.string.str_copy_1) + " " + PASSW + " " + getString(R.string.str_copy_2);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text",PASSW);

                clipboard.setPrimaryClip(clip);

                Toast.makeText(MyActivity.this,message,Toast.LENGTH_LONG).show();
            }
        });
    }

    // function to extract the password from the wpa_supplicant file
    private String getPASSWD(int position) {

        String rep = "None";
        int i = 0;

        FileInputStream instream = null;

        try {
            instream = openFileInput ("wpa_supplicant.conf");

            // if file the available for reading
            if (instream != null) {
                // prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader (instream);
                BufferedReader buffreader = new BufferedReader (inputreader);

                String line;

                // read every line of the file into the line-variable, one line at time
                do {
                    line = buffreader.readLine();

                    if ( line.contains("\tssid=") ){

                        i++;

                        if ( i == position + 1 ) {

                            line = buffreader.readLine();

                            while ( ( ! line.toString().startsWith("}") ) ){

                                if ( line.contains("\tpsk=" ) ){
                                    rep = line.split("=")[1].substring(1,line.split("=")[1].length() -1);
                                }else if ( line.contains("\tkey_mgmt=NONE") ){
                                    break;
                                }

                                line = buffreader.readLine();

                            }

                            break;
                        }
                    }

                    // do something with the line

                } while (line != null);

                // close the file.
                instream.close();

            }

        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(),Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(),Toast.LENGTH_LONG).show();
        }

        return rep;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.action_acercade:
                Dialog d = new Dialog(MyActivity.this);
                d.setContentView(R.layout.aboutdialog);
                d.setTitle(getString(R.string.action_acercade));
                d.show();

        }
        return true;
    }
}
