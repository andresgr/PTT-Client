/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.service.ims.configuration;

/**
 *
 * @author Usuario
 */
public class AccountInfo {

    private String accountID;
    private String registrationState;
    private String presenteState;
    private String presenteStateDescripcion;

    public AccountInfo(String accID, String regState) {
        this(accID,regState,null, null);
    }

    public AccountInfo(String accID, String regState, String statusName, String statusDescription) {
        accountID = accID;
        registrationState = regState;
        presenteState = statusName;
        presenteStateDescripcion = statusDescription;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getRegistrationState() {
        return registrationState;
    }

    public String getPresenceState() {
        return presenteState;
    }

    public String getPresenceStateDescription() {
        return presenteStateDescripcion;
    }

}
