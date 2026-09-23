package com.pocketpad.remote;
import org.json.JSONObject;
interface Transport { void send(JSONObject event); void close(); boolean connected(); }
