package com.tumblr4j.tumblr.factory;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.tumblr4j.tumblr.Client;
import com.tumblr4j.tumblr.OAuthAccessToken;
import com.tumblr4j.tumblr.Tumblr;
import com.tumblr4j.tumblr.enums.HttpClientType;
import com.tumblr4j.tumblr.enums.Permission;
import com.tumblr4j.tumblr.exception.TumblrException;
import com.tumblr4j.tumblr.http.APICallerFactory;
import com.tumblr4j.tumblr.http.APICallerInterface;
import com.tumblr4j.tumblr.util.Constants;

/**
 * You can create a singleton instance of this class. This class is thread safe and you can have 
 * one instance for every unique client id and client secret
 * @author Nischal Shetty
 */
public class TumblrFactory extends OAuthFactory implements Serializable {

	private static final long serialVersionUID = 338904169165196518L;
	
	private Client client;
	private HttpClientType httpClientType;
	private APICallerInterface caller; 
	
	Logger logger = Logger.getLogger(TumblrFactory.class.getName());
	
	public TumblrFactory(String clientId, String clientSecret){
		this(new Client(clientId, clientSecret), HttpClientType.APACHE_HTTP_CLIENT);
	}
	
	public TumblrFactory(Client client){
		this(client,HttpClientType.APACHE_HTTP_CLIENT);
	}
	
	public TumblrFactory(String clientId, String clientSecret, HttpClientType clientType){
		this(new Client(clientId, clientSecret),clientType);
	}
	
	public TumblrFactory(Client client, HttpClientType clientType){
		this.client = client;
		this.httpClientType = clientType;
		caller = APICallerFactory.getAPICallerInstance(clientType);
	}
	
	public HttpClientType getHttpClientType() {
		return httpClientType;
	}

	/**
	 * Returns a new instance of Tumblr pertaining to the authenticated user 
	 * @param accessToken
	 * @return Tumblr instance 
	 */
	public Tumblr getInstance(OAuthAccessToken accessToken){
		return new Tumblr(accessToken,httpClientType);
	}

	
	/**
	 * This will return the redirect URL. You need to redirect the user to this URL.
	 * @return The URL to redirect the user to. If the callbackURL is null then the default URL as set is obtained
	 */
	public String getRedirectURL(String callbackURL){
		
		return getRedirectURL(callbackURL, new Permission[0]);
		
	}
	
	public String getRedirectURL(String callbackURL, Permission... permission){
		
		String redirectURL = null;
		
		try {
			redirectURL = Constants.AUTHORIZE_URL+"?"+Constants.PARAM_CLIENT_ID+"="+client.getClientId() 
											+ "&"+Constants.PARAM_REDIRECT_URI+"="+URLEncoder.encode(callbackURL,"UTF-8")+"&"+Constants.PARAM_RESPONSE_TYPE+"="+Constants
											.PARAM_RESPONSE_TYPE_VALUE;
			
			if(permission!=null && permission.length>0){
				StringBuilder permissions = null;
				for(Permission tempPermission: permission){
					if(permissions!=null){
						permissions.append("+"+tempPermission.toString());
					} else {
						permissions = new StringBuilder(tempPermission.toString());	
					}
				}
				redirectURL += "&"+Constants.PARAM_PERMISSION + "=" + permissions.toString().toLowerCase();
			}
			
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "Unsupported encoding! Please use some other encoding", e);
		}
		return redirectURL;
	}
	
	/**
	 * 
	 * @param code As passed by the authenticating site (Tumblr)
	 * @return
	 * @throws TumblrException 
	 * @throws Exception
	 */
	public Map<String, Object> getOAuthAccessToken(String code, String callbackURL) throws TumblrException{
		
		//We make a call with the provided code, client_id, client_secret and redirect_uri
		
		NameValuePair[] nameValuePairs = getAccessTokenNameValuePairs(code, callbackURL);

    String rawData = caller.postData(Constants.ACCESS_TOKEN_URL,nameValuePairs);
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> userData = null;
		try {
			userData = mapper.readValue(rawData, Map.class);
			//accessToken = new OAuthAccessToken((String)userData.get("access_token"));
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return userData;
	}
	
	private NameValuePair[] getAccessTokenNameValuePairs(String code, String callbackURL){

	return new NameValuePair[]{
															new BasicNameValuePair(Constants.PARAM_CLIENT_ID,client.getClientId()),
															new BasicNameValuePair(Constants.PARAM_CLIENT_SECRET,client.getClientSecret()),
															new BasicNameValuePair(Constants.PARAM_RESPONSE_GRANT_TYPE, Constants.PARAM_RESPONSE_GRANT_TYPE_VALUE),
															new BasicNameValuePair(Constants.PARAM_REDIRECT_URI,callbackURL),
															new BasicNameValuePair(Constants.PARAM_CODE,code)
														};
	}
	
	/*private OAuthAccessToken getAccessToken(String rawData) {
			OAuthAccessToken accessToken = null;
			
			String finalSplit[] = rawData.split("access_token=")[1].split("&expires=");
			accessToken = new OAuthAccessToken(finalSplit[0]);
			if(finalSplit.length>1){
				accessToken.setExpires(Long.parseLong(finalSplit[1]));
			}
			return accessToken;
	}*/
	
}