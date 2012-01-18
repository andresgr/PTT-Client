/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.service.ims.presence;

import es.umu.mvia.imsclient.service.protocol.Contact;

/**
 *
 * @author Usuario
 */
public class PresenceInfo {

    private String groupName;
    private String presenceState;
    private String presenteStateDescripcion;

    public PresenceInfo(Contact c) {
        groupName = c.getParentContactGroup().getGroupName();
        presenceState = c.getPresenceStatus().getStatusName();
        presenteStateDescripcion = c.getPresenceStatus().getStatusDescription();
    }

    
    public String getGroupName() {
        return groupName;
    }

    public String getPresenceStateDescription() {
        return presenteStateDescripcion;
    }

    public String getPresenceStateName() {
        return presenceState;
    }


}
