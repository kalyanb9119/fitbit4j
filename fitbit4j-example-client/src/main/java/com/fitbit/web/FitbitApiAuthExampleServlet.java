package com.fitbit.web;

import com.fitbit.api.FitbitAPIException;
import com.fitbit.api.client.*;
import com.fitbit.api.client.service.FitbitAPIClientService;
import com.fitbit.api.common.model.activities.*;
import com.fitbit.api.common.model.user.UserInfo;
import com.fitbit.api.model.APIResourceCredentials;
import com.fitbit.api.model.FitbitUser;
import org.joda.time.LocalDate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Kiryl
 * Date: 6/22/11
 * Time: 7:05 AM
 */
public class FitbitApiAuthExampleServlet extends HttpServlet {

    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_VERIFIER = "oauth_verifier";

    private FitbitAPIEntityCache entityCache = new FitbitApiEntityCacheMapImpl();
    private FitbitApiCredentialsCache credentialsCache = new FitbitApiCredentialsCacheMapImpl();
    private FitbitApiSubscriptionStorage subscriptionStore = new FitbitApiSubscriptionStorageInMemoryImpl();

    private String apiBaseUrl;
    private String fitbitSiteBaseUrl;
    private String exampleBaseUrl;
    private String clientConsumerKey;
    private String clientSecret;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            apiBaseUrl = properties.getProperty("apiBaseUrl");
            fitbitSiteBaseUrl = properties.getProperty("fitbitSiteBaseUrl");
            exampleBaseUrl = properties.getProperty("exampleBaseUrl").replace("/app", "");
            clientConsumerKey = properties.getProperty("clientConsumerKey");
            clientSecret = properties.getProperty("clientSecret");
        } catch (IOException e) {
            throw new ServletException("Exception during loading properties", e);
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        FitbitAPIClientService<FitbitApiClientAgent> apiClientService = new FitbitAPIClientService<FitbitApiClientAgent>(
                new FitbitApiClientAgent(apiBaseUrl, fitbitSiteBaseUrl, credentialsCache),
                clientConsumerKey,
                clientSecret,
                credentialsCache,
                entityCache,
                subscriptionStore
        );
        if (request.getParameter("completeAuthorization") != null) {
            String tempTokenReceived = request.getParameter(OAUTH_TOKEN);
            String tempTokenVerifier = request.getParameter(OAUTH_VERIFIER);
            APIResourceCredentials resourceCredentials = apiClientService.getResourceCredentialsByTempToken(tempTokenReceived);

            if (resourceCredentials == null) {
                throw new ServletException("Unrecognized temporary token when attempting to complete authorization: " + tempTokenReceived);
            }
            // Get token credentials only if necessary:
            if (!resourceCredentials.isAuthorized()) {
                // The verifier is required in the request to get token credentials:
                resourceCredentials.setTempTokenVerifier(tempTokenVerifier);
                try {
                    // Get token credentials for user:
                    apiClientService.getTokenCredentials(new LocalUserDetail(resourceCredentials.getLocalUserId()));
                } catch (FitbitAPIException e) {
                    throw new ServletException("Unable to finish authorization with Fitbit.", e);
                }
            }
            try {

//                List<ActivityLog> activityLogs = activities.getActivities();
//                System.out.println("======ActivityLog.toString()=========");
//                for(int i=0;i<activityLogs.size();i++){
//                    ActivityLog activityLog = activityLogs.get(i);
//                    System.out.println(activityLog.toString());
////                    System.out.println(activityLog.getSteps());
//                }



                UserInfo userInfo = apiClientService.getClient().getUserInfo(new LocalUserDetail(resourceCredentials.getLocalUserId()));
/*              request.setAttribute("userInfo", userInfo);
                request.getRequestDispatcher("/fitbitApiAuthExample.jsp").forward(request, response); */

                System.out.println("======ActivityDistance.toString()=========");
                File file = new File("/Users/bkumar3/Documents/fitbit/IntuitFit/fitbit4j/activity.txt");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
                bufferedWriter.write(userInfo.getDisplayName()+"("+userInfo.getFullName()+")");
                bufferedWriter.newLine();
                LocalDate localDate = LocalDate.now();
                bufferedWriter.write("LOCALDATE:"+localDate.toString());
                bufferedWriter.newLine();
                bufferedWriter.write("localDate.getYear():"+localDate.getYear()+" localDate.getMonthOfYear():"+localDate.getMonthOfYear()+ " localDate.getDayOfMonth():"+localDate.getDayOfMonth());
                bufferedWriter.newLine();;
                for(int i=1;i<=localDate.getDayOfMonth();i++){
                    LocalDate tempDate = LocalDate.fromDateFields(new Date(localDate.getYear()-1900,localDate.getMonthOfYear()-1,i));
                    bufferedWriter.write("Date:" + tempDate.toString() + "\t");
                    Activities activities = apiClientService.getClient().getActivities(new LocalUserDetail(resourceCredentials.getLocalUserId()), FitbitUser.CURRENT_AUTHORIZED_USER, tempDate);
                    ActivitiesSummary activitiesSummary = activities.getSummary();
                    List<ActivityDistance> activityDistanceList  = activitiesSummary.getDistances();
                    bufferedWriter.write(activitiesSummary.getSteps() + " steps\n");
                    bufferedWriter.newLine();
                }
                System.out.println("======ActivityDistance.toString()=========");
                 bufferedWriter.close();

                /*for(int i=0;i<activityDistanceList.size();i++){
                    ActivityDistance activityDistance = activityDistanceList.get(i);
                    System.out.println("DISTANCE:"+activityDistance.getDistance());
                    System.out.println("ACTIVITY:"+activityDistance.getActivity());
                }*/



//                Activities activities = apiClientService.getClient().getActivities(new LocalUserDetail(resourceCredentials.getLocalUserId()), FitbitUser.CURRENT_AUTHORIZED_USER, LocalDate.now());
//                ActivitiesSummary activitiesSummary = activities.getSummary();
//                System.out.println("AAAAAAAA:"+activitiesSummary.getSteps());

            } catch (FitbitAPIException e) {
                throw new ServletException("Exception during getting user info", e);
            }
        } else {
            try {
                response.sendRedirect(apiClientService.getResourceOwnerAuthorizationURL(new LocalUserDetail("-"), exampleBaseUrl + "/fitbitApiAuthExample?completeAuthorization="));
            } catch (FitbitAPIException e) {
                throw new ServletException("Exception during performing authorization", e);
            }
        }
    }
}
