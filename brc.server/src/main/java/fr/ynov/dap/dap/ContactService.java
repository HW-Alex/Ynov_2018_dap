package fr.ynov.dap.dap;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.ListConnectionsResponse;

import fr.ynov.dap.dap.models.ContactResponse;

/**
 * The Class ContactService.
 */
@Service
public class ContactService extends GoogleService{
	
	/** The logger. */
    //TODO brc by Djer Si pas de modifier, sera celui de la classe, ton Logger est donc public !!!!
	static Logger logger = LogManager.getLogger(ContactService.class);
	
	/**
	 * Result contact.
	 *
	 * @param userId the user id
	 * @return the contact response
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws GeneralSecurityException the general security exception
	 */
	public ContactResponse resultContact(String userId) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        PeopleService service = new PeopleService.Builder(HTTP_TRANSPORT, cfg.getJSON_FACTORY(), getCredentials(HTTP_TRANSPORT, cfg.CREDENTIALS_FILE_PATH(), userId))
                .setApplicationName(cfg.getAPPLICATION_NAME())
                .build();

        // Request 10 connections.
        ListConnectionsResponse response = service.people().connections()
                .list("people/me")
                .setPageSize(10)
                .setPersonFields("names")
                .execute();
        int nbContact = response.getTotalPeople();
        
        return new ContactResponse(nbContact);

    }
}