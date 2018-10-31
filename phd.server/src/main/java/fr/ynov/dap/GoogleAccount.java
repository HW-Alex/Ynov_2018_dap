package fr.ynov.dap;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//TODO phd by Djer configure tes "save Actions" dans Eclispe (cd mémo eclipse)
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;

import fr.ynov.dap.service.GoogleService;

@Controller
//TODO ohd by Djer Pouruqoi cette classe n'est pas dans le package "controller" ?
public class GoogleAccount extends GoogleService{
	/**
	 * SENSIBLE_DATA_FIRST_CHAR 
	 */
	private static final int SENSIBLE_DATA_FIRST_CHAR = 5;
	/**
	 * SENSIBLE_DATA_LAST_CHAR
	 */
	private static final int SENSIBLE_DATA_LAST_CHAR = 10;
	/**
	 * 
	 */
	private Logger logger = Logger.getLogger("GoogleAccount");
	/**
     * Handle the Google response.
     * @param request The HTTP Request
     * @param code    The (encoded) code use by Google (token, expirationDate,...)
     * @param session the HTTP Session
     * @return the view to display
     * @throws ServletException When Google account could not be connected to DaP.
	 * @throws GeneralSecurityException 
     */
    @RequestMapping("/oAuth2Callback")
    public String oAuthCallback(@RequestParam final String code, final HttpServletRequest request,
            final HttpSession session) throws ServletException, GeneralSecurityException {
        final String decodedCode = extracCode(request);
        final String redirectUri = buildRedirectUri(request, config.getoAuth2CallbackUrl());

        final String userId = extracUser(request);
        try {
            final GoogleAuthorizationCodeFlow flow = super.getFlow();
            final TokenResponse response = flow.newTokenRequest(decodedCode).setRedirectUri(redirectUri).execute();

            final Credential credential = flow.createAndStoreCredential(response, userId);
            if (null == credential || null == credential.getAccessToken()) {
                logger.warning("Trying to store a NULL AccessToken for user : " + userId);
            }

            if (logger.isLoggable(Level.ALL)) {
                if (null != credential && null != credential.getAccessToken()) {
                    logger.fine("New user credential stored with userId : " + userId + "partial AccessToken : "
                            + credential.getAccessToken().substring(SENSIBLE_DATA_FIRST_CHAR,
                                    SENSIBLE_DATA_LAST_CHAR));
                }
            }
            // onSuccess(request, resp, credential);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            throw new ServletException("Error while trying to conenct Google Account");
        }
        //TODO phd by Djer Es-tu sure d'avoir un Mapping sur / ?
        return "redirect:/";
    }

    /**
     * retrieve the User ID in Session.
     * @param session the HTTP Session
     * @return the current User Id in Session
     * @throws ServletException if no User Id in session
     */
    private String getUserid(final HttpSession session) throws ServletException {
        String userId = null;
        if (null != session && null != session.getAttribute("userId")) {
            userId = (String) session.getAttribute("userId");
        }

        if (null == userId) {
            logger.severe("userId in Session is NULL in Callback");
            throw new ServletException("Error when trying to add Google acocunt : userId is NULL is User Session");
        }
        return userId;
    }

    /**
     * Extract OAuth2 Google code (from URL) and decode it.
     * @param request the HTTP request to extract OAuth2 code
     * @return the decoded code
     * @throws ServletException if the code cannot be decoded
     */
    private String extracCode(final HttpServletRequest request) throws ServletException {
        final StringBuffer buf = request.getRequestURL();
        if (null != request.getQueryString()) {
            buf.append('?').append(request.getQueryString());
        }
        final AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());
        final String decodeCode = responseUrl.getCode();

        if (decodeCode == null) {
            throw new MissingServletRequestParameterException("code", "String");
        }

        if (null != responseUrl.getError()) {
            logger.severe("Error when trying to add Google acocunt : " + responseUrl.getError());
            throw new ServletException("Error when trying to add Google acocunt");
        }

        return decodeCode;
    }
    
    private String extracUser(final HttpServletRequest request) throws ServletException {
        final StringBuffer buf = request.getRequestURL();
        if (null != request.getQueryString()) {
            buf.append('?').append(request.getQueryString());
        }
        final AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());
        final String decodeState = responseUrl.getState();

        if (decodeState == null) {
            throw new MissingServletRequestParameterException("code", "String");
        }
        String[] statesSplits = decodeState.split("=");
        if (null != responseUrl.getError() || statesSplits.length < 1) {
            logger.severe("Error when trying to add Google acocunt : " + responseUrl.getError());
            throw new ServletException("Error when trying to add Google acocunt");
        }

        return statesSplits[1];
    }

    /**
     * Build a current host (and port) absolute URL.
     * @param req         The current HTTP request to extract schema, host, port
     *                    informations
     * @param destination the "path" to the resource
     * @return an absolute URI
     */
    protected String buildRedirectUri(final HttpServletRequest req, final String destination) {
        final GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath(destination);
        return url.build();
    }
    
    /**
     * Add a Google account (user will be prompt to connect and accept required
     * access).
     * @param userId  the user to store Data
     * @param request the HTTP request
     * @param session the HTTP session
     * @return the view to Display (on Error)
     * @throws GeneralSecurityException 
     */
    @RequestMapping("/account/add/{userId}")
    public String addAccount(@PathVariable final String userId, final HttpServletRequest request,
            final HttpSession session) throws GeneralSecurityException {
        String response = "errorOccurs";
        GoogleAuthorizationCodeFlow flow;
        Credential credential = null;
        try {
            flow = super.getFlow();
            credential = flow.loadCredential(userId);

            if (credential != null && credential.getAccessToken() != null) {
                response = "AccountAlreadyAdded";
            } else {
                // redirect to the authorization flow
                final AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
                authorizationUrl.setRedirectUri(buildRedirectUri(request, "/oAuth2Callback"));
                authorizationUrl.setState("userId=" + userId);
                // store userId in session for CallBack Access
                //session.setAttribute("userId", userId);
                response = "redirect:" + authorizationUrl.build();
            }
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        // only when error occurs, else redirected BEFORE
        return response;
    }
    
}
