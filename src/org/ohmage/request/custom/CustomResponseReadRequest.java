package org.ohmage.request.custom;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.annotator.Annotator;
import org.ohmage.domain.campaign.*;
import org.ohmage.domain.campaign.response.NumberPromptResponse;
import org.ohmage.domain.campaign.response.SingleChoicePromptResponse;
import org.ohmage.exception.DomainException;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.exception.ServiceException;
import org.ohmage.exception.ValidationException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.UserRequest;
import org.ohmage.service.CampaignServices;
import org.ohmage.service.SurveyResponseReadServices;
import org.ohmage.service.SurveyResponseServices;
import org.ohmage.util.DateTimeUtils;
import org.ohmage.validator.CampaignValidators;
import org.ohmage.validator.SurveyResponseValidators;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by kennetvuong on 15/04/15.
 */
public class CustomResponseReadRequest extends UserRequest {
    public static final Logger LOGGER = Logger.getLogger(CustomResponseReadRequest.class);
    public static final String JSON_KEY_CONTEXT = "data";
    public static final String JSON_KEY_RPE = "rpe";
    public static final String JSON_KEY_WELLNESS = "wellness";
    public static final String JSON_KEY_INJURY = "injury";

    public static final String URN_SPECIAL_ALL = "urn:ohmage:special:all";
    public static final Collection<String> URN_SPECIAL_ALL_LIST;
    static {
        URN_SPECIAL_ALL_LIST = new HashSet<String>();
        URN_SPECIAL_ALL_LIST.add(URN_SPECIAL_ALL);
    }

    //Constants
    private static final int MAX_NUMBER_OF_SURVEYS = 10;

    private final String campaignId;
    private final Collection<String> usernames;
    private final Collection<String> surveyIds;

    private Campaign campaign;
    private List<SurveyResponse> surveyResponseList =
            new ArrayList<SurveyResponse>();
    private long surveyResponseCount = 0;
    final long surveyResponsesToProcess = Long.MAX_VALUE;
    final long surveyResponsesToSkip = 0;

    /*
    {
     result=“success"
     data={
     “RPE”: {

                    “lif.1234” :[
                                  {      “2015-14-05”:{“srpeType”: 1, “srpeValue”: 3, “srpeLength”: 10"}
                                  {      “2015-14-04”:{“srpeType”: 1, “srpeValue”: 3, “srpeLength”: 10},
                              ],
                    "lif.abcd":[{},{}]

         },
    “wellness”:{}
    “injury”:{}
    }
    }

     */


    public CustomResponseReadRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
        super(httpRequest, false, TokenLocation.EITHER, null);


        //tmp Containers for campaign id, username and survey ids
        String tCampaignId = null;
        Set<String> tUsernames = null;
        Set<String> tSurveyIds = null;
        if(!isFailed()){
            LOGGER.info("Creating a custom response read request.");
            String[] t;

            //campaign
            try {
                // Campaign ID
                t = getParameterValues(InputKeys.CAMPAIGN_URN);
                if (t.length == 0) {
                    throw new ValidationException(
                            Annotator.ErrorCode.CAMPAIGN_INVALID_ID,
                            "The required campaign ID was not present: " +
                                    InputKeys.CAMPAIGN_URN);
                } else if (t.length > 1) {
                    throw new ValidationException(
                            Annotator.ErrorCode.CAMPAIGN_INVALID_ID,
                            "Multiple campaign IDs were found: " +
                                    InputKeys.CAMPAIGN_URN);
                } else {
                    tCampaignId = CampaignValidators.validateCampaignId(t[0]);
                    if (tCampaignId == null) {
                        throw new ValidationException(
                                Annotator.ErrorCode.CAMPAIGN_INVALID_ID,
                                "The required campaign ID was not present: " +
                                        InputKeys.CAMPAIGN_URN);
                    }
                }
                // User List
                t = getParameterValues(InputKeys.USER_LIST);
                if(t.length == 0) {
                    throw new ValidationException(
                            Annotator.ErrorCode.SURVEY_MALFORMED_USER_LIST,
                            "The user list is missing: " +
                                    InputKeys.USER_LIST);
                }
                else if(t.length > 1) {
                    throw new ValidationException(
                            Annotator.ErrorCode.SURVEY_MALFORMED_USER_LIST,
                            "Mutliple user lists were given: " +
                                    InputKeys.USER_LIST);
                }
                else {
                    tUsernames =
                            SurveyResponseValidators.validateUsernames(t[0]);

                    if(tUsernames == null) {
                        throw new ValidationException(
                                Annotator.ErrorCode.SURVEY_MALFORMED_USER_LIST,
                                "The user list is missing: " +
                                        InputKeys.USER_LIST);
                    }//Removed limit on user_list
                }

                // Survey ID List
                t = getParameterValues(InputKeys.SURVEY_ID_LIST);
                if(t.length > 1) {
                    throw new ValidationException(
                            Annotator.ErrorCode.SURVEY_MALFORMED_SURVEY_ID_LIST,
                            "Multiple survey ID lists were given: " +
                                    InputKeys.SURVEY_ID_LIST);
                }
                else if(t.length == 1) {
                    tSurveyIds =
                            SurveyResponseValidators.validateSurveyIds(t[0]);

                    if((tSurveyIds != null) && (tSurveyIds.size() > MAX_NUMBER_OF_SURVEYS)) {
                        throw new ValidationException(
                                Annotator.ErrorCode.SURVEY_TOO_MANY_SURVEY_IDS,
                                "More than " +
                                        MAX_NUMBER_OF_SURVEYS +
                                        " survey IDs were given: " +
                                        tSurveyIds.size());
                    }
                }
            }
            catch (ValidationException e) {
                e.failRequest(this);
                e.logException(LOGGER);
            }


                //user list
            //Survey list
        }

