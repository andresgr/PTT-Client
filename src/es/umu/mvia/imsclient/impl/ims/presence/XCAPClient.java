/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.impl.ims.presence;

import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;
import es.umu.mvia.imsclient.util.Logger;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.httpclient.HttpException;
import org.openxdm.xcap.client.*;
import org.openxdm.xcap.common.key.*;
import org.openxdm.xcap.common.uri.*;
import org.openxdm.xcap.common.resource.*;
import org.w3c.dom.*;


/**
 *
 * @author andres
 */
public class XCAPClient {

    /***********************
     * XCAP Client Methods *
     ***********************/

  //private static final String contactsFile = "index";
  private String DEFAULT_LIST_NAME = "friends";
  private static final String RESOURCE_LISTS = "resource-lists";
  private static final String RLS_SERVICES = "rls-services";

  private org.openxdm.xcap.client.XCAPClient client;
  
  private String userId;
  /*private String serverAddress;
  private int serverPort;
  private String serverRoot;*/

  private Logger logger = Logger.getLogger(XCAPClient.class);

  /*private static final String INITIAL_DOCUMENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                    "<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" " +
                                        "xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\" " +
                                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                                        "<service uri=\"sip:andres-contacts@open-ims.test\">" +
                                            "<list name=\"friends\">" +
                                                "<rl:entry uri=\"sip:andres@open-ims.test\"/>" +
                                                "<rl:entry uri=\"sip:asgarcia@open-ims.test\"/>" +
                                                "<rl:entry uri=\"sip:bob@open-ims.test\"/>" +
                                                "<rl:entry uri=\"sip:alice@open-ims.test\"/>" +
                                            "</list>" +
                                            "<packages>" +
                                                "<package>presence</package>" +
                                            "</packages>" +
                                        "</service>" +
                                    "</rls-services>";*/

  public XCAPClient(String server, int port, String root, String user) {

      userId = user.startsWith("sip:") ? user : "sip:" + user;
      /*serverAddress = server;
      serverPort = port;
      serverRoot = root;*/
      try {
          client = new XCAPClientImpl(server, port, root);
          client.setAuthenticationCredentials(userId, userId);
          client.setDoAuthentication(true);
      } catch (InterruptedException ex) {
          logger.error("Error creating XCAP client: " + ex.getMessage());
      }
  }

  public void endXCAPClient() {
      client.shutdown();
  }

  public void createInitialDocument(String file) throws HttpException, IOException {

      UserDocumentUriKey key = new UserDocumentUriKey(RLS_SERVICES, userId, file);
      Response resp = client.get(key, null);
      logger.debug(key + ":\n" + resp.getContent());
      if (resp != null && resp.getCode() != 200) {

            String document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" " +
                                    "xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\" " +
                                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                                    "<service uri=\"" +
                                    createContactsURI() +
                                    "\">" +
                                        "<list name=\"" + DEFAULT_LIST_NAME +"\">" +
                                            //"<rl:entry uri=\"" + userId + "\"/>" +
                                            "</list>" +
                                        "<packages>" +
                                            "<package>presence</package>" +
                                        "</packages>" +
                                    "</service>" +
                                "</rls-services>";

            resp = client.put(key, "application/" + RLS_SERVICES + "+xml", document, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                logger.info("Document created for user " + userId + " in xcap server");
                showDocument(file);
            }
      }

      /*if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
          System.out.println("Document created in xcap server");
          resp = client.get(key, null);
          if (resp != null && resp.getCode() == 200)
                System.out.println("RLS-SERVICES:\n" + resp.getContent());

        try {
            Response resp = client.get(new UserDocumentUriKey(RESOURCE_LISTS, user, file), null);
            if (resp != null && resp.getCode() != 200) {
                UserDocumentUriKey docKey = new UserDocumentUriKey(RESOURCE_LISTS, user, file);

                resp = client.put(docKey, "application/resource-lists+xml", initialDocument, null);

                if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                    System.out.println("Document " + file + " created in xcap server");
                    return true;
                } else {
                    System.out.println("Bad response from xcap server: " + resp.toString());
                    return false;
                }
            }
            return true;
        } catch (HttpException ex) {
            System.out.println("Error retrieving " + file + " document: " + ex.getMessage());
            return false;
        } catch (IOException ex) {
            System.out.println("Error retrieving " + file + " document: " + ex.getMessage());
            return false;
        }*/
    }


