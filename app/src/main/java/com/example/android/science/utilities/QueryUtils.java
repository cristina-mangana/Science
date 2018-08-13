package com.example.android.science.utilities;

import android.text.TextUtils;
import android.util.Log;

import com.example.android.science.model.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by Cristina on 09/08/2018.
 * Helper methods related to requesting and receiving API data.
 */
public class QueryUtils {

    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    /**
     * Query the data set and return a List of {@link Question}.
     */
    public static List<Question> fetchQuestionsListData(String requestUrl) {

        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request", e);
        }

        // Extract relevant fields from the JSON response and return it
        return extractListDataFromJson(jsonResponse);
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpsURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setSSLSocketFactory(new MySSLSocketFactory(urlConnection.getSSLSocketFactory()));
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the whole JSON response from
     * the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
                    Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a list of {@link Question} objects that has been built up from parsing a JSON response.
     */
    private static List<Question> extractListDataFromJson(String stringJSON) {
        /* Key value for the "response_code" integer.*/
        final String RESPONSE_CODE_KEY = "response_code";

        /* Key value for the "results" array.*/
        final String RESULTS_KEY = "results";

        /* Key value for the cake "question" string.*/
        final String QUESTION_KEY = "question";

        /* Key value for the "correct_answer" string.*/
        final String CORRECT_KEY = "correct_answer";

        /* Key value for the "incorrect_answers" array.*/
        final String INCORRECT_KEY = "incorrect_answers";

        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(stringJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding questions to
        List<Question> questions = new ArrayList<>();
        try {
            JSONObject response = new JSONObject(stringJSON);
            // Check the response code
            if (response.has(RESPONSE_CODE_KEY)) {
                switch (response.getInt(RESPONSE_CODE_KEY)) {
                    case 0:
                        Log.d(LOG_TAG, "Returned results successfully");
                        break;
                    case 1:
                        Log.d(LOG_TAG, "The API doesn't have enough questions for the query");
                        break;
                    case 2:
                        Log.d(LOG_TAG, "Query contains an invalid parameter");
                        break;
                    case 3:
                        Log.d(LOG_TAG, "Session Token does not exist");
                        break;
                    case 4:
                        Log.d(LOG_TAG, "Session Token has returned all questions");
                        break;
                }
            }
            if (response.has(RESULTS_KEY)) {
                JSONArray results = response.getJSONArray(RESULTS_KEY);
                // Loop through each feature in the array
                for (int i = 0; i < results.length(); i++) {
                    // Create an empty Question Object so that we can start adding information about it
                    Question question = new Question();

                    // Get the question JSONObject at position i
                    JSONObject questionObject = results.getJSONObject(i);

                    // Extract question title
                    if (questionObject.has(QUESTION_KEY)) {
                        String questionTitle = questionObject.getString(QUESTION_KEY);
                        question.setQuestionTitle(questionTitle);
                    }

                    // Extract correct answer
                    if (questionObject.has(CORRECT_KEY)) {
                        String correctAnswer = questionObject.getString(CORRECT_KEY);
                        question.setCorrectAnswer(correctAnswer);
                    }

                    // Extract incorrect answers array
                    if (questionObject.has(INCORRECT_KEY)) {
                        JSONArray incorrectAnswers = questionObject.getJSONArray(INCORRECT_KEY);
                        List<String> incorrectArray = new ArrayList<>();
                        for (int j = 0; j < incorrectAnswers.length(); j++) {
                            incorrectArray.add(incorrectAnswers.getString(j));
                        }
                        question.setIncorrectAnswers(incorrectArray);
                    }
                    // Add question Object to list of questions
                    questions.add(question);
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the JSON results", e);
        }

        // Return the list of questions
        return questions;
    }

    // Enable TSL 1.2: https://tinyurl.com/yd2k4jyg
    public static class MySSLSocketFactory extends SSLSocketFactory {

        SSLSocketFactory sslSocketFactory;

        public MySSLSocketFactory(SSLSocketFactory sslSocketFactory) {
            super();
            this.sslSocketFactory = sslSocketFactory;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return sslSocketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return sslSocketFactory.getSupportedCipherSuites();
        }

        @Override
        public SSLSocket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(s, host, port, autoClose);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException,
                UnknownHostException {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port, localHost, localPort);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(address, port, localAddress, localPort);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }
    }
}
