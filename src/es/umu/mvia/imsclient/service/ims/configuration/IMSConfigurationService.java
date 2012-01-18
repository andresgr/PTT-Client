/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.service.ims.configuration;

import java.util.Map;
import es.umu.mvia.imsclient.service.protocol.ProtocolProviderFactory;
import es.umu.mvia.imsclient.service.protocol.ProtocolProviderService;

/**
 * Servicio para la configuracion de una unica cuenta de usuario
 * Cada equipo es monopuesto
 * @author Usuario
 */
public interface IMSConfigurationService {

    public ProtocolProviderService newAccount(String userID, String password, Map<String,String> accountProperties);

    public void deleteAccount();

    public AccountInfo getAccountInfo();

    public ProtocolProviderService getProtocolProvider();

    public ProtocolProviderFactory getProtocolProviderFactory();

}
