/*
 * Copyright (C) 2017 Ankit Sadana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boulder.igotthis

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boulder.igotthis.util.BaseLifecycleProvider
import com.boulder.igotthis.util.task.ActionType
import com.boulder.igotthis.util.task.EventType
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Class Description
 *
 * @author asadana
 * @since 12/24/17
 */

class TaskCreation(context: Context, viewGroupContainer: ViewGroup) : BaseLifecycleProvider(context, viewGroupContainer) {

    private val tag: String? = this.javaClass.name
    private val rootView: View

    // Event Elements
    private val eventDropDownSpinner: Spinner
    private val itemActionLayout: LinearLayout
    // Action Elements
    private val actionDropDownSpinner: Spinner
    private val addActionButton: ImageButton
    private val addEventActionButton: Button
    private val mResetEventActionButton: Button

    init {
        Log.d(tag, "init")
        rootView = LayoutInflater.from(context).inflate(R.layout.task_creation_layout, viewGroupContainer, false)
        eventDropDownSpinner = rootView.findViewById(R.id.event_drop_down_list)
        itemActionLayout = rootView.findViewById(R.id.item_action_select_layout)
        itemActionLayout.addView(View.inflate(context, R.layout.task_creation_item_action, null))
        actionDropDownSpinner = itemActionLayout.findViewById(R.id.item_drop_down_list)
        addActionButton = itemActionLayout.findViewById(R.id.item_action_add_action_button)
        addEventActionButton = rootView.findViewById(R.id.button_add)
        mResetEventActionButton = rootView.findViewById(R.id.button_reset)

        addActionButton.isEnabled = false   // TODO: Keep disabled till multiple actions can be linked to one event.
        addEventActionButton.isEnabled = false
        mResetEventActionButton.isEnabled = false
        addButtonListeners()
        populateEventSpinner()
    }

    private fun addButtonListeners() {
        addEventActionButton.setOnClickListener { view -> Log.d(tag, "setOnClickListener clicked for addEventActionButton" + view) }
    }

    private fun populateEventSpinner() {
        val eventDropDownList: MutableList<String> = mutableListOf()
        // TODO : make this dependent on the permissions given, otherwise disable it.
        EventType.values().mapTo(eventDropDownList) { context!!.getString(EventType.getStringResource(it)) }

        val eventDropDownAdapter: ArrayAdapter<String>? = ArrayAdapter(context, R.layout.drop_down_item, eventDropDownList)
        eventDropDownAdapter?.setDropDownViewResource(R.layout.drop_down_item)
        eventDropDownSpinner.adapter = eventDropDownAdapter
        eventDropDownSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (EventType.valueAt(position)) {
                    EventType.BLUETOOTH_CONNECTED,
                    EventType.BLUETOOTH_DISCONNECTED,
                    EventType.WIFI_CONNECTED,
                    EventType.WIFI_DISCONNECTED -> addActionView(EventType.valueAt(position))

                // TODO temporarily do nothing for charging urlConnectionected/disurlConnectionected.
                    EventType.CHARGING_CONNECTED,
                    EventType.CHARGING_DISCONNECTED -> itemActionLayout.removeAllViews()
                    else -> itemActionLayout.removeAllViews()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(context, "Nothing selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addActionView(eventType: EventType?) {
        val actionDropDownList: MutableList<String> = mutableListOf()
        // TODO : make this dependent on the permissions given, otherwise disable it.
        // TODO : make this dynamic so there's no duplicates from previous choices?
        for (actionType: ActionType in ActionType.values()) {
            actionDropDownList.add(context!!.getString(ActionType.getStringResource(actionType)))
        }
        val actionDropDownAdapter: ArrayAdapter<String>? = ArrayAdapter(context, R.layout.drop_down_item, actionDropDownList)
        actionDropDownAdapter?.setDropDownViewResource(R.layout.drop_down_item)

        actionDropDownSpinner.adapter = actionDropDownAdapter
        actionDropDownSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                when (ActionType.valueAt(position)) {
                    ActionType.TURN_BLUETOOTH_ON -> {
                        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
                        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled)
                            bluetoothAdapter.enable()
                    }
                    ActionType.TURN_BLUETOOTH_OFF -> {
                        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
                        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled)
                            bluetoothAdapter.disable()
                    }
                    ActionType.TURN_WIFI_ON -> {
                        val wifiManager: WifiManager? = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        if (wifiManager != null && !wifiManager.isWifiEnabled)
                            wifiManager.isWifiEnabled = true
                    }
                    ActionType.TURN_WIFI_OFF -> {
                        val wifiManager: WifiManager? = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        if (wifiManager != null && wifiManager.isWifiEnabled)
                            wifiManager.isWifiEnabled = false
                    }
                    ActionType.OPEN_APP -> {
                        val launchIntent: Intent? = context?.packageManager?.getLaunchIntentForPackage("com.waze")
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context?.startActivity(launchIntent)
                    }
                    ActionType.SEND_MESSAGE_USING_TELEGRAM -> {
                        // TODO
                    }
                    ActionType.PERFORM_CUSTOM_ACTION -> {
                        val performRequestAsyncTask = PerformRequestAsyncTask()
                        performRequestAsyncTask.execute("https://www.google.com", "GET")
                    }
                    else -> {
                        // TODO
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(context, "Nothing selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPause() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRootView() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inner class PerformRequestAsyncTask : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg params: String?): String {
            val urlString: String? = params[0]
            var resultToDisplay: String = ""
            var httpURLConnection: HttpURLConnection? = null

            try {

                var url = URL(urlString)
                httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.readTimeout = 15000
                httpURLConnection.connectTimeout = 15000
                httpURLConnection.requestMethod = params[1] ?: "GET"
                httpURLConnection.doInput = true
//                httpURLConnection.doOutput = true
/*

                                    OutputStream os = httpURLConnection.getOutputStream();
                                    BufferedWriter writer = new BufferedWriter(
                                            new OutputStreamWriter(os, "UTF-8"));
                                    writer.write(getPostDataString(postDataParams));

                                    writer.flush();
                                    writer.close();
                                    os.close();
*/

                val responseCode: Int? = httpURLConnection.responseCode
                Log.e(tag, "doInBackground: " + responseCode)

                //if (responseCode == HttpURLConnection.HTTP_OK) {

                val allText = httpURLConnection.inputStream.bufferedReader().use(BufferedReader::readText)

                /*} else {
                                resultToDisplay = "false : " + responseCode;
                            }*/

            }
            catch (error: MalformedURLException) {
                //Handles an incorrectly entered URL
                Log.e(tag, "doInBackground: ", error)
            }
            catch (error: SocketTimeoutException) {
                //Handles URL access timeout.
                Log.e(tag, "doInBackground: ", error)
            }
            catch (error: IOException) {
                //Handles input and output error
                Log.e(tag, "doInBackground: ", error)
            }
            finally {
                if (httpURLConnection != null) // Make sure the connection is not null.
                    httpURLConnection.disconnect()
            }

            return resultToDisplay
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
        }
    }
}
