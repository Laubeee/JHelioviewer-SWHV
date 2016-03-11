package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.jhv.base.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Wrapper for JSONObject.
 *
 * The idea behind this class is simply to get a better name and to prevent the
 * user from handling all the JSONExceptions.
 *
 * @author Markus Langenberg
 */
class APIResponse {
    /**
     * Complete answer
     */
    private JSONObject data;
    /**
     * URI of the response
     */
    private URI uri;

    /**
     * Constructor with a reader as source
     *
     * @param source
     *            - Reader from which the JSON object will be read
     */
    public APIResponse(Reader source) {
        try {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(source);

                String b;
                while ((b = br.readLine()) != null) {
                    sb.append(b);
                    Log.debug("answer : " + b);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // data = new JSONObject(new JSONTokener(source));
            data = new JSONObject(new JSONTokener(new StringReader(sb.toString())));
            if (!data.isNull("uri")) {
                uri = new URI(data.getString("uri"));
            }
        } catch (JSONException e) {
            Log.error("Invalid JSON response " + data, e);
        } catch (URISyntaxException e) {
            Log.error("Invalid uri in response " + data, e);
        }
    }

    /**
     * Returns the value for a given key.
     *
     * Returns null if the key does not exist or the value is not a string.
     *
     * @param key
     *            Key to search for
     * @return value for given key
     */
    public String getString(String key) {
        try {
            return data.getString(key);
        } catch (JSONException e) {
        }
        return null;
    }

    /**
     * Returns the URI of the image which results from this API response.
     *
     * The URI is used as the unique identifier for the response.
     *
     * @return unique URI corresponding to this response
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Checks if a JSON object could be created
     *
     * @return true if a valid JSON object has been created
     */
    public boolean hasData() {
        return data != null;
    }

}