  public void deleteDocument(String file) {
        try {
            Response resp = client.get(new UserDocumentUriKey(RLS_SERVICES, userId, file), null);
            if (resp != null && resp.getCode() == 200) {
                UserDocumentUriKey docKey = new UserDocumentUriKey(RLS_SERVICES, userId, file);
                resp = client.delete(docKey, null);

                if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                    logger.info("Document " + file + " deleted");
                } else {
                    logger.warn("Bad response from xcap server: " + resp.toString());
                }
            }
        } catch (HttpException ex) {
            logger.error("Error retrieving " + file + " document: " + ex.getMessage());
        } catch (IOException ex) {
            logger.error("Error retrieving " + file + " document: " + ex.getMessage());
        }
    }


  /*public boolean createList(String file, String listName) {
        try {
            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RESOURCE_LISTS);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step2);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RESOURCE_LISTS, userId,
                   file, elemSel, null);
            String content = "<list name=\"" + listName + "\" xmlns=\"urn:ietf:params:xml:ns:resource-lists\"/>";
            Response resp = client.put(key, ElementResource.MIMETYPE, content, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                return true;
            } else {
                return false;
            }
        } catch (HttpException ex) {
          System.out.println("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          System.out.println("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        }
  }

  public boolean deleteList(String file, String listName) {
        try {
            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RESOURCE_LISTS);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step2);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RESOURCE_LISTS, userId,
                    file, elemSel, null);
            Response resp = client.delete(key, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                return true;
            } else {
                return false;
            }
        } catch (HttpException ex) {
          System.out.println("Error deleting " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          System.out.println("Error deleting " + listName + " client: " + ex.getMessage());
          return false;
        }
  }
*/


    public boolean createList(String file, String listName) {
        try {
            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RLS_SERVICES);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("service", "uri",
                    createContactsURI());
            elementSelectorSteps.add(step2);
            ElementSelectorStep step3 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step3);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RLS_SERVICES, userId, file, elemSel, null);
            String content = "<list name=\"" + listName + "\"/>";
                        //"<entry uri=\"sip:andres@open-ims.test\" " +
//                            "xmlns=\"urn:ietf:params:xml:ns:resource-lists\"/>" +
  //                  "</list>";

            Response resp = client.put(key, ElementResource.MIMETYPE, content, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                logger.info("LISTA CREADA");
                showDocument(file);
                return true;
            } else {
                showDocument(file);
                return false;
            }
        } catch (HttpException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        }
    }

    public boolean createContact(String file, String contactName, String listName) {
        try {

            if (!getList(file, listName)) {
                createList(file, listName);
            }

            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RLS_SERVICES);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("service", "uri",
                    createContactsURI());
            elementSelectorSteps.add(step2);
            ElementSelectorStep step3 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step3);

            contactName = contactName.startsWith("sip:") ? contactName : "sip:" + contactName;

            ElementSelectorStep step4 = new ElementSelectorStepByAttr("entry", "uri", contactName);
            elementSelectorSteps.addLast(step4);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RLS_SERVICES, userId, file, elemSel, null);
            String content = "<entry uri=\"" + contactName +
                    "\" xmlns=\"urn:ietf:params:xml:ns:resource-lists\"/>";
            
            Response resp = client.put(key, ElementResource.MIMETYPE, content, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                showDocument(file);
                return true;
            } else {
                showDocument(file);
                return false;
            }
        } catch (HttpException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        }
  }

  public boolean deleteContact(String file, String contactName, String listName) {
        try {
            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RLS_SERVICES);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("service", "uri",
                    createContactsURI());
            elementSelectorSteps.add(step2);
            ElementSelectorStep step3 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step3);
            ElementSelectorStep step4 = new ElementSelectorStepByAttr("entry", "uri", contactName);
            elementSelectorSteps.addLast(step4);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RLS_SERVICES, userId,
                    file, elemSel, null);
            Response resp = client.delete(key, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                showDocument(file);
                return true;
            } else {
                showDocument(file);
                return false;
            }
        } catch (HttpException ex) {
          logger.error("Error deleting " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          logger.error("Error deleting " + listName + " client: " + ex.getMessage());
          return false;
        }
  }

  private boolean getList(String file, String listName) {
        try {
            LinkedList<ElementSelectorStep> elementSelectorSteps = new LinkedList<ElementSelectorStep>();
            ElementSelectorStep step1 = new ElementSelectorStep(RLS_SERVICES);
            elementSelectorSteps.add(step1);
            ElementSelectorStep step2 = new ElementSelectorStepByAttr("service", "uri",
                    createContactsURI());
            elementSelectorSteps.add(step2);
            ElementSelectorStep step3 = new ElementSelectorStepByAttr("list", "name", listName);
            elementSelectorSteps.add(step3);
            ElementSelector elemSel = new ElementSelector(elementSelectorSteps);
            UserElementUriKey key = new UserElementUriKey(RLS_SERVICES, userId, file, elemSel, null);

            Response resp = client.get(key, null);
            if (resp != null && (resp.getCode() == 200 || resp.getCode() == 201) ) {
                showDocument(file);
                return true;
            } else {
                showDocument(file);
                return false;
            }
        } catch (HttpException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        } catch (IOException ex) {
          logger.error("Error creating " + listName + " client: " + ex.getMessage());
          return false;
        }
  }

  private void showDocument(String file) {
      try {
          UserDocumentUriKey key = new UserDocumentUriKey(RLS_SERVICES, userId, file);
          Response resp = client.get(key, null);
          if (resp != null && resp.getCode() == 200) {
                logger.info(resp.getContent());
          }
      }catch (Exception e){
          e.printStackTrace();
      }
  }

