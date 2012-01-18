/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.launcher;

import es.umu.mvia.imsclient.IMSClient;
import es.umu.mvia.imsclient.IMSIllegalStateException;
import es.umu.mvia.imsclient.IMSMessageListener;
import es.umu.mvia.imsclient.IMSPresenceListener;
import es.umu.mvia.imsclient.service.ims.configuration.AccountInfo;
import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;
import java.io.*;
import java.util.*;
import javax.sip.SipException;
import javax.sip.TransactionUnavailableException;


/**
 *
 * @author Usuario
 */
public class IMSClientMain extends Thread implements IMSMessageListener, IMSPresenceListener{

    private static final String END = "exit";
    private static final String SHOW = "show";
    private static final String ACCOUNT = "account";
    private static final String MESSAGE = "message";
    private static final String STATE = "state";
    private static final String CONTACT = "contact";

    private static final String ADD = "add";
    private static final String REMOVE = "remove";

    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";

    private static final String ECALL = "ecall";

    private static final String CALL = "call";

    IMSClient client;

    public IMSClientMain() {
        client = new IMSClient();
        client.addMessageListener(this);
        client.addPresenceListener(this);
        this.start();
    }

    @Override
    public void run(){

        System.out.println("Escribir \" " + END + " \" para salir.");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // Bucle infinito
        while (true) {

            try {
                
                // Esperamos un comando
                String line = "";
                System.out.print("IMS Client> ");
                line = in.readLine();

                //Salimos de la consola
                if (line.equals(END)){                    

                    //Dejamos de estar subscritos a la recepcion de mensajes
                    client.removeMessageListener(this);
                    client.removePresenceListener(this);

                    //Nos desregistramos del sistema
                    if (client.isRegistered()){
                        try {
                            client.unregister();
                        } catch (IMSIllegalStateException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }

                    //Finalizamos la aplicacion
                    client.closeClientIMS();
               
                }else if (line.trim().equals(""))
                    continue;

                StringTokenizer st = new StringTokenizer(line);
                String command = null;
                if (st.hasMoreTokens())
                    command = st.nextToken();

                if (command.equals(ACCOUNT)){

                    String opt;
                    if (st.hasMoreTokens())
                        opt = st.nextToken();
                    else{
                        help();
                        continue;
                    }

                    if (opt.equals(ADD)) {
                        String userID = null;
                        if (st.hasMoreTokens())
                            userID = st.nextToken();

                        String passwd = null;
                        if (st.hasMoreTokens()) {
                            passwd = st.nextToken();

                            if (st.hasMoreTokens()){
                                client.newAccount(userID, passwd, st.nextToken());
                            }else {
                                client.newAccount(userID, passwd);
                            }
                        }else
                            help();

                    }else if (opt.equals(REMOVE)){
                        try {
                            client.deleteAccount();
                        } catch (IMSIllegalStateException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }else if (opt.equals(SHOW)){
                        try {
                            AccountInfo accInfo = client.getAccountInfo();
                            if (accInfo != null) {
                                System.out.println("AccountID: " + accInfo.getAccountID());
                                System.out.println("Registration state: " + accInfo.getRegistrationState());
                                System.out.println("Presence state: " + accInfo.getPresenceState() +
                                        " \tNote: " + accInfo.getPresenceStateDescription());
                            }
                        } catch (IMSIllegalStateException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }else{
                        help();
                    }

                }else if (command.equals(MESSAGE)){
                    if (st.hasMoreTokens()) {
                        try {
                            if (st.countTokens() >= 2) {
                                client.sendMessage(st.nextToken(), st.nextToken(""));
                            }else
                                help();
                        } catch (IMSIllegalStateException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }else if (command.equals(SHOW)) {
                    try {
                        Map<String,PresenceInfo> m = client.getContactsInfo();
                        if (m != null) {
                            Iterator<String> it = m.keySet().iterator();
                            while (it.hasNext()) {
                                String id = it.next();
                                PresenceInfo pi = m.get(id);
                                System.out.println(id  + "\tGroup: " + pi.getGroupName() +
                                        "\tEstado: " + pi.getPresenceStateName() +
                                        " \tDescripcion: " + pi.getPresenceStateDescription());
                            }
                        }
                    } catch (IMSIllegalStateException ex) {
                        System.out.println(ex.getMessage());
                    }
                }else if (command.equals(STATE)) {
                    try {
                        String state = null;
                        if (st.hasMoreTokens())
                            state = st.nextToken();

                        if (st.hasMoreTokens())
                            client.setNewState(state,st.nextToken(""));
                        else
                            client.setNewState(state);

                    } catch (IMSIllegalStateException ex) {
                        System.out.println(ex.getMessage());
                    }
                }else if (command.equals(CONTACT)) {
                    if (st.hasMoreTokens()){
                        String opt = st.nextToken();
                        if (opt.equals(ADD) && st.hasMoreTokens()) {
                            String userID = st.nextToken();
                            String grupo = null;
                            if (st.hasMoreTokens()) {
                                grupo = st.nextToken();
                                try {
                                    client.addContact(userID, grupo);
                                } catch (IMSIllegalStateException ex) {
                                    System.out.println(ex.getMessage());
                                }
                            }else
                                try {
                                client.addContact(userID);
                            } catch (IMSIllegalStateException ex) {
                                System.out.println(ex.getMessage());
                            }
                        }else if (opt.equals(REMOVE) && st.hasMoreTokens()){
                            String userID = st.nextToken();
                            try {
                                client.removeContact(userID);
                            } catch (IMSIllegalStateException ex) {
                                System.out.println(ex.getMessage());
                            }
                        }else {
                            help();
                        }

                    } else {
                        help();
                        continue;
                    }
                }else if(command.equalsIgnoreCase(REGISTER)){
                    try {
                        client.register();
                    } catch (IMSIllegalStateException ex) {
                        System.out.println(ex.getMessage());
                    }
                }else if(command.equals(UNREGISTER)){
                    try {
                        client.unregister();
                    } catch (IMSIllegalStateException ex) {
                        System.out.println(ex.getMessage());
                    }
                } else if (command.equals(ECALL)) {
                    initiateECall();
                } else if (command.equals(CALL)) {
                    if (st.hasMoreTokens()){
                        String param = st.nextToken();
                        initiateCall(param);
                    }else
                        help();
                } else {
                    help();
                }
            }catch (NullPointerException ex){
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void help() {
        System.out.println("Ayuda de SIPOSGI:");
        System.out.println("- exit: salir de la interfaz de comandos de SIPGOSGI");
        System.out.println("- help: esta ayuda");
        System.out.println("- register: registra al usuario en el CORE IMS");
        System.out.println("- unregister: desregistra al usuario del CORE IMS");
        System.out.println("- account add userID password [Proxy_Address]: Crea una nueva cuenta para el " +
                "usuario \"userID\" con password \"password\"");
        System.out.println("- account show: Muestra informacion de la cuenta de usuario");
        System.out.println("- account remove: Elimina la cuenta de usuario (si existia)");
        System.out.println("- message dest texto: Envia el mensaje \"texto\" al usuario \"dest\"");
        System.out.println("- show: Muestra los contactos del usuario y su estado de presencia");
        System.out.println("- state Online|Busy|Away|Phone|Offline|Other [Note]: Cambia el estado del cliente al especificado");
        System.out.println("- contact add|remove contactID: A�ade/Elimina al usuario \"contactID\" " +
                "de la lista de contactos del usuario");
        System.out.println("- ecall: Inicia una llamada de emergencia");
        System.out.println("- call CALLEE: Llama al identificador CALLEE");

    }

    
    public void receiveMessage(String contact, String content) {
        System.out.println(contact + ": " + content);
    }

    public static void main(String[] args) {
        new IMSClientMain();
    }

    public void contactStateChanged(String contactID, PresenceInfo presenceInfo) {
        System.out.println(contactID  + "\tGroup: " + presenceInfo.getGroupName() +
                                "\tEstado: " + presenceInfo.getPresenceStateName() +
                                " \tDescripcion: " + presenceInfo.getPresenceStateDescription());
    }

    private void initiateECall() {
        try {
            client.sendInvite();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initiateCall(String party) {
        try {
            client.sendInvite(party);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
