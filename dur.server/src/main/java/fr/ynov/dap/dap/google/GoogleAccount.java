package fr.ynov.dap.dap.google;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
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

// TODO: Auto-generated Javadoc
/**
 * The Class GoogleAccount.
 */
@Controller
public class GoogleAccount extends GoogleService {
    
    /**
     * Handle the Google response.
     *
     * @param code    The (encoded) code use by Google (token, expirationDate,...)
     * @param request The HTTP Request
     * @param session the HTTP Session
     * @return the view to display
     * @throws ServletException When Google account could not be connected to DaP.
     * @throws GeneralSecurityException the general security exception
     */
    @RequestMapping("/oAuth2Callback")
    public String oAuthCallback(@RequestParam final String code, final HttpServletRequest request,
            final HttpSession session) throws ServletException, GeneralSecurityException {
        final String decodedCode = extracCode(request);

        final String redirectUri = buildRedirectUri(request, config.getoAuth2CallbackUrl());

        final String userId = getUserid(session);
        try {
            final GoogleAuthorizationCodeFlow flow = super.getFlow();
            final TokenResponse response = flow.newTokenRequest(decodedCode).setRedirectUri(redirectUri).execute();

            final Credential credential = flow.createAndStoreCredential(response, userId);
            if (null == credential || null == credential.getAccessToken()) {
                //TODO dur by Djer Pourquoi surpprimer des LOG utiles ?
                //LOG.warn("Trying to store a NULL AccessToken for user : " + userId);
            }

             //onSuccess(request, resp, credential);
        } catch (IOException e) {
            //LOG.error("Exception while trying to store user Credential", e);
            throw new ServletException("Error while trying to conenct Google Account");
        }

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
            //LOG.error("userId in Session is NULL in Callback");
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
            throw new ServletException("Error when trying to add Google acocunt");
        }

        return decodeCode;
    }

    /**
     * Build a current host (and port) absolute URL.
     * @param req   The current HTTP request to extract schema, host, port informations
     * @param destination the "path" to the resource
     * @return an absolute URI
     */
    protected String buildRedirectUri(final HttpServletRequest req, final String destination) {
        final GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath(destination);
        return url.build();
    }
    
    
    /**
     * Adds the account.
     *
     * @param userId the user id
     * @param request the request
     * @param session the session
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GeneralSecurityException the general security exception
     */
    @RequestMapping("/account/add/{userId}")
    public String addAccount(@PathVariable final String userId, final HttpServletRequest request,
            final HttpSession session)  throws IOException, GeneralSecurityException{

    	 String response = "errorOccurs";
         GoogleAuthorizationCodeFlow flow;
         Credential credential = null;
         try {
             flow = super.getFlow();
             credential = flow.loadCredential(userId);

             if (credential != null && credential.getAccessToken() != null) {
                 response = "AccountAlreadyAdded";
             } else {
                 final AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
                 authorizationUrl.setRedirectUri(buildRedirectUri(request, config.getoAuth2CallbackUrl()));
                 // store userId in session for CallBack Access
                 session.setAttribute("userId", userId);
                 
                 response = "Redirect:" + authorizationUrl.build();
             }
         } catch (IOException e) {
         }
         return response;
    }
}