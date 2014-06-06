package com.xwikisas.eesc.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xwiki.component.annotation.Component;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.xwikisas.eesc.EESC;
import com.xwikisas.eesc.Group;
import com.xwikisas.eesc.User;

@Component
public class EESCImpl implements EESC
{
    private Gson gson = new Gson();

    private String webserviceURL = "https://demo.monent.fr/interop";
    
    public void setServiceURL(String wsURL) {
        this.webserviceURL = wsURL;
    }

    private JsonElement askForJSON(String url)
    {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse httpResponse;
        int statusCode;

        httpGet.setHeader("Content-type", "application/json");

        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            String jsonString;
            JsonElement json;
            try {
                jsonString = IOUtils.toString(httpResponse.getEntity().getContent());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            try {
                json = gson.fromJson(jsonString, JsonElement.class);
            } catch (Exception e) {
                return null;
            }
            return json;
        }
        return null;
    }

    @Override
    public String getUID(String casID)
    {
        String convertCasIdToUserIdURL = String.format("%s/annuaire/user/%s/login", webserviceURL, casID);
        JsonElement json;
        String userID;

        json = askForJSON(convertCasIdToUserIdURL);
        if (json == null) {
            return null;
        }
        userID = json.getAsJsonObject().get("id").getAsString();
        return userID;
    }

    @Override
    public User getUser(String userID)
    {
        String getUserURL = String.format("%s/annuaire/user/%s", webserviceURL, userID);
        JsonElement json;
        User user;
        String id, nickname, status;

        json = askForJSON(getUserURL);
        if (json == null) {
            return null;
        }
        id = json.getAsJsonObject().get("id").getAsString();
        nickname = json.getAsJsonObject().get("nickname").getAsString();
        status = json.getAsJsonObject().get("status").getAsString();
        user = new User(id, nickname, status);
        return user;
    }

    @Override
    public Group getGroup(String groupID)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> getUsersForGroup(String groupID)
    {
        String getUsersForGroupURL = String.format("%s/annuaire/group/%s/users", webserviceURL, groupID);
        JsonElement json;
        List<User> userList = new ArrayList<User>();
        User user;
        String id, nickname, status;

        json = askForJSON(getUsersForGroupURL);
        if (json == null) {
            return userList;
        }
        for (JsonElement jsonUser : json.getAsJsonArray()) {
            id = jsonUser.getAsJsonObject().get("id").getAsString();
            nickname = jsonUser.getAsJsonObject().get("nickname").getAsString();
            status = jsonUser.getAsJsonObject().get("status").getAsString();
            user = new User(id, nickname, status);
            userList.add(user);
        }
        return userList;
    }

    @Override
    public List<Group> getGroupsForUser(String userID)
    {
        String getGroupsForUserURL = String.format("%s/annuaire/user/%s/groups", webserviceURL, userID);
        JsonElement json;
        List<Group> groupList = new ArrayList<Group>();
        Group group;
        String id, name, type;

        json = askForJSON(getGroupsForUserURL);
        if (json == null) {
            return groupList;
        }
        for (JsonElement jsonGroup : json.getAsJsonArray()) {
            id = jsonGroup.getAsJsonObject().get("id").getAsString();
            name = jsonGroup.getAsJsonObject().get("nickname").getAsString();
            type = jsonGroup.getAsJsonObject().get("type").getAsString();
            group = new Group(id, name, type);
            groupList.add(group);
        }
        return groupList;
    }

    @Override
    public boolean isMember(String userID, String groupID)
    {
        String isMemberURL = String.format("%s/annuaire/group/%s/user/%s", webserviceURL, groupID, userID);
        JsonElement json;
        String id, nickname, status;

        json = askForJSON(isMemberURL);
        if (json == null) {
            return false;
        }
        id = json.getAsJsonObject().get("id").getAsString();
        nickname = json.getAsJsonObject().get("nickname").getAsString();
        status = json.getAsJsonObject().get("status").getAsString();
        try {
            new User(id, nickname, status);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public List<Group> getAllGroups()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
