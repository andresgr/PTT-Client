/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.impl.media;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.sdp.*;

import es.umu.mvia.imsclient.service.media.*;
import es.umu.mvia.imsclient.service.media.MediaException;
import es.umu.mvia.imsclient.service.netaddr.*;
import es.umu.mvia.imsclient.service.protocol.*;
import es.umu.mvia.imsclient.service.protocol.event.*;
import es.umu.mvia.imsclient.util.*;
import es.umu.mvia.imsclient.impl.media.codec.*;
import es.umu.mvia.imsclient.impl.netaddr.NetworkAddressManagerServiceImpl;
import javax.media.control.*;


/**
 * Contains parameters associated with a particular Call such as media (audio
 * video), a reference to the call itself, RTPManagers and others.
 * <p>
 * Currently the class works the following way:<p>
 * We create 2 rtp managers (one for video and one for audio) upon
 * initialization of this call session and initialize/bind them on local
 * addresses.
 * <p>
 * When we are asked to create an SDP offer we ask the <tt>MediaControl</tt>
 * for the Formats/Encodings that we support and create a media description that
 * would advertise these formats as well as the ports that our RTP managers are
 * bound upon.
 * <p>
 * When we need to process an incoming offer we ask the <tt>MediaControl</tt>
 * for the Formats/Encodings that we support, intersect them with those that
 * were sent by the offerer and make <tt>MediaControl</tt> configure our source
 * processor so that it would transmit in the format that it is expected to
 * according to the format set that resulted from the intersection. We also
 * prepare our <tt>RTPManager</tt>-s to send streams for every media type
 * requested in the offer. (Note that these streams are not started until
 * the associated call enters the CONNECTED state).
 * <p>
 * Processing an SDP answer is quite similar to processing an offer with the
 * exception that the intersection of all supported formats has been performed
 * bye the remote party and we only need to configure our processor and
 * <tt>RTPManager</tt>s.
 *
 * @todo implement SendStreamListener.
 * @todo implement ReceiveStreamListener.
 *
 * @author Emil Ivov
 * @author Ryan Ricard
 */
public class CallSessionImpl
        implements   CallSession
                   , CallParticipantListener
                   , CallChangeListener
                   , ReceiveStreamListener
                   , SendStreamListener
                   , SessionListener
                   , ControllerListener

