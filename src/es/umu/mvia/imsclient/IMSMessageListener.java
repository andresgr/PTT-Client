/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient;

/**
 *
 * @author Usuario
 */
public interface IMSMessageListener {

    public void receiveMessage(String contact, String content);

}