        //sett variables
        campaignId = tCampaignId;
        usernames = tUsernames;
        surveyIds = tSurveyIds;
    }

    /*
    * (non-Javadoc)
    * @see org.ohmage.request.Request#service()
    */
    @Override
    public void service() {
        LOGGER.info("Servicing a custom response read request.");
        if(! authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED)) {
            return;
        }
        try {
            LOGGER.info("Retrieving campaign configuration.");

            campaign = CampaignServices.instance().getCampaign(campaignId);
            if (campaign == null) {
                throw
                        new ServiceException(
                                Annotator.ErrorCode.CAMPAIGN_INVALID_ID,
                                "The campaign does not exist.");
            }

            if((surveyIds != null) && (! surveyIds.isEmpty()) && (! URN_SPECIAL_ALL_LIST.equals(surveyIds))) {
                LOGGER.info("Verifying that the survey ids in the query belong to the campaign.");
                SurveyResponseReadServices.instance().verifySurveyIdsBelongToConfiguration(surveyIds, campaign);
            }


            LOGGER.info("Dispatching to the data layer.");
            LOGGER.info("CustomResponseService - usernames:" + usernames.size() + " surveys: " + surveyIds.size());
            surveyResponseCount =
                    SurveyResponseServices.instance().readSurveyResponseInformation(
                            campaign,
                            getUser().getUsername(),
                            null, // surveyResponseIds, no ids
                            (URN_SPECIAL_ALL_LIST.equals(usernames) ? null : usernames),
                            null, // startDate
                            null, // endDate
                            null,//privacyState,
                            (URN_SPECIAL_ALL_LIST.equals(surveyIds)) ? null : surveyIds,
                            null, //(URN_SPECIAL_ALL_LIST.equals(promptIds)) ? null : promptIds,
                            null,
                            null,//promptResponseSearchTokens,
                            null, //((collapse != null) && collapse && (! columns.equals(URN_SPECIAL_ALL_LIST))) ? columns : null,
                            null,//sortOrder,
                            surveyResponsesToSkip,//numSurveyResponsesToSkip,
                            surveyResponsesToProcess,//numSurveyResponsesToProcess,
                            surveyResponseList
                    );

            int numPromptResponses = 0;
            for(SurveyResponse surveyResponse : surveyResponseList) {
                numPromptResponses += surveyResponse.getResponses().size();
            }

            LOGGER.info(
                    "Found " +
                            surveyResponseList.size() +
                            " results after filtering and paging a total of " +
                            surveyResponseCount +
                            " applicable responses, which contains " +
                            numPromptResponses +
                            " prompt responses.");
        }
        catch(ServiceException e) {
            e.failRequest(this);
            e.logException(LOGGER);
        }
        //Validate user campaign stuff first
        //fetch data from SurveyResponseService

    }

    /**
     * Builds the output depending on the state of this request and whatever
     * output format the requester selected.
     */
    @Override
    public void respond(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LOGGER.info("Responding to the custom response read request.");

        if(isFailed()) {
            super.respond(httpRequest, httpResponse, (JSONObject) null);
            return;
        }

        // Create a writer for the HTTP response object.
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(httpRequest, httpResponse)));
        }
        catch(IOException e) {
            LOGGER.error("Unable to write response message. Aborting.", e);
            return;
        }

        // Sets the HTTP headers to disable caching.
        expireResponse(httpResponse);

        String resultString = "";

        if(!isFailed()) {
            httpResponse.setContentType("application/json");

            try {

                JSONObject resultJson = new JSONObject();
                resultJson.put(JSON_KEY_RESULT,RESULT_SUCCESS);
                JSONObject data = new JSONObject();
                //Set med unique username and surveys
                //Survey ID -> Username

                for(SurveyResponse surveyResponse : getSurveyResponses()) {
                    String surveyId = surveyResponse.getSurvey().getId();
                    String username = surveyResponse.getUsername();
                    DateTime time = surveyResponse.getDate();

                    //Has Key
                    if(data.has(surveyId)){
                        JSONObject objUser = data.getJSONObject(surveyId);//get survey id
                        //has username
                        if(objUser.has(username)){
                            //add prompt to Json array
                            //Get user array
                            JSONArray arrUser = objUser.getJSONArray(username);
                            //Create res
                            JSONObject res = processResponse(time, surveyResponse.getResponses());
                            //add res to array
                            arrUser.put(res);
                            //put array back
                            objUser.put(username,arrUser);
                        }
                        //username does not exist, add user object with empty array
                        else{
                            //since user does not exists, array does exist either
                            JSONArray arrUser = new JSONArray();
                            //add prompt to json aray.
                            JSONObject res = processResponse(time, surveyResponse.getResponses());
                            //Put the res into array
                            arrUser.put(res);
                            //Add user with array
                            objUser.put(username, arrUser);
                        }
                        //put the objUser back
                        data.put(surveyId, objUser);
                    }
                    else{
                        //Survey id does not exists
                        //add survey
                        //add username
                        //add response
                        JSONObject res = processResponse(time, surveyResponse.getResponses());
                        JSONArray arrUser = new JSONArray();
                        arrUser.put(res);
                        JSONObject objUser = new JSONObject();
                        objUser.put(username, arrUser);
                        data.put(surveyId, objUser);//empty array with prompt

                    }

                }


                resultJson.put(JSON_KEY_DATA, data);
                resultString = resultJson.toString();
            } catch (JSONException e) {
                LOGGER.error(e.toString(), e);
                setFailed();
            }
            catch(IllegalStateException e) {
                LOGGER.error(e.toString(), e);
                setFailed();
            }

           /* catch(DomainException e) {
                LOGGER.error(e.toString(), e);
                setFailed();
            }*/
        }

        if(isFailed()) {
            httpResponse.setContentType("application/json");
            resultString = this.getFailureMessage();
        }

        try {
            writer.write(resultString);
        }
        catch(ClientAbortException e) {
            LOGGER.info("The client hung up unexpectedly.", e);
        }
        catch(IOException e) {
            LOGGER.warn("Unable to write response message. Aborting.", e);
        }

        // Close it.
        try {
            writer.close();
        }
        catch(ClientAbortException e) {
            LOGGER.info("The client hung up unexpectedly.", e);
        }
        catch(IOException e) {
            LOGGER.warn("Unable to close the writer.", e);
        }
    }
        public Collection<SurveyResponse> getSurveyResponses() {
            return Collections.unmodifiableCollection(surveyResponseList);
        }

    /**
     * @return a json object with time, and all responses with "id" : value*/
    private JSONObject processResponse(DateTime time, Map<Integer, Response> s) throws JSONException {

        JSONObject obj = new JSONObject();
        obj.put("time", DateTimeUtils.getIso8601DateString(
                time, false));
        for(int key : s.keySet()){
            Response response = s.get(key);
            if(response instanceof SingleChoicePromptResponse){
                JSONObject resObj = response.toJson(true);
                String resKey = resObj.get("prompt_id").toString();
                String resVal = resObj.getString("value").toString();
                obj.put(resKey,resVal);
                /*JSONObject newObj = new JSONObject();
                newObj.put(resKey,resVal);

                LOGGER.info("New Obj: " + newObj.toString());*/
            }
            else if(response instanceof NumberPromptResponse){
                JSONObject resObj = response.toJson(true);
                String resKey = resObj.get("prompt_id").toString();
                int resVal = Integer.parseInt(resObj.getString("value").toString());
                obj.put(resKey,resVal);
                /*JSONObject newObj = new JSONObject();
                newObj.put(resKey, resVal);
                LOGGER.info("New Obj: " + newObj.toString());*/
            }
        }

        return obj;
    }
}
