/* Firmador is a program to sign documents using AdES standards.

Copyright (C) Firmador authors.

This file is part of Firmador.

Firmador is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Firmador is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Firmador.  If not, see <http://www.gnu.org/licenses/>.  */

package cr.libre.firmador.remote;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import cr.libre.firmador.MessageUtils;
import cr.libre.firmador.connections.Connection;
import cr.libre.firmador.gui.GUISwing;
import cr.libre.firmador.gui.swing.RemoteDocInformation;
import cr.libre.firmador.gui.swing.RequestHostAuthorizationRemote;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;

//import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
//import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cr.libre.firmador.Settings;
import cr.libre.firmador.SettingsManager;
import cr.libre.firmador.cards.CardSignInfo;
import cr.libre.firmador.cards.SmartCardDetector;
import cr.libre.firmador.gui.GUIInterface;
import cr.libre.firmador.gui.swing.RequestPinWindowRemote;
import cr.libre.firmador.signers.BasicSigner;
import cr.libre.firmador.signers.FirmadorUtils;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;


public class RemoteHttpWorker<T, V> extends SwingWorker<T, V> {
    final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public final static int API_ACTION_SIGN = 1;
	public final static int API_ACTION_LIST_CARDS = 2;


    protected GUIInterface gui;
    private Connection connection;
    private HttpServer server;
    //private String requestFileName;
    private final List<String> errorList = new ArrayList<>();
    //private final List<String> lastRequest = new ArrayList<>();

	protected HashMap<String, RemoteDocInformation> docInformation = new HashMap<>();

    public RemoteHttpWorker(GUIInterface gui, Connection connection) {
        super();
        this.gui=gui;
        this.connection = connection;
    }

	public void stop() {
		server.stop();
		this.cancel(true);
	}

	public HashMap<String, RemoteDocInformation> getDocInformation() {
		return docInformation;
	}