/* <?xml version="1.0" encoding="UTF-8"?>
    <rls-services xmlns="urn:ietf:params:xml:ns:rls-services" xmlns:rl="urn:ietf:params:xml:ns:resource-lists" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <service uri="sip:andres-contacts@open-ims.test">
            <list name="friends">
                <entry xmlns="urn:ietf:params:xml:ns:resource-lists" uri="sip:andres@open-ims.test"/>
                <entry xmlns="urn:ietf:params:xml:ns:resource-lists" uri="sip:asgarcia@open-ims.test"/>
            </list>
            <packages>
                <package>presence</package>
            </packages>
        </service>
    </rls-services>*/
  
  public Map<String,PresenceInfo> getContactsInfo(String file) {

      Map<String,PresenceInfo> m = new HashMap<String,PresenceInfo>();
      try {
          UserDocumentUriKey key = new UserDocumentUriKey(RLS_SERVICES, userId, file);
          Response resp = client.get(key, null);
          if (resp != null && resp.getCode() == 200) {
                logger.info(resp.getContent());
                Document doc = convertDocument(resp.getContent());

                 if (doc == null) {
                     return m;
                 }

                NodeList rlsServicesList = doc.getElementsByTagName(RLS_SERVICES);
                if (rlsServicesList.getLength() == 1) {
                    Node rlsServicesNode = rlsServicesList.item(0);
                    NodeList serviceList = rlsServicesNode.getChildNodes();
                    for (int i=0; i < serviceList.getLength(); i++) {
                        Node service = serviceList.item(i);
                        NodeList serviceChilds = service.getChildNodes();
                        for (int j=0; j < serviceChilds.getLength(); j++) {
                            Node n = serviceChilds.item(j);
                            if (n.getNodeName().compareToIgnoreCase("list") == 0) {
                                NodeList contactsList = n.getChildNodes();
                                for (int k=0; k < contactsList.getLength(); k++) {
                                    Element contact = (Element)contactsList.item(k);
                                    m.put(contact.getAttribute("uri"), null);
                                }
                            }
                        }
                    }
                }

          }
          return m;
      }catch (Exception e){
          e.printStackTrace();
          return m;
      }
  }



    private DocumentBuilderFactory docBuilderFactory = null;
    private DocumentBuilder docBuilder = null;
    private TransformerFactory transFactory = null;
    private Transformer transformer = null;
    
  private Document convertDocument(String document) {
        StringReader reader = new StringReader(document);
        StreamSource source = new StreamSource(reader);
        Document doc = createDocument();

        if (doc == null) {
            return null;
        }

        DOMResult result = new DOMResult(doc);

        try {
            if (this.transFactory == null) {
                this.transFactory = TransformerFactory.newInstance();
            }

            if (this.transformer == null) {
                this.transformer = this.transFactory.newTransformer();
            }

            this.transformer.transform(source, result);
        } catch (Exception e) {
            System.err.println("can't convert the string into a xml document:"+ e);
            return null;
        }

        return doc;
    }

    private Document createDocument() {
        try {
            if (this.docBuilderFactory == null) {
                this.docBuilderFactory = DocumentBuilderFactory.newInstance();
            }

            if (this.docBuilder == null) {
                this.docBuilder = this.docBuilderFactory.newDocumentBuilder();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return this.docBuilder.newDocument();
    }

    private String createContactsURI() {
        return userId.substring(0, userId.indexOf('@')) + //sip:usuario
                "-contacts" + //-contacts
                userId.substring(userId.indexOf('@')); //@dominio
    }



}
