/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.umu.mvia.imsclient.launcher;

import es.umu.mvia.imsclient.IMSClient;
import es.umu.mvia.imsclient.IMSIllegalStateException;
import es.umu.mvia.imsclient.IMSPresenceListener;
import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;

import java.util.*;
import java.io.*;
import java.lang.Thread;

/**
 *
 * @author usuario
 */
public class PruebaClienteIMS implements IMSPresenceListener {

    /**
     * @param args the command line arguments
     */
    public static int HORAS_PRUEBA = 2;
    public static int MINUTOS_PRUEBA = 1;
    IMSClient c;
    TestsThread t;
    PrintStream ps;
    PrintStream ps2;

    public static void main(String[] args) {

        PruebaClienteIMS p = new PruebaClienteIMS();
        p.ejecutarPrueba();
    }

    public void contactStateChanged(String contactID, PresenceInfo p) {

        try {
            System.err.println(c.getAccountInfo().getAccountID() + "   " + Calendar.getInstance().getTime().toString() + ":   " + contactID + "  " + p.getPresenceStateName() + "  " + p.getPresenceStateDescription());
        } catch (IMSIllegalStateException ex) {
            ex.printStackTrace();
        }
    }

    public void ejecutarPrueba() {
        c = new IMSClient();

        c.newAccount("antonioruiz@open-ims.test", "antonioruiz", "195.235.93.158");
        // c.newAccount("antonio@open-ims.test", "antonio", "155.54.210.103");
        c.addPresenceListener(this);
        String time = Calendar.getInstance().getTime().toString();
        time = time.replace(":", "-");
        try {
            String logdir = System.getProperty(".");
            String logfile = "Test-Tiempo-Registro-" + time + ".txt";
            ps = new PrintStream(
                    new BufferedOutputStream(new FileOutputStream(
                    new File(logdir, logfile))), true);

            String logfile2 = "Test-Tiempo-DesRegistro-" + time + ".txt";
            ps2 = new PrintStream(
                    new BufferedOutputStream(new FileOutputStream(
                    new File(logdir, logfile2))), true);

            //System.setOut(ps);


            t = new TestsThread(c);
            t.start();

            TestsThread2 t2 = new TestsThread2(c);
            t2.start();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void dormir(int sec) {
        try {
            Thread.currentThread().sleep(sec * 1000);//sleep for 1000 ms
        } catch (Exception ie) {
        }
    }

    private class TestsThread extends Thread {

        IMSClient client;

        public TestsThread(IMSClient c) {
            client = c;
        }

        public void run() {
            int prueba = 0;
            String state = "";
            String note = "";
            Calendar fin = Calendar.getInstance();
            //fin.add(Calendar.HOUR, HORAS_PRUEBA);
            fin.add(Calendar.MINUTE, MINUTOS_PRUEBA);
            while (Calendar.getInstance().before(fin)) {
                try {
                    //Establecer el periodo de reenv�o
                  /*Registramos al cliente en el core IMS*/
                    c.register();
                    dormir(3);
                    ps.print("Tiempo Inicio de registro =" + Long.toString(c.getTiempoInicioRegistro()));
                    ps.print("   Tiempo Fin de registro ="  +  c.getTiempoFinRegistro());
                    ps.println("   Diferencia tiempo: " + (Long.toString(c.getTiempoFinRegistro()-c.getTiempoInicioRegistro())));

                    
                    c.unregister();
                    dormir(3);
                    ps2.print("Tiempo Inicio de Desregistro =" + Long.toString(c.getTiempoInicioDesRegistro()));
                    ps2.print("   Tiempo Fin de Desregistro ="  +  c.getTiempoFinRegistro());
                    ps2.println("   Diferencia tiempo: " + (Long.toString(c.getTiempoFinRegistro()-c.getTiempoInicioDesRegistro())));
                    

                } catch (IMSIllegalStateException e) {
                    System.out.println("Excepci�n al cambiar estado en modo test");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            try {

                c.closeClientIMS();
                dormir(2);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class TestsThread2 extends Thread {

        IMSClient client;

        public TestsThread2(IMSClient c) {
            client = c;
        }

        public void run() {

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            try {
                line = in.readLine();

                //Salimos de la consola
                if (line.equalsIgnoreCase("exit")) {

                    t.interrupt();
                    if (c.isRegistered()){
                        c.unregister();
                        dormir(5);
                    }

                    c.closeClientIMS();
                    System.exit(0);
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

        }
    }
}