{
    private static final Logger logger
        = Logger.getLogger(CallSessionImpl.class);

    /**
     * The call associated with this session.
     */
    private Call call = null;

    /**
     * The session address that is used for audio communication in this call.
     */
    private SessionAddress audioSessionAddress = null;

    /**
     * The public address returned by the net address manager for the audio
     * session address.
     */
    private InetSocketAddress audioPublicAddress = null;

    /**
     * The session address that is used for video communication in this call.
     */
    private SessionAddress videoSessionAddress = null;

    /**
     * The public address returned by the net address manager for the video
     * session address.
     */
    private InetSocketAddress videoPublicAddress = null;

    /**
     * The rtpManager that handles audio streams in this session.
     */
    private RTPManager audioRtpManager = RTPManager.newInstance();

    /**
     * The rtpManager that handles video streams in this session.
     */
    private RTPManager videoRtpManager = RTPManager.newInstance();

    /**
     * The media service instance that created us.
     */
    private MediaServiceImpl mediaServCallback = null;

    /**
     * The minimum port number that we'd like our rtp managers to bind upon.
     */
    private static int minPortNumber = 5000;

    /**
     * The maximum port number that we'd like our rtp managers to bind upon.
     */
    private static int maxPortNumber = 6000;

    /**
     * The name of the property indicating the length of our receive buffer.
     */
    private static final String PROPERTY_NAME_RECEIVE_BUFFER_LENGTH
        = "es.umu.mvia.imsclient.impl.media.RECEIVE_BUFFER_LENGTH";

    /**
     * The list of currently active players that we have created during this
     * session.
     */
    private List players = new ArrayList();

    /**
     * The list of currently open Video frames that we have created during this
     * session.
     */
    private List videoFrames = new ArrayList();

    /**
     * The Custom Data Destination used for this call session.
     */
    private URL dataSink = null;


    private NetworkAddressManagerServiceImpl networkAddressManagerServiceImpl;

    /**
     * List of RTP format strings which are supported by SIP Communicator in addition
     * to the JMF standard formats.
     * 
     * @see #registerCustomCodecFormats(RTPManager)
     * @see MediaControl#registerCustomCodecs()
     */
    private static final javax.media.Format[] CUSTOM_CODEC_FORMATS 
        = new javax.media.Format[] 
    {
        new AudioFormat(Constants.ILBC_RTP),
        new AudioFormat(Constants.ALAW_RTP),
        new AudioFormat(Constants.SPEEX_RTP)
    };

    /**
     * Creates a new session for the specified <tt>call</tt> with a custom
     * destination for incoming data.
     *
     * @param call The call associated with this session.
     * @param mediaServCallback the media service instance that created us.
     * @param dataSink the place to send incoming data.
     */
    public CallSessionImpl(Call call, 
                           MediaServiceImpl mediaServCallback,
                           URL dataSink,
                           NetworkAddressManagerServiceImpl namsi)
    {
        this.call = call;
        this.mediaServCallback = mediaServCallback;
        this.dataSink = dataSink;

        this.networkAddressManagerServiceImpl = namsi;

        call.addCallChangeListener(this);
        initializePortNumbers();
    }

    /**
     * Creates a new session for the specified <tt>call</tt>.
     *
     * @param call The call associated with this session.
     * @param mediaServCallback the media service instance that created us.
     */
    public CallSessionImpl(Call call, MediaServiceImpl mediaServCallback,
                           NetworkAddressManagerServiceImpl namsi)
    {
        this(call, mediaServCallback, null , namsi);
    }

    /**
     * Returns the call associated with this Session.
     *
     * @return the Call associated with this session.
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns the port that we are using for receiving video data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving video data in this
     * <tt>CallSession</tt>.
     */
    public int getVideoPort()
    {
        return videoSessionAddress.getDataPort();
    }

    /**
     * Returns the port that we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     */
    public int getAudioPort()
    {
        return audioSessionAddress.getDataPort();
    }

    /**
     * Returns the rtp manager that we are using for audio streams.
     * @return the RTPManager instance that we are using for audio streams.
     */
    public RTPManager getAudioRtpManager()
    {
        return this.audioRtpManager;
    }

    /**
     * Returns the rtp manager that we are using for video streams.
     * @return the RTPManager instance that we are using for audio streams.
     */
    public RTPManager getVideoRtpManager()
    {
        return this.videoRtpManager;
    }

    /**
     * Opens all streams that have been initialized for local RTP managers.
     *
     * @throws MediaException if start() fails for all send streams.
     */
    private void startStreaming()
        throws MediaException
    {
        //start all audio streams
        boolean startedAtLeastOneStream = false;
        RTPManager rtpManager = getAudioRtpManager();

        Vector sendStreams = rtpManager.getSendStreams();
        if(sendStreams != null && sendStreams.size() > 0)
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " audio send streams.");
            Iterator ssIter = sendStreams.iterator();

            while (ssIter.hasNext())
            {
                SendStream stream = (SendStream) ssIter.next();
                try
                {
                    /** @todo are we sure we want to connect here? */
                    stream.getDataSource().connect();
                    stream.start();
                    startedAtLeastOneStream = true;
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to start stream.", ex);
                }
            }
        }
        else
        {
            logger.trace("No audio send streams will be started.");
        }

        //start video streams
        rtpManager = getVideoRtpManager();
        sendStreams = rtpManager.getSendStreams();
        if(sendStreams != null && sendStreams.size() > 0)
        {
            logger.trace("Will be starting " + sendStreams.size()
                         + " video send streams.");
            Iterator ssIter = sendStreams.iterator();

            while (ssIter.hasNext())
            {
                SendStream stream = (SendStream) ssIter.next();
                try
                {
                    stream.start();
                    startedAtLeastOneStream = true;
                }
                catch (IOException ex)
                {
                    logger.warn("Failed to start stream.", ex);
                }
            }
        }
        else
        {
            logger.trace("No video send streams will be started.");
        }


        if(!startedAtLeastOneStream && sendStreams.size() > 0)
        {
            stopStreaming();
            throw new MediaException("Failed to start streaming"
                                     , MediaException.INTERNAL_ERROR);
        }
    }

    /**
     * Stops and closes all streams that have been initialized for local
     * RTP managers.
     */
    private void stopStreaming()
    {
        stopStreaming(getAudioRtpManager(), "audio");
        this.audioRtpManager = null;
        stopStreaming(getVideoRtpManager(), "video");
        this.videoRtpManager = null;
    }

    /**
     * Stops and closes all streams currently handled by <tt>rtpManager</tt>.
     *
     * @param rtpManager the rtpManager whose streams we'll be stopping.
     */
    private void stopStreaming(RTPManager rtpManager,
                               String rtpManagerDescription)
    {
        Vector sendStreams = rtpManager.getSendStreams();
        Iterator ssIter = sendStreams.iterator();

        while(ssIter.hasNext())
        {
            SendStream stream = (SendStream) ssIter.next();
            try
            {
                stream.getDataSource().stop();
                stream.stop();
                stream.close();
            }
            catch (IOException ex)
            {
                logger.warn("Failed to stop stream.", ex);
            }
        }

        Vector receiveStreams = rtpManager.getReceiveStreams();
        Iterator rsIter = receiveStreams.iterator();
        while(rsIter.hasNext())
        {
            ReceiveStream stream = (ReceiveStream) rsIter.next();
            try
            {
                stream.getDataSource().stop();
            }
            catch (IOException ex)
            {
                logger.warn("Failed to stop stream.", ex);
            }
        }

        //remove targets
        rtpManager.removeTargets("Session ended.");

        printFlowStatistics(rtpManager);

        //stop listening
        rtpManager.removeReceiveStreamListener(this);
        rtpManager.removeSendStreamListener(this);
        rtpManager.removeSessionListener(this);
        rtpManager.dispose();
    }

    /**
     * Prints all statistics available for rtpManager. (Method contributed by
     * Michael Koch).
     *
     * @param rtpManager the RTP manager that we'd like to print statistics for.
     */
    private void printFlowStatistics(RTPManager rtpManager)
    {
        String rtpManagerDescription = (rtpManager == getAudioRtpManager())
            ? "(for audio flows)"
            : "(for video flows)";

        //print flow statistics.
        GlobalTransmissionStats s = rtpManager.getGlobalTransmissionStats();

        logger.info(
            "global transmission stats (" + rtpManagerDescription + "): \n" +
            "bytes sent: " + s.getBytesSent() + "\n" +
            "local colls: " + s.getLocalColls() + "\n" +
            "remote colls: " + s.getRemoteColls() + "\n" +
            "RTCP sent: " + s.getRTCPSent() + "\n" +
            "RTP sent: " + s.getRTPSent() + "\n" +
            "transmit failed: " + s.getTransmitFailed()
        );

        GlobalReceptionStats rs = rtpManager.getGlobalReceptionStats();

        logger.info(
            "global reception stats (" + rtpManagerDescription + "): \n" +
            "bad RTCP packets: " + rs.getBadRTCPPkts() + "\n" +
            "bad RTP packets: " + rs.getBadRTPkts() + "\n" +
            "bytes received: " + rs.getBytesRecd() + "\n" +
            "local collisions: " + rs.getLocalColls() + "\n" +
            "malformed BYEs: " + rs.getMalformedBye() + "\n" +
            "malformed RRs: " + rs.getMalformedRR() + "\n" +
            "malformed SDESs: " + rs.getMalformedSDES() + "\n" +
            "malformed SRs: " + rs.getMalformedSR() + "\n" +
            "packets looped: " + rs.getPacketsLooped() + "\n" +
            "packets received: " + rs.getPacketsRecd() + "\n" +
            "remote collisions: " + rs.getRemoteColls() + "\n" +
            "RTCPs received: " + rs.getRTCPRecd() + "\n" +
            "SRRs received: " + rs.getSRRecd() + "\n" +
            "transmit failed: " + rs.getTransmitFailed() + "\n" +
            "unknown types: " + rs.getUnknownTypes()
        );
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The
     * resources (address and port) allocated for the <tt>callParticipant</tt>
     * should be kept by the media service implementation until the originating
     * <tt>callParticipant</tt> enters the DISCONNECTED state. Subsequent sdp
     * offers/answers requested for the <tt>Call</tt> that the original
     * <tt>callParticipant</tt> belonged to MUST receive the same IP/port couple
     * as the first one in order to allow for conferencing. The associated port
     * will be released once the call has ended.
     *
     * @todo implement ice.
     *
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     */
    public String createSdpOffer()
        throws es.umu.mvia.imsclient.service.media.MediaException
    {
        return createSessionDescription(null, null).toString();
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. The intendedDestination
     * parameter, may contain the address that the offer is to be sent to. In
     * case it is null we'll try our best to determine a default local address.
     *
     * @param intendedDestination the address of the call participant that the
     * descriptions is to be sent to.
     * @return a new SDP description String advertising all params of
     * <tt>callSession</tt>.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     */
    public String createSdpOffer(InetAddress intendedDestination)
        throws es.umu.mvia.imsclient.service.media.MediaException
    {
        //return createSessionDescription(null, intendedDestination).toString();
        return "";
    }

    /**
     * The method is meant for use by protocol service implementations upon
     * reception of an SDP answer in response to an offer sent by us earlier.
     *
     * @param sdpAnswerStr the SDP answer that we'd like to handle.
     * @param responder the participant that has sent the answer.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     * @throws ParseException if sdpAnswerStr does not contain a valid sdp
     * String.
     */
    public synchronized void processSdpAnswer(CallParticipant responder, 
                                              String sdpAnswerStr)
        throws MediaException, ParseException
    {
        logger.trace("Parsing sdp answer: " + sdpAnswerStr);
        //first parse the answer
        SessionDescription sdpAnswer = null;
        try
        {
            sdpAnswer = mediaServCallback.getSdpFactory()
                .createSessionDescription(sdpAnswerStr);
        }
        catch (SdpParseException ex)
        {
            throw new ParseException("Failed to parse SDPOffer: "
                                     + ex.getMessage()
                                     , ex.getCharOffset());
        }

        //extract media descriptions
        Vector mediaDescriptions = null;
        try
        {
            mediaDescriptions = sdpAnswer.getMediaDescriptions(true);
        }
        catch (SdpException exc)
        {
            logger.error("failed to extract media descriptions", exc);
            throw new MediaException("failed to extract media descriptions"
                                    , MediaException.INTERNAL_ERROR
                                    , exc);
        }


        //add the RTP targets
        this.initStreamTargets(sdpAnswer.getConnection(), mediaDescriptions);

        //create and init the streams (don't start streaming just yet but wait
        //for the call to enter the connected state).
        createSendStreams(mediaDescriptions);
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. Apart
     * from simply generating an SDP response description, the method records
     * details
     *
     * @param sdpOfferStr the SDP offer that we'd like to create an answer for.
     * @param offerer the participant that has sent the offer.
     *
     * @return a String containing an SDP answer descibing parameters of the
     * <tt>Call</tt> associated with this session and matching those advertised
     * by the caller in their <tt>sdpOffer</tt>.
     *
     * @throws MediaException code INTERNAL_ERROR if processing the offer and/or
     * generating the anser fail for some reason.
     * @throws ParseException if <tt>sdpOfferStr</tt> does not contain a valid
     * sdp string.
     */
    public String processSdpOffer(CallParticipant offerer, String sdpOfferStr)
        throws MediaException, ParseException
    {
        //first parse the offer
        SessionDescription sdpOffer = null;
        try
        {
            sdpOffer = mediaServCallback.getSdpFactory()
                .createSessionDescription(sdpOfferStr);
        }
        catch (SdpParseException ex)
        {
            throw new ParseException("Failed to parse SDPOffer: "
                                     + ex.getMessage()
                                     , ex.getCharOffset());
        }

        //create an sdp answer.
        SessionDescription sdpAnswer = createSessionDescription(sdpOffer, null);

        //extract the remote addresses.
        Vector mediaDescriptions = null;
        try
        {
            mediaDescriptions = sdpOffer.getMediaDescriptions(true);
        }
        catch (SdpException exc)
        {
            logger.error("failed to extract media descriptions", exc);
            throw new MediaException("failed to extract media descriptions"
                                    , MediaException.INTERNAL_ERROR
                                    , exc);
        }


        //add the RTP targets
        this.initStreamTargets(sdpOffer.getConnection(), mediaDescriptions);

        //create and init the streams (don't start streaming just yet but wait
        //for the call to enter the connected state).
        createSendStreams(mediaDescriptions);

        return sdpAnswer.toString();
    }

    /**
     * Creates a DataSource for all encodings in the mediaDescriptions vector
     * and initializes send streams in our rtp managers for every stream in the
     * data source.
     * @param mediaDescriptions a <tt>Vector</tt> containing
     * <tt>MediaDescription</tt> instances as sent by the remote side with their
     * SDP description.
     * @throws MediaException if we fail to create our data source with the
     * proper encodings and/or fail to initialize the RTP managers with the
     * necessary streams and/or don't find encodings supported by both the
     * remote participant and the local controller.
     */
    private void createSendStreams(Vector mediaDescriptions)
        throws MediaException
    {
        //extract the encodings these media descriptions specify
        Hashtable mediaEncodings
            = extractMediaEncodings(mediaDescriptions);

        //make our processor output in these encodings.
        DataSource dataSource = mediaServCallback.getMediaControl(getCall())
            .createDataSourceForEncodings(mediaEncodings);

        //get all the steams that our processor creates as output.
        PushBufferStream[] streams
            = ((PushBufferDataSource)dataSource).getStreams();

        //for each stream - determine whether it is a video or an audio
        //stream and assign it to the corresponding rtpmanager
        for (int i = 0; i < streams.length; i++)
        {
            RTPManager rtpManager = null;
            if(streams[i].getFormat() instanceof VideoFormat)
            {
                rtpManager = getVideoRtpManager();
            }
            else if (streams[i].getFormat() instanceof AudioFormat)
            {
                rtpManager = getAudioRtpManager();
            }
            else
            {
                logger.warn("We are apparently capable of sending a format "
                            +" that is neither videro nor audio. Is "
                            +"this really possible?:"
                            +streams[i].getFormat());
                continue;
            }

            try
            {
                SendStream stream = rtpManager.createSendStream(dataSource, i);

                logger.trace("Created a send stream for format "
                             + streams[i].getFormat());
            }
            catch (Exception exc)
            {
                throw new MediaException(
                    "Failed to create an RTP send stream for format "
                    + streams[i].getFormat()
                    , MediaException.IO_ERROR
                    , exc);
            }
        }
    }

    /**
     * Extracts the addresses that our interlocutor has sent for receiving media
     * and adds them as targets to our RTP manager.
     *
     * @param globalConnParam the global <tt>Connection</tt> (if there was one)
     * specified by our interlocutor outside any media description.
     * @param mediaDescriptions a Vector containing all media descriptions sent
     * by our interlocutor, that we'd use to verify whether connection level
     * parameters have been specified.
     *
     * @throws ParseException if there was a problem with the sdp
     * @throws MediaException if we simply fail to initialize the remote
     * addresses or set them as targets on our RTPManagers.
     */
    private void initStreamTargets(Connection globalConnParam,
                                   Vector mediaDescriptions)
        throws MediaException, ParseException
    {
        try
        {
            String globalConnectionAddress = null;

            if (globalConnParam != null)
                  globalConnectionAddress = globalConnParam.getAddress();

            Iterator mediaDescsIter = mediaDescriptions.iterator();
            while (mediaDescsIter.hasNext())
            {
                SessionAddress target = null;
                MediaDescription mediaDescription
                    = (MediaDescription) mediaDescsIter.next();

                int port = mediaDescription.getMedia().getMediaPort();
                String type = mediaDescription.getMedia().getMediaType();

                // If there\u2019s a global address, we use it.
                // If there isn\u2019t a global address, we get the address from
                // the media Description
                // Fix by Pablo L. - Telefonica
                String address;
                if (globalConnectionAddress != null)
                {
                    address = globalConnectionAddress;
                }
                else
                {
                    address = mediaDescription.getConnection().getAddress();
                }

                //check if we have a media level address
                Connection mediaLevelConnection = mediaDescription.
                    getConnection();

                if (mediaLevelConnection != null)
                {
                    address = mediaLevelConnection.getAddress();
                }

                InetAddress inetAddress = null;
                try
                {
                    inetAddress = InetAddress.getByName(address);
                }
                catch (UnknownHostException exc)
                {
                    throw new MediaException(
                        "Failed to resolve address " + address
                        , MediaException.NETWORK_ERROR
                        , exc);
                }

                //create the session address for this media type and add it to
                //the RTPManager.
                target = new SessionAddress(inetAddress, port);

                /** @todo the following line assumes that we have a single rtp
                 * manager per media type which is not necessarily true (e.g. we
                 * may two distinct video streams: 1 for a webcam video and another
                 * one desktop capture stream) */
                RTPManager rtpManager = type.equals("video")
                    ? getVideoRtpManager()
                    : getAudioRtpManager();

                try
                {
                    rtpManager.addTarget(target);
                    logger.trace("added target " + target
                                 + " for type " + type);
                }
                catch (Exception exc)
                {
                    throw new MediaException("Failed to add RTPManager target."
                        , MediaException.INTERNAL_ERROR
                        , exc);
                }
            }
        }
        catch(SdpParseException exc)
        {
            throw new ParseException("Failed to parse SDP data. Error on line "
                                     + exc.getLineNumber() + " "
                                     + exc.getMessage()
                                     , exc.getCharOffset());
        }
    }

    /**
     * Creates an SDP description of this session using the offer descirption
     * (if not null) for limiting. The intendedDestination parameter, which may
     * contain the address that the offer is to be sent to, will only be used if
     * the <tt>offer</tt> or its connection parameter are <tt>null</tt>. In the
     * oposite case we are using the address provided in the connection param as
     * an intended destination.
     *
     * @param offer the call participant meant to receive the offer or null if
     * we are to construct our own offer.
     * @param intendedDestination the address of the call participant that the
     * descriptions is to be sent to.
     * @return a SessionDescription of this CallSession.
     *
     * @throws MediaException code INTERNAL_ERROR if we get an SDP exception
     * while creating and/or parsing the sdp description.
     */
    private SessionDescription createSessionDescription(
                                            SessionDescription offer,
                                            InetAddress intendedDestination)
        throws MediaException
    {
        try
        {
            SessionDescription sessDescr
                = mediaServCallback.getSdpFactory().createSessionDescription();

            //"v=0"
            Version v = mediaServCallback.getSdpFactory().createVersion(0);

            sessDescr.setVersion(v);

            //we don't yet implement ice so just try to choose a local address
            //that corresponds to the address provided by the offer or as an
            //intended destination.
            /*NetworkAddressManagerService netAddressManager
                = MediaActivator.getNetworkAddressManagerService();*/

            if(offer != null)
            {
                Connection c = offer.getConnection();
                if(c != null)
                {
                    try
                    {
                        intendedDestination = InetAddress.getByName(c.
                            getAddress());
                    }
                    catch (SdpParseException ex)
                    {
                        logger.warn("error reading remote sdp. "
                                    + c.toString()
                                    + " is not a valid connection parameter.",
                                    ex);
                    }
                    catch (UnknownHostException ex)
                    {
                        logger.warn("error reading remote sdp. "
                                    + c.toString()
                                    + " does not contain a valid address.",
                                    ex);
                    }
                }
            }

            allocateMediaPorts(intendedDestination);

            InetAddress publicIpAddress = audioPublicAddress.getAddress();

            String addrType
                = publicIpAddress instanceof Inet6Address
                ? Connection.IP6
                : Connection.IP4;

            //spaces in the user name mess everything up.
            //bug report - Alessandro Melzi
            Origin o = mediaServCallback.getSdpFactory().createOrigin(
                call.getProtocolProvider().getAccountID().getUserID()
                , 0
                , 0
                , "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setOrigin(o);
            //c=
            Connection c = mediaServCallback.getSdpFactory().createConnection(
                "IN"
                , addrType
                , publicIpAddress.getHostAddress());

            sessDescr.setConnection(c);

            //"s=-"
            SessionName s
                = mediaServCallback.getSdpFactory().createSessionName("-");
            sessDescr.setSessionName(s);

            //"t=0 0"
            TimeDescription t
                = mediaServCallback.getSdpFactory().createTimeDescription();
            Vector timeDescs = new Vector();
            timeDescs.add(t);

            sessDescr.setTimeDescriptions(timeDescs);

            //media descriptions.
            Vector offeredMediaDescriptions  = null;
            if(offer != null)
                offeredMediaDescriptions = offer.getMediaDescriptions(false);

            logger.debug("Will create media descs with: audio public address="
                         + audioPublicAddress
                         + " and video public address="
                         + videoPublicAddress);

            Vector mediaDescs
                = createMediaDescriptions(offeredMediaDescriptions
                                        , audioPublicAddress
                                        , videoPublicAddress);

            sessDescr.setMediaDescriptions(mediaDescs);

            if (logger.isTraceEnabled())
            {
                logger.trace("Generated SDP - " + sessDescr.toString());
            }

            return sessDescr;
        }
        catch (SdpException exc)
        {
            throw new MediaException(
                "An SDP exception occurred while generating local "
                + "sdp description"
                , MediaException.INTERNAL_ERROR
                , exc);
        }

    }

    /**
     * Creates a vector containing SDP descriptions of media types and formats
     * that we support. If the offerVector is non null
     * @param offerMediaDescs the media descriptions sent by the offerer (could
     * be null).
     *
     * @param publicAudioAddress the <tt>InetSocketAddress</tt> that we should
     * be using for sending audio.
     * @param publicVideoAddress the <tt>InetSocketAddress</tt> that we should
     * be using for sending video.
     *
     * @return a <tt>Vector</tt> containing media descriptions that we support
     * and (if this is an answer to an offer) that the offering
     * <tt>CallParticipant</tt> supports as well.
     *
     * @throws SdpException we fail creating the media descriptions
     * @throws MediaException with code UNSUPPORTED_FORMAT_SET_ERROR if we don't
     * support any of the offered media formats.
     */
    private Vector createMediaDescriptions(
                                          Vector            offerMediaDescs,
                                          InetSocketAddress publicAudioAddress,
                                          InetSocketAddress publicVideoAddress)
        throws SdpException
              ,MediaException
    {
        //supported audio formats.
        String[] supportedAudioEncodings = mediaServCallback
            .getMediaControl(getCall())
                .getSupportedAudioEncodings();

        //supported video formats
        String[] supportedVideoEncodings = mediaServCallback
            .getMediaControl(getCall())
                .getSupportedVideoEncodings();

        //if there was an offer extract the offered media formats and use
        //the intersection between the formats we support and those in the
        //offer.
        if (offerMediaDescs != null && offerMediaDescs.size() > 0)
        {
            Vector offeredVideoEncodings = new Vector();
            Vector offeredAudioEncodings = new Vector();
            Iterator offerDescsIter = offerMediaDescs.iterator();

            while (offerDescsIter.hasNext())
            {
                MediaDescription desc
                    = (MediaDescription) offerDescsIter.next();
                Media media = desc.getMedia();
                String mediaType = media.getMediaType();

                if (mediaType.equalsIgnoreCase("video"))
                {
                    offeredVideoEncodings = media.getMediaFormats(true);
                    continue;
                }

                if (mediaType.equalsIgnoreCase("audio"))
                {
                    offeredAudioEncodings = media.getMediaFormats(true);
                    continue;
                }
            }

            //now intersect the offered encodings with what we support
            Hashtable encodings = new Hashtable(2);
            encodings.put("audio", offeredAudioEncodings);
            encodings.put("video", offeredVideoEncodings);
            encodings = intersectMediaEncodings(encodings);
            List intersectedAudioEncsList = (List)encodings.get("audio");
            List intersectedVideoEncsList = (List)encodings.get("video");

            //now replace the encodings arrays with the intersection
            supportedAudioEncodings
                = new String[intersectedAudioEncsList.size()];
            supportedVideoEncodings
                = new String[intersectedVideoEncsList.size()];

            for (int i = 0; i < supportedAudioEncodings.length; i++)
                supportedAudioEncodings[i]
                    = (String)intersectedAudioEncsList.get(i);

            for (int i = 0; i < supportedVideoEncodings.length; i++)
                supportedVideoEncodings[i]
                    = (String)intersectedVideoEncsList.get(i);

        }

        Vector mediaDescs = new Vector();

        if(supportedAudioEncodings.length > 0)
        {
            //--------Audio media description
            //make sure preferred formats come first
            MediaDescription am
                = mediaServCallback.getSdpFactory().createMediaDescription(
                    "audio"
                    , publicAudioAddress.getPort()
                    , 1
                    , "RTP/AVP"
                    , supportedAudioEncodings);

            if (!mediaServCallback.getDeviceConfiguration()
                .isAudioCaptureSupported())
            {
                am.setAttribute("recvonly", null);
            }
            mediaDescs.add(am);
        }
        //--------Video media description
        if(supportedVideoEncodings.length> 0)
        {
            //"m=video 22222 RTP/AVP 34";
            MediaDescription vm
                = mediaServCallback.getSdpFactory().createMediaDescription(
                    "video"
                    , publicVideoAddress.getPort()
                    , 1
                    , "RTP/AVP"
                    , supportedVideoEncodings);

            if (!mediaServCallback.getDeviceConfiguration()
                .isVideoCaptureSupported())
            {
                vm.setAttribute("recvonly", null);
            }
            mediaDescs.add(vm);
        }




        /** @todo record formats for participant. */

        return mediaDescs;
    }

    /**
     * Compares audio/video encodings in the <tt>offeredEncodings</tt>
     * hashtable with those supported by the currently valid media controller
     * and returns the set of those that were present in both. The hashtable
     * a maps "audio"/"video" specifier to a list of encodings present in both
     * the source <tt>offeredEncodings</tt> hashtable and the list of supported
     * encodings.
     *
     * @param offeredEncodings a Hashtable containing sets of encodings that an
     * interlocutor has sent to us.
     * @return a <tt>Hashtable</tt> mapping an "audio"/"video" specifier to a
     * list of encodings present in both the source <tt>offeredEncodings</tt>
     * hashtable and the list of encodings supported by the local media
     * controller.
     * @throws MediaException code UNSUPPORTED_FORMAT_SET_ERROR if the
     * intersection of both encoding sets does not contain any elements.
     */
    private Hashtable intersectMediaEncodings(Hashtable offeredEncodings)
        throws MediaException
    {
        //audio encodings supported by the media controller
        String[] supportedAudioEncodings = mediaServCallback
            .getMediaControl(getCall())
                .getSupportedAudioEncodings();

        //video encodings supported by the media controller
        String[] supportedVideoEncodings = mediaServCallback
            .getMediaControl(getCall())
                .getSupportedVideoEncodings();

        //audio encodings offered by the remote party
        List offeredAudioEncodings = (List)offeredEncodings.get("audio");

        //video encodings offered by the remote party
        List offeredVideoEncodings = (List)offeredEncodings.get("video");

        //recreate the formats we create according to what the other party
        //offered.
        List supportedAudioEncsList = Arrays.asList(supportedAudioEncodings);
        List intersectedAudioEncsList = new LinkedList();
        List supportedVideoEncsList = Arrays.asList(supportedVideoEncodings);
        List intersectedVideoEncsList = new LinkedList();

        //intersect supported audio formats with offered audio formats
        if (offeredAudioEncodings != null
            && offeredAudioEncodings.size() > 0)
        {
            Iterator offeredAudioEncsIter
                = offeredAudioEncodings.iterator();

            while (offeredAudioEncsIter.hasNext())
            {
                String format = (String) offeredAudioEncsIter.next();
                if (supportedAudioEncsList.contains(format))
                    intersectedAudioEncsList.add(format);
            }
        }

        if (offeredVideoEncodings != null
            && offeredVideoEncodings.size() > 0)
        {
            //intersect supported video formats with offered video formats
            Iterator offeredVideoEncsIter = offeredVideoEncodings.iterator();

            while (offeredVideoEncsIter.hasNext())
            {
                String format = (String) offeredVideoEncsIter.next();
                if (supportedVideoEncsList.contains(format))
                    intersectedVideoEncsList.add(format);
            }
        }

        //if the intersection contains no common formats then we need to
        //bail.
        if (intersectedAudioEncsList.size() == 0
            && intersectedVideoEncsList.size() == 0)
        {
            throw new MediaException(
                "None of the offered formats was supported by this "
                + "media implementation"
                , MediaException.UNSUPPORTED_FORMAT_SET_ERROR);

        }

        Hashtable intersection = new Hashtable(2);
        intersection.put("audio", intersectedAudioEncsList);
        intersection.put("video", intersectedVideoEncsList);

        return intersection;
    }
    /**
     * Returns a <tt>Hashtable</tt> mapping media types (e.g. audio or video)
     * to lists of JMF encoding strings corresponding to the SDP formats
     * specified in the <tt>mediaDescriptions</tt> vector.
     * @param mediaDescriptions a <tt>Vector</tt> containing
     * <tt>MediaDescription</tt> instances extracted from an SDP offer or
     * answer.
     * @return a <tt>Hashtable</tt> mapping media types (e.g. audio or video)
     * to lists of JMF encoding strings corresponding to the SDP formats
     * specified in the <tt>mediaDescriptions</tt> vector.

     */
    private Hashtable extractMediaEncodings(Vector mediaDescriptions)
    {
        Hashtable mediaEncodings = new Hashtable(2);

        Iterator descriptionsIter = mediaDescriptions.iterator();

        while(descriptionsIter.hasNext())
        {
            MediaDescription mediaDescription
                = (MediaDescription)descriptionsIter.next();
            Media media = mediaDescription.getMedia();
            Vector mediaFormats = null;
            String mediaType = null;
            try
            {
                mediaFormats = media.getMediaFormats(true);
                mediaType    = media.getMediaType();
            }
            catch (SdpParseException ex)
            {
                //this shouldn't happen since nist-sdp is not doing
                //lasy parsing but log anyway
                logger.warn("Error parsing sdp.",ex);
                continue;
            }


            if(mediaFormats.size() > 0)
            {
                List jmfEncodings = MediaUtils.sdpToJmfEncodings(mediaFormats);
                if(jmfEncodings.size() > 0)
                    mediaEncodings.put(mediaType, jmfEncodings);
            }
        }
        logger.trace("Possible media encodings="+mediaEncodings);
        return mediaEncodings;
    }

    /**
     * Create the RTP managers and bind them on some ports.
     */
    private void initializePortNumbers()
    {
        //first reset to default values
        minPortNumber = 5000;
        maxPortNumber = 6000;

        //then set to anything the user might have specified.
        String minPortNumberStr = networkAddressManagerServiceImpl.
                getConfigurationService().getString(
                    MediaService.MIN_PORT_NUMBER_PROPERTY_NAME);

        if (minPortNumberStr != null)
        {
            try{
                minPortNumber = Integer.parseInt(minPortNumberStr);
            }catch (NumberFormatException ex){
                logger.warn(minPortNumberStr
                            + " is not a valid min port number value. "
                            +"using min port " + minPortNumber);
            }
        }

        String maxPortNumberStr = networkAddressManagerServiceImpl.
                getConfigurationService().getString(
                MediaService.MAX_PORT_NUMBER_PROPERTY_NAME);

        if (maxPortNumberStr != null)
        {
            try{
                maxPortNumber = Integer.parseInt(maxPortNumberStr);
            }catch (NumberFormatException ex){
                logger.warn(maxPortNumberStr
                            + " is not a valid max port number value. "
                            +"using max port " + maxPortNumber,
                            ex);
            }
        }
    }

    /**
     * Allocates a local port for the RTP manager, tries to obtain a public
     * address for it and after succeeding makes the network address manager
     * protect the address until we are ready to bind on it.
     *
     * @param intendedDestination a destination that the rtp manager would be
     * communicating with.
     * @param sessionAddress the sessionAddress that we're locally bound on.
     * @param bindRetries the number of times that we need to retry a bind.
     *
     * @return the SocketAddress the public address that the network address
     * manager returned for the session address that we're bound on.
     *
     * @throws MediaException if we fail to initialize rtp manager.
     */
    private InetSocketAddress allocatePort(InetAddress intendedDestination,
                                           SessionAddress sessionAddress,
                                           int bindRetries)
        throws MediaException
    {
        InetSocketAddress publicAddress = null;
        boolean initialized = false;

        /*NetworkAddressManagerService netAddressManager
                    = MediaActivator.getNetworkAddressManagerService();*/



        //try to initialize a public address for the rtp manager.
        for (int i = bindRetries; i > 0; i--)
        {
            //first try to obtain a binding for the address.
            try
            {
                publicAddress = networkAddressManagerServiceImpl.
                        getPublicAddressFor(intendedDestination,
                                         sessionAddress.getDataPort());
                initialized =true;
                break;
            }
            catch (IOException ex)
            {
                logger.warn("Retrying a bind because of a failure. "
                            + "Failed Address is: "
                            + sessionAddress.toString(), ex);

                //reinit the session address we tried with and prepare to retry.
                sessionAddress
                    .setDataPort(sessionAddress.getDataPort()+2);
                sessionAddress
                    .setControlPort(sessionAddress.getControlPort()+2);
            }

        }

        if(!initialized)
            throw new MediaException("Failed to bind to a local port in "
                                     + Integer.toString(bindRetries)
                                     + " tries."
                                     , MediaException.INTERNAL_ERROR);

        return publicAddress;
    }

    /**
     * Looks for free ports and initializes the RTP manager according toe the 
     * specified <tt>intendedDestination</tt>.
     * 
     * @param intendedDestination the InetAddress that we will be transmitting
     * to. 
     * @throws MediaException if we fail initializing the RTP managers.
     */
    private void allocateMediaPorts(InetAddress intendedDestination)
        throws MediaException
    {
        InetAddress inAddrAny = null;

        try
        {
            //create an ipv4 any address since it also works when accepting
            //ipv6 connections.
            inAddrAny = InetAddress.getByName(NetworkUtils.IN_ADDR_ANY);
        }
        catch (UnknownHostException ex)
        {
            //this shouldn't happen.
            throw new MediaException("Failed to create the ANY inet address."
                                     , MediaException.INTERNAL_ERROR
                                     , ex);
        }

        //check the number of times that we'd have to rety binding to local
        //ports before giving up.
        String bindRetriesStr
            = networkAddressManagerServiceImpl.getConfigurationService().getString(
                MediaService.BIND_RETRIES_PROPERTY_NAME);

        int bindRetries = MediaService.BIND_RETRIES_DEFAULT_VALUE;
        try
        {
            if(bindRetriesStr != null && bindRetriesStr.length() > 0)
                bindRetries = Integer.parseInt(bindRetriesStr);
        }
        catch (NumberFormatException ex)
        {
            logger.warn(bindRetriesStr
                        + " is not a valid value for number of bind retries."
                        , ex);
        }

        //initialize audio rtp manager.
        audioSessionAddress = new SessionAddress(inAddrAny, minPortNumber);
        audioPublicAddress = allocatePort(intendedDestination,
                                          audioSessionAddress,
                                          bindRetries);

        logger.debug("AudioSessionAddress="+audioSessionAddress);
        logger.debug("AudioPublicAddress="+audioPublicAddress);

        //augment min port number so that no one else tries to bind here.
        minPortNumber = audioSessionAddress.getDataPort() + 2;

        //initialize video rtp manager.
        videoSessionAddress = new SessionAddress(inAddrAny, minPortNumber);
        videoPublicAddress = allocatePort(intendedDestination,
                                          videoSessionAddress,
                                          bindRetries);

        //augment min port number so that no one else tries to bind here.
        minPortNumber = videoSessionAddress.getDataPort() + 2;

        //if we have reached the max port number - reinit.
        if(minPortNumber > maxPortNumber -2)
            initializePortNumbers();

        //now init the rtp managers and make them bind
        //initializeRtpManager(audioRtpManager, audioSessionAddress);
        //initializeRtpManager(videoRtpManager, videoSessionAddress);
    }

    /**
     * Initializes the RTP manager so that it would start listening on the
     * <tt>address</tt> session address. The method also initializes the RTP
     * manager buffer control.
     *
     * @param rtpManager the <tt>RTPManager</tt> to initialize.
     * @param bindAddress the <tt>SessionAddress</tt> to use when initializing the
     * RTPManager.
     *
     * @throws MediaException if we fail to initialize the RTP manager.
     */
    private void initializeRtpManager(RTPManager rtpManager,
                                      SessionAddress bindAddress)
        throws MediaException
    {
        try
        {
            rtpManager.initialize(bindAddress);
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            logger.error("Failed to init an RTP manager.", exc);
            throw new MediaException("Failed to init an RTP manager."
                                     , MediaException.IO_ERROR
                                     , exc);
        }


        //it appears that if we don't do this managers don't play
        // You can try out some other buffer size to see
        // if you can get better smoothness.
        BufferControl bc = (BufferControl)rtpManager
            .getControl(BufferControl.class.getName());
        if (bc != null)
        {
            long buff = 500;
            String buffStr = networkAddressManagerServiceImpl.
                    getConfigurationService().getString(
                        PROPERTY_NAME_RECEIVE_BUFFER_LENGTH);
            try
            {
                if(buffStr != null && buffStr.length() > 0)
                    buff = Long.parseLong(buffStr);
            }
            catch (NumberFormatException exc)
            {
                logger.warn(buffStr
                            + " is not a valid receive buffer value (integer)."
                            , exc);
            }

            buff = bc.setBufferLength(buff);
            logger.trace("set receiver buffer len to=" + buff);
            bc.setEnabledThreshold(true);
            bc.setMinimumThreshold(100);
        }

        //add listeners
        rtpManager.addReceiveStreamListener(this);
        rtpManager.addSendStreamListener(this);
        rtpManager.addSessionListener(this);
    }
    
    /**
     * Registers the RTP formats which are supported by SIP Communicator in addition
     * to the JMF standard formats. This has to be done for every RTP Manager instance.
     *  
     * @param rtpManager The manager with which to register the formats.
     * @see MediaControl#registerCustomCodecs()
     */
    static void registerCustomCodecFormats(RTPManager rtpManager)
    {
        for (int i=0; i<CUSTOM_CODEC_FORMATS.length; i++) 
        {
            javax.media.Format format = CUSTOM_CODEC_FORMATS[i];
            logger.debug("registering format " + format + " with RTP manager");
            /*
             * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat 
             * leaks memory, since it stores the Format in a static Vector. 
             * AFAIK there is no easy way around it, but the memory impact 
             * should not be too bad.
             */
            rtpManager.addFormat(
                format, MediaUtils.jmfToSdpEncoding(format.getEncoding()));
        }
    }

    /**
     * Indicates that a change has occurred in the state of the source call.
     * @param evt the <tt>CallChangeEvent</tt> instance containing the source
     * calls and its old and new state.
     */
    public void callStateChanged(CallChangeEvent evt)
    {
        if( evt.getNewValue() == CallState.CALL_IN_PROGRESS
            && evt.getNewValue() != evt.getOldValue())
        {
            try
            {
                startStreaming();
                mediaServCallback.getMediaControl(getCall())
                    .startProcessingMedia(this);
            }
            catch (MediaException ex)
            {
                /** @todo need to notify someone */
                logger.error("Failed to start streaming.", ex);
            }
        }
        else if( evt.getNewValue() == CallState.CALL_ENDED
                 && evt.getNewValue() != evt.getOldValue())
        {
            stopStreaming();
            mediaServCallback.getMediaControl(getCall())
                .stopProcessingMedia(this);

            //close all players that we have created in this session
            Iterator playersIter = players.iterator();

            while(playersIter.hasNext())
            {
                Player player = ( Player )playersIter.next();
                player.stop();
                player.deallocate();
                player.close();
                playersIter.remove();
            }


            //close all video frames that we have created in this session
            Iterator videoFramesIter = videoFrames.iterator();
            while(videoFramesIter.hasNext())
            {
                javax.swing.JFrame frame
                    = ( javax.swing.JFrame )videoFramesIter.next();
                frame.setVisible(false);
                frame.dispose();
                videoFramesIter.remove();
            }

            //remove ourselves as listeners from the call
            evt.getSourceCall().removeCallChangeListener(this);

            RTPManager audioRtpMan = getAudioRtpManager();

            if(audioRtpMan != null)
                audioRtpMan.dispose();

            RTPManager videoRtpMan = getVideoRtpManager();
            if(videoRtpMan != null)
                videoRtpMan.dispose();
        }
    }


    /**
     * Indicates that a change has occurred in the status of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        /** @todo implement participantStateChanged() */
        /** @todo remove target for participant. */
    }

    /**
     * Indicates that a new call participant has joined the source call.
     * @param evt the <tt>CallParticipantEvent</tt> containing the source call
     * and call participant.
     */
    public synchronized void callParticipantAdded(CallParticipantEvent evt)
    {
        CallParticipant sourceParticipant = evt.getSourceCallParticipant();
        sourceParticipant.addCallParticipantListener(this);
    }

    /**
     * Indicates that a call participant has left the source call.
     * @param evt the <tt>CallParticipantEvent</tt> containing the source call
     * and call participant.
     */
    public void callParticipantRemoved(CallParticipantEvent evt)
    {

    }

    //-------- dummy implementations of listener methods that we don't need
    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantTransportAddressChanged(
                                CallParticipantChangeEvent evt)
    {
        /** @todo i am not sure we should be ignoring this one ... */
    }

    /**
     * Ignore - we're not concerned by this event inside a call session.
     *
     * @param evt ignore.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {
    }

    //implementation of jmf listener methods
    /**
     * Method called back in the SessionListener to notify
     * listener of all Session Events.SessionEvents could be one
     * of NewParticipantEvent or LocalCollisionEvent.
     *
     * @param event the newly received SessionEvent
     */
    public synchronized void update(SessionEvent event)
    {
        if (event instanceof NewParticipantEvent)
        {
            Participant participant
                = ( (NewParticipantEvent) event).getParticipant();
            if (logger.isDebugEnabled())
            {
                logger.debug("A new participant had just joined: "
                             + participant.getCNAME());
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                    "Received the following JMF Session event - "
                    + event.getClass().getName() + "=" + event);
            }
        }

    }
    /**
     * Method called back in the RTPSessionListener to notify
     * listener of all SendStream Events.
     *
     * @param event the newly received SendStreamEvent
     */
    public synchronized void update(SendStreamEvent event)
    {
        logger.debug(
            "received the following JMF SendStreamEvent - "
            + event.getClass().getName() + "="+ event);

    }

    /**
     * Method called back in the RTPSessionListener to notify
     * listener of all ReceiveStream Events.
     *
     * @param evt the newly received ReceiveStreamEvent
     */
    public synchronized void update(ReceiveStreamEvent evt)
    {
        RTPManager mgr = (RTPManager) evt.getSource();
        Participant participant = evt.getParticipant(); // could be null.
        ReceiveStream stream = evt.getReceiveStream(); // could be null.
        if (evt instanceof NewReceiveStreamEvent)
        {
            try
            {
                logger.debug("received a new incoming stream. " + evt);
                stream = ( (NewReceiveStreamEvent) evt).getReceiveStream();
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl(
                    "javax.media.rtp.RTPControl");
                if (logger.isDebugEnabled())
                {
                    if (ctl != null)
                    {
                        logger.debug("Received new RTP stream: "
                                     + ctl.getFormat());
                    }
                    else
                    {
                        logger.debug("Received new RTP stream");
                    }
                }

                Player player = null;
                //if we are using a custom destination, create a processor
                //if not, a player will suffice
                if (dataSink != null)
                {
                    player = Manager.createProcessor(ds);
                }
                else
                {
                    player = Manager.createPlayer(ds);
                }
                 player.addControllerListener(this);

                //a processor needs to be configured then realized.
                if (dataSink !=  null)
                {
                    ((Processor)player).configure();
                }
                else
                {
                    player.realize();
                }

                players.add(player);
            }
            catch (Exception e)
            {
                logger.error("NewReceiveStreamEvent exception ", e);
                return;
            }
        }
        else if (evt instanceof StreamMappedEvent)
        {
            if (stream != null && stream.getDataSource() != null)
            {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl(
                    "javax.media.rtp.RTPControl");
                if (logger.isDebugEnabled())
                {
                    String msg = "The previously unidentified stream ";
                    if (ctl != null)
                    {
                        msg += ctl.getFormat();
                    }
                    msg += " had now been identified as sent by: "
                        + participant.getCNAME();
                    logger.debug(msg);
                }
            }
        }
        else if (evt instanceof ByeEvent)
        {
            logger.debug("Got \"bye\" from: " + participant.getCNAME());
        }
    }

    /**
     * This method is called when an event is generated by a
     * <code>Controller</code> that this listener is registered with.
     * @param ce The event generated.
     */
    public synchronized void controllerUpdate(ControllerEvent ce)
    {
        logger.debug("Received a ControllerEvent: " + ce);
        Player player = (Player) ce.getSourceController();

        if (player == null)
        {
            return;
        }

        //if configuration is completed and this is a processor
        //we need to set file format and explicitly call realize().
        if (ce instanceof ConfigureCompleteEvent)
        {
            try
            {
                ((Processor)player).setContentDescriptor(
                        new FileTypeDescriptor(FileTypeDescriptor.WAVE));
                player.realize();
            }
            catch (Exception exc)
            {
                logger.error("failed to record to file", exc);
            }
        }

        // Get this when the internal players are realized.
        if (ce instanceof RealizeCompleteEvent)
        {

            //set the volume as it is not on max by default.
            //XXX: I am commenting this since apparently it is causing some 
            //problems on windows.
            //GainControl gc
            //    = (GainControl)player.getControl(GainControl.class.getName());
            //if (gc != null)
            //{
            //    logger.debug("Setting volume to max");
            //    gc.setLevel(1);
            //}
            //else
            //    logger.debug("Player does not have gain control.");


            logger.debug("A player was realized and will be started.");
            player.start();

            if (dataSink != null)
            {
                try
                {
                    logger.info("starting recording to file: "+dataSink);
                    MediaLocator dest = new MediaLocator(dataSink);    
                    DataSource ds = ((Processor)player).getDataOutput();
                    DataSink sink = Manager.createDataSink(
                        ((Processor)player).getDataOutput(), dest);
                    player.start();
                    //do we know the output file's duration
                    RecordInitiator record = new RecordInitiator(sink);
                    record.start();
                }
                catch(Exception e)
                {
                    logger.error("failed while trying to record to file",e);
                }
            }
            else
            {
                player.start();
            }


            /** @todo video frame is currently handled with very ugly test code
             * please don't forget to remove */
            //------------ ugly video test code starts here --------------------
            java.awt.Component vc = player.getVisualComponent();
            if(vc != null)
            {
                javax.swing.JFrame frame = new javax.swing.JFrame();
                frame.setTitle("SIP Communicator - Video Call");
                frame.getContentPane().add(vc);
                frame.pack();
                //center
                java.awt.Dimension frameSize = frame.getSize();

                //ugly resize if too tiny
                if(frameSize.width < 300)
                {
                    frame.setSize(frameSize.width * 2, frameSize.height * 2);
                    frameSize = frame.getSize();
                }
                java.awt.Dimension screenSize
                    = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

                frame.setLocation((screenSize.width - frameSize.width)/2
                    ,(screenSize.height - frameSize.height)/2);

                frame.setVisible(true);
                videoFrames.add(frame);
            }
            //------------- ugly video test code ends here ---------------------
        }
        if (ce instanceof StartEvent) {
            logger.debug("Received a StartEvent");
        }
        if (ce instanceof ControllerErrorEvent) {
            logger.error(
                "The following error was reported while starting a player"
                + ce);
        }
        if (ce instanceof ControllerClosedEvent) {
            logger.debug("Received a ControllerClosedEvent");
        }
    }

    /**
     * The record initiator is started after taking a call that is supposed to
     * be answered by a mailbox plug-in. It waits for the outgoing message to 
     * stop transmitting and starts recording whatever comes after that.
     */
    private class RecordInitiator extends Thread
    {
        private DataSink sink;

        public RecordInitiator(DataSink sink)
        {
            this.sink = sink;
        }

        public void run()
        {
            //determine how long to wait for the outgoing 
            //message to stop playing
            javax.media.Time timeToWait = mediaServCallback
                                    .getMediaControl(call)
                                    .getOutputDuration();

            //if the time is unknown, we will start recording immediately
            if (timeToWait != javax.media.Time.TIME_UNKNOWN)
            {
                double millisToWait = timeToWait.getSeconds() * 1000;
                long timeStartedPlaying = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeStartedPlaying
                         + millisToWait)
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error("Interrupted while waiting to start "
                            + "recording incoming message",e);
                    }
                }
            }

            //open the dataSink and start recording
            try
            {
                sink.open();
                sink.start();
            }
            catch (IOException e)
            {
                logger.error("IO Exception while attempting to start "
                             + "recording incoming message",e);
            }
        }
    }

}
