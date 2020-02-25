package com.alliander.schema_composer.loaders;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class EnterpriseArchitectLoader {

    private String baseURI, uid, pwd;

    public EnterpriseArchitectLoader(JSONObject data) {
        this.baseURI = data.getString("base");
        this.uid = data.getString("username");
        this.pwd = data.getString("pwd");
    }

    /** lists the diagrams in the selected package */
    public String getDiagrams(String pk_guid, String uid) {
        return getJSON(eaCall("nestedresources/pk_" + pk_guid + "/?useridentifier=" + uid, true, false));
    }

    /** Lists the relevant elements in a given diagram */
    public String getDiagram(String dg_guid, String uid) {
        // fetch the diagram information (XML)
        JSONObject diagramInfo = XML.toJSONObject(eaCall("features/dg_" + dg_guid + "/?useridentifier=" + uid, true, false));
        String imageMap = diagramInfo.getJSONObject("rdf:RDF").getJSONObject("ss:features").getJSONObject("ss:diagramimage").getJSONObject("rdf:Description").get("ss:imagemap").toString();
        // parse the classes + position (assumes coords between quotes and elements between brackets)
        JSONArray elements = new JSONArray();
        String[] currentElement = imageMap.split("<area shape=\"rect\"");
        for (int x = 1; x < currentElement.length; x++) {
            JSONObject element = new JSONObject();
            String guid = currentElement[x].split("element=")[1].split("\"")[0];
            element.put("coords", currentElement[x].split("coords=\"")[1].split("\"")[0]);
            element.put("guid", guid);
            element.put("metadata", XML.toJSONObject(eaCall("resource/el_" + guid + "/?useridentifier=" + uid, true, false)));
            // fetch the attributes
            element.put("attributes", XML.toJSONObject(eaCall("features/el_" + guid + "/?useridentifier=" + uid, true, false)));
            // fetch the generalizations + associations
            JSONObject links = XML.toJSONObject(eaCall("linkedresources/el_" + guid + "/?useridentifier=" + uid, true, false)).getJSONObject("rdf:RDF");
            // TODO ones EA updates its API we might be able to fetch resources with prefix LT_
            element.put("linkedResources", links);
            elements.put(element);
        }
        return elements.toString(4);
    }

    public String getUID() {
        return getJSON(eaCall("login/", false, true));
    }

    /** method to perform get request (to EA OSLC api) and parse the xml result */
    private String eaCall(String path, boolean authorize, boolean post) {
        try {
            URL url = new URL(this.baseURI + path);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setInstanceFollowRedirects(false);
            // if post we retrieve an user identifier
            if (post) {
                con.setRequestMethod("POST");
                DataOutputStream wr = null;
                wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes("uid=" + this.uid + ";pwd=" + this.pwd);
                wr.flush();
                wr.close();
            }
            // set user credentials if required
            if (authorize) {
                con.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.uid + ":" + this.pwd).getBytes()));
            }
            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            return content.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private String getJSON(String xml) {
        return XML.toJSONObject(xml).toString(4);
    }
}