    protected T doInBackground() throws IOException, InterruptedException {
        class RequestHandler implements HttpRequestHandler {
			protected Settings settings;
            protected GUIInterface gui;
			private SmartCardDetector smartCardDetector;


            public RequestHandler(GUIInterface gui, Settings settings) {
                super();
                this.gui = gui;
				this.settings = settings;
				if (smartCardDetector == null)
					smartCardDetector = new SmartCardDetector();
            }

			public void processSign(String name, RemoteDocInformation data) {
				gui.loadRemoteDocument(name);
			}
			private void getCertificates(final ClassicHttpRequest request, final ClassicHttpResponse response) {
				List<CardSignInfo> cards = null;
				byte[] jsonBytes = null;
				try {
					cards = smartCardDetector.readListSmartCard();
					ObjectMapper mapper = new ObjectMapper();
					jsonBytes = mapper.writeValueAsBytes(cards);
					sendResponse(response, jsonBytes);
				} catch (Throwable e) {
					e.printStackTrace();
					LOG.error(MessageUtils.t("remote_http_worker_error_get_cards"), e);
				}
			}

			private CardSignInfo getCardInfoByNumberID(String serialnumber) {
				List<CardSignInfo> cards = null;
				CardSignInfo result = null;
				String card_serialnumber;
				try {
					cards = smartCardDetector.readListSmartCard();
                    smartCardDetector.setCardInfo(cards);
					for (CardSignInfo card : cards) {
						card_serialnumber = card.getCertificate().getSerialNumber().toString();
						if (card_serialnumber.equals(serialnumber)) {
							result = card;
							break;
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					LOG.error(MessageUtils.t("remote_http_worker_error_get_cards"), e);
				}
				return result;
			}

			private void doRegister(final ClassicHttpRequest request, final ClassicHttpResponse response) throws Throwable {
				String requestOrigin = String.valueOf(request.getHeader("Origin")).replace("Origin:", "").trim();
				List<String> allowedOrigins = settings.getAllowedHosts();
				if (allowedOrigins.contains(requestOrigin)) {
					response.setCode(HttpStatus.SC_ACCEPTED);
				}else {
					RequestHostAuthorizationRemote pinrequest = new RequestHostAuthorizationRemote();
					int ok = pinrequest.showAndWait(requestOrigin);
					if (ok == 1) {
						allowedOrigins.add(requestOrigin);
						settings.setRegisteredAllowedOrigins(allowedOrigins);
						SettingsManager settingsManager = SettingsManager.getInstance();
						settingsManager.setSettings(settings, true);
						response.setCode(HttpStatus.SC_OK);
                        settings.removeNoAuthorizedHost(requestOrigin);
                        ((GUISwing) gui).reloadConfig();
					}else if (ok == 2){
						settings.addTempAllowedHost(requestOrigin);
						response.setCode(HttpStatus.SC_OK);
                        settings.removeNoAuthorizedHost(requestOrigin);
                        ((GUISwing) gui).reloadConfig();
					}else{
						response.setCode(HttpStatus.SC_FORBIDDEN);
					}
				}

			}

			private void multipleSign(final ClassicHttpRequest request, final ClassicHttpResponse response) throws Throwable {
				String jsonobj = this.parseBody(request);
				ObjectMapper mapper = new ObjectMapper();
				List<FirmadorRemoteDocument> docRequests = Arrays.asList(mapper.readValue(jsonobj, FirmadorRemoteDocument[].class));

				if (docRequests.isEmpty()) {
					response.setCode(HttpStatus.SC_BAD_REQUEST);
					return;
				}

				// Obtener información de la tarjeta de la primera solicitud
				CardSignInfo card = this.getCardInfoByNumberID(docRequests.get(0).getSerialnumber());
				if (card == null) {
					response.setCode(HttpStatus.SC_BAD_REQUEST);
					return;
				}

				BasicSigner signer = new BasicSigner(gui);
				RequestPinWindowRemote pinrequest = new RequestPinWindowRemote();
				pinrequest.setCard(card);
				pinrequest.setDocumentName(MessageUtils.t("remote_http_worker_multiple_sign_documents"));
				pinrequest.setIcon(null); // No se usa una imagen específica

				int ok = pinrequest.showandwait();
				if (ok != 0) {
					response.setCode(HttpStatus.SC_NOT_ACCEPTABLE);
					return;
				}

				List<RemoteSignatureValueDTO> signedDocuments = new ArrayList<>();
				for (FirmadorRemoteDocument docRequest : docRequests) {
					SignatureValueDTO signature = signer.sign(card, docRequest.getTobesigned());
					signedDocuments.add(new RemoteSignatureValueDTO(docRequest, signature));
				}

				ObjectMapper responseMapper = new ObjectMapper();
				byte[] jsonBytes = responseMapper.writeValueAsBytes(signedDocuments);
				sendResponse(response, jsonBytes);
			}

			private void sign(final ClassicHttpRequest request, final ClassicHttpResponse response) throws Throwable {
				String jsonobj = this.parseBody(request);
				ObjectMapper mapper = new ObjectMapper();
				FirmadorRemoteDocument docrequest = mapper.readValue(jsonobj, FirmadorRemoteDocument.class);
				CardSignInfo card = this.getCardInfoByNumberID(docrequest.getSerialnumber());
				BasicSigner signer = new BasicSigner(gui);
				if(card != null) {
					RequestPinWindowRemote pinrequest = new RequestPinWindowRemote();
					pinrequest.setCard(card);
					pinrequest.setDocumentName(docrequest.getDocumentName());
					pinrequest.setIcon(docrequest.getImageIcon());
					int ok = pinrequest.showandwait();
					if (ok == 0) {
						SignatureValueDTO signature = signer.sign(card, docrequest.getTobesigned());
						RemoteSignatureValueDTO rsignature = new RemoteSignatureValueDTO(docrequest, signature);
						ObjectMapper responsemapper = new ObjectMapper();
						byte[] jsonBytes = responsemapper.writeValueAsBytes(rsignature);
                        smartCardDetector.restoreSessions();
						sendResponse(response, jsonBytes);
					} else {
						response.setCode(HttpStatus.SC_NOT_ACCEPTABLE);
					}
				} else {
					response.setCode(HttpStatus.SC_BAD_REQUEST);
				}
			}

			private void authenticate(final ClassicHttpRequest request, final ClassicHttpResponse response)
					throws Throwable {
				String jsonobj = this.parseBody(request);
				ObjectMapper mapper = new ObjectMapper();
				AuthenticationRequest authrequest = mapper.readValue(jsonobj, AuthenticationRequest.class);
				CardSignInfo card = this.getCardInfoByNumberID(authrequest.getSerialnumber());
				AuthenticatorSigner signer = new AuthenticatorSigner(gui);
				if (card != null) {
					authrequest.setGui(gui);
					authrequest.setCertificate(card.getCertificate());
					RequestPinWindowRemote pinrequest = new RequestPinWindowRemote();
					pinrequest.setCard(card);
					pinrequest.setIcon(authrequest.getImageIcon());
					pinrequest.setDocumentName("<br>Solicitud de autenticación con la información<br>"
							+ "Dominio: <strong>"
							+ authrequest.getDomain() + "</strong> " + "<br>Código: <strong>"
							+ authrequest.getShortAuthCode() + "</strong>");
					int ok = pinrequest.showandwait();
					if (ok == 0) {
						RemoteDocument doc = signer.signAuthentication(card, authrequest.getSignDocument());
						ObjectMapper responsemapper = new ObjectMapper();
						byte[] jsonBytes = responsemapper.writeValueAsBytes(doc);
						sendResponse(response, jsonBytes);
					} else {
						response.setCode(HttpStatus.SC_NOT_ACCEPTABLE);
					}

				} else {
					response.setCode(HttpStatus.SC_BAD_REQUEST);

				}

			}

			private boolean determineAPIAction(final ClassicHttpRequest request, final ClassicHttpResponse response)
					throws Throwable {

				boolean responsed = false;
				if (request.getUri().getPath().equals("/certificates")) {
					this.getCertificates(request, response);
					responsed = true;
				}  else if (request.getUri().getPath().startsWith("/multipleSign")) {
					if (!request.getMethod().contains("OPTIONS")) {
						this.multipleSign(request, response);
						responsed = true;
					} else {
						response.setCode(HttpStatus.SC_NO_CONTENT);
						responsed = true;
					}
				}else if (request.getUri().getPath().startsWith("/sign")) {
					if (!request.getMethod().contains("OPTIONS")) {
						this.sign(request, response);
						responsed = true;
					} else {
						response.setCode(HttpStatus.SC_NO_CONTENT);
						responsed = true;
					}
				} else if (request.getUri().getPath().startsWith("/authenticate")) {
					if (!request.getMethod().contains("OPTIONS")) {
						this.authenticate(request, response);
						responsed = true;
					} else {
						response.setCode(HttpStatus.SC_NO_CONTENT);
						responsed = true;
					}
				} else if (request.getUri().getPath().startsWith("/ok")) {
					response.setCode(HttpStatus.SC_NO_CONTENT);
					responsed = true;
				}else if (request.getUri().getPath().startsWith("/doRegister")) {
					this.doRegister(request, response);
					responsed = true;
				}
				return responsed;
            }

			public String parseBody(final ClassicHttpRequest request) throws Throwable {
				InputStream content = request.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8));
				String text = reader.lines().collect(Collectors.joining("\n"));
				return text;
			}

			private Boolean isAllowedOrigin(String requestOrigin) {
				List<String> allowedOrigins = settings.getAllowedHosts();
				List<String> normalizedAllowedOrigins;

				normalizedAllowedOrigins = allowedOrigins.stream()
						.map(String::toLowerCase)
						.map(origin -> origin.replaceAll("/$", ""))
						.map(String::trim)
						.toList();

				return normalizedAllowedOrigins.contains(requestOrigin);
			}

			public void handle(final ClassicHttpRequest request, final ClassicHttpResponse response, final HttpContext context) throws HttpException, IOException {
				String requestOrigin = String.valueOf(request.getHeader("Origin")).replace("Origin:", "").trim();
                response.setHeader("Access-Control-Allow-Private-Network", "true");
                
				try {
					if (request.getUri().getPath().startsWith("/doRegister")) {
						response.setHeader("Access-Control-Allow-Origin", "*");
					} else {
						if (isAllowedOrigin(requestOrigin)) {
							response.setHeader("Access-Control-Allow-Origin", requestOrigin);
						} else {
                            settings.addNoAuthorizedHost(requestOrigin);
                            errorList.add(MessageUtils.t("remote_http_worker_error_origin_not_allowed")  + requestOrigin);
							response.setHeader("Access-Control-Allow-Origin", "null");
							notifyErrorConnection();
							response.setCode(HttpStatus.SC_FORBIDDEN);
							return;
						}
					}
				} catch (URISyntaxException e) {
					LOG.error(MessageUtils.t("remote_http_worker_error_normalizing_origin"), e);
                    errorList.add(MessageUtils.t("remote_http_worker_error_normalizing_origin"));
				}

				response.setHeader("Vary", "Origin");
				response.setHeader("Referrer-Policy", "unsafe-url");
				response.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
				response.addHeader("Access-Control-Allow-Headers", "*");

				boolean success = false;
				try {
					success = determineAPIAction(request, response);
				} catch (URISyntaxException e) {
					LOG.error("Error URISyntaxException", e);
                    errorList.add(FirmadorUtils.getRootCause(e).toString());
					gui.showError(FirmadorUtils.getRootCause(e));
					response.setCode(HttpStatus.SC_NO_CONTENT);
					notifyErrorConnection();
					return;
				} catch (Throwable e) {
					LOG.error("Error procesando petición", e);
                    errorList.add(FirmadorUtils.getRootCause(e).toString());
					gui.showError(FirmadorUtils.getRootCause(e));
					response.setCode(HttpStatus.SC_NO_CONTENT);
					notifyErrorConnection();
					return;
				}

				if (!success) {
					try {
						if ("/close".equals(request.getUri().getPath())) {
							response.setCode(HttpStatus.SC_OK);
							LOG.info("Closing...");
							response.close();
							SwingUtilities.invokeLater(() -> {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
                                    errorList.add(MessageUtils.t("remote_http_worker_error_interrupted"));
									notifyErrorConnection();
									LOG.error(MessageUtils.t("remote_http_worker_error_interrupted"), e);
								}
							});
							return;
						}

						String requestFileName = request.getUri().getPath().substring(1);

						if ("DELETE".equalsIgnoreCase(request.getMethod())) {
							if (docInformation.containsKey(requestFileName)) {
								docInformation.remove(requestFileName);
								response.setCode(HttpStatus.SC_SUCCESS);
							} else {
								response.setCode(HttpStatus.SC_NOT_FOUND);
							}
							return;
						}
					} catch (URISyntaxException e) {
						LOG.error("Error URISyntaxException", e);
                        errorList.add(FirmadorUtils.getRootCause(e).toString());
						gui.showError(FirmadorUtils.getRootCause(e));
					} catch (Exception e) {
                        errorList.add(FirmadorUtils.getRootCause(e).toString());
						LOG.error(MessageUtils.t("remote_http_worker_error_processing_request"), e);
					}

					HttpEntity entity = request.getEntity();
					response.setCode(HttpStatus.SC_ACCEPTED);
                    String requestFileName = null;
                    try {
                        requestFileName = request.getUri().getPath().substring(1);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    RemoteDocInformation docinfo;

					if (!docInformation.containsKey(requestFileName)) {
						docinfo = new RemoteDocInformation(requestFileName, new ByteArrayOutputStream(), HttpStatus.SC_ACCEPTED);
						if (entity.getContentLength() > 0) {
							docinfo.setInputdata(entity.getContent());
							publish();
							docInformation.put(requestFileName, docinfo);
							processSign(requestFileName, docinfo);
						} else {
							docinfo.setStatus(HttpStatus.SC_NO_CONTENT);
						}
					} else {
						docinfo = docInformation.get(requestFileName);
					}

					if (docinfo.getStatus() != HttpStatus.SC_NO_CONTENT) {
						response.setEntity(new ByteArrayEntity(docinfo.getData().toByteArray(), ContentType.DEFAULT_TEXT));
					}
					response.setCode(docinfo.getStatus());
				}
                if (!errorList.isEmpty()) {
                    notifyErrorConnection();
                }

			}

		};
        Settings settings = SettingsManager.getInstance().getAndCreateSettings();
        try {
            server = ServerBootstrap.bootstrap().setListenerPort(settings.portNumber)
                .setLocalAddress(InetAddress.getLoopbackAddress()).setSslContext(null)
                .register("localhost", "*",
                    new RequestHandler(gui, settings)).create();


            server.start();
            server.awaitTermination(TimeValue.MAX_VALUE);
        } catch (Exception e) {
            errorList.add(MessageUtils.t("remote_http_worker_error_starting_server") +" "+settings.portNumber);
            LOG.error(MessageUtils.t("remote_http_worker_error_starting_server") +" " +settings.portNumber);
            notifyErrorConnection();

            this.cancel(true);
            return null;
        }
        return null;
    }

	private void sendResponse(ClassicHttpResponse response, byte[] payload) {

    	ByteArrayEntity entity = new ByteArrayEntity(payload, ContentType.APPLICATION_JSON);
		response.setEntity(entity);
		response.setCode(HttpStatus.SC_OK);
		response.setReasonPhrase("OK");
    }

/*
    private void sendResponse(ClassicHttpResponse response, String payload) {
		sendResponse(response, payload.getBytes());
	}
*/
    private void notifyErrorConnection() {
        SwingUtilities.invokeLater(() -> {
            List<String> cleanedErrors = errorList.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            ((GUISwing) gui).getConnectionPanel().updateErrors(cleanedErrors, connection);
            LOG.error(MessageUtils.t("remote_http_worker_error") + cleanedErrors.toString());
            errorList.clear();
        });
    }

}
