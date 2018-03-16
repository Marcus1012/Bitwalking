package bitwalking.bitwalking.server.api;

import android.util.Pair;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.server.BitwalkingServer;
import bitwalking.bitwalking.server.OnServerResponse;
import bitwalking.bitwalking.server.requests.BusinessInviteRequest;
import bitwalking.bitwalking.server.requests.ConfirmPasswordResetRequest;
import bitwalking.bitwalking.server.requests.DataRequest;
import bitwalking.bitwalking.server.requests.EmailAvailableRequest;
import bitwalking.bitwalking.server.requests.JoinEventRequest;
import bitwalking.bitwalking.server.requests.LoginRequest;
import bitwalking.bitwalking.server.requests.PaymentSendRequest;
import bitwalking.bitwalking.server.requests.ResetPasswordRequest;
import bitwalking.bitwalking.server.requests.UpdateDeviceLogsRequest;
import bitwalking.bitwalking.server.requests.UpdateMeRequest;
import bitwalking.bitwalking.server.requests.UpdateUserAvatar;
import bitwalking.bitwalking.server.requests.UpdateUserEmail;
import bitwalking.bitwalking.server.requests.UpdateUserPassword;
import bitwalking.bitwalking.server.requests.UpdateUserPhone;
import bitwalking.bitwalking.server.requests.UpdateUserSteps;
import bitwalking.bitwalking.server.requests.UpdateUserVote;
import bitwalking.bitwalking.server.requests.UserRegisterRequest;
import bitwalking.bitwalking.server.requests.ValidateEmailRequest;
import bitwalking.bitwalking.server.requests.VerifyPasswordRequest;
import bitwalking.bitwalking.server.requests.VerifyPhoneRequest;
import bitwalking.bitwalking.server.responses.BasicServerResponse;
import bitwalking.bitwalking.server.responses.EmailAvailableResponse;
import bitwalking.bitwalking.server.responses.EventInfoResponse;
import bitwalking.bitwalking.server.responses.EventsListResponse;
import bitwalking.bitwalking.server.responses.GetCurrentEventResponse;
import bitwalking.bitwalking.server.responses.GetMeResponse;
import bitwalking.bitwalking.server.responses.GetSurveyResponse;
import bitwalking.bitwalking.server.responses.GetUserAvatarResponse;
import bitwalking.bitwalking.server.responses.GetUserBalanceResponse;
import bitwalking.bitwalking.server.responses.LoginResponse;
import bitwalking.bitwalking.server.responses.RegistrationResponse;
import bitwalking.bitwalking.server.responses.UpdatePhoneResponse;
import bitwalking.bitwalking.server.responses.UpdateUserAvatarResponse;
import bitwalking.bitwalking.server.responses.UserTodayResponse;
import bitwalking.bitwalking.server.responses.UserVoteResponse;
import bitwalking.bitwalking.server.responses.VerifyPhoneResponse;
import bitwalking.bitwalking.user_info.BalanceInfo;
import bitwalking.bitwalking.user_info.CurrentEventInfo;
import bitwalking.bitwalking.user_info.MeInfo;
import bitwalking.bitwalking.user_info.UserInfo;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;

/**
 * Created by Marcus on 10/27/16.
 */

public class ServerApi {
    private static final String TAG = ServerApi.class.getSimpleName();

    //region Today

    public static void getToday(String id, String secret, final MiningListener listener) {
        Date startDate = Globals.getZeroTimeDate(new Date());
        getMining(startDate, null, id, secret, listener);
    }

    public static void getMining(final Date startDate, final Date endDate, String id, String secret, final MiningListener listener) {
        if (startDate.before(Globals.getMigrationDate())) { // ignore, too early
            if (null != listener)
                listener.onMining(startDate, null);
        }
        else {
            List<Pair<String, String>> params = new ArrayList();
            params.add(new Pair<>("start", Globals.INSTANCE.getFullDateFormat().format(startDate)));
            if (null != endDate)
                params.add(new Pair<>("end", Globals.INSTANCE.getFullDateFormat().format(endDate)));
            String paramsString = BitwalkingServer.INSTANCE.encodeUrlParameters(params);
            BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                    new OnServerResponse() {
                        @Override
                        public void onBasicResponse(BasicServerResponse response, int id) {
                            UserTodayResponse.MiningInfo miningInfo = null;
                            switch (response.getResponseCode()) {
                                case 200:
                                    UserTodayResponse res = new UserTodayResponse(response);
                                    miningInfo = res.getMiningInfo();
                                    break;
                                case 400: // illegal parameter
                                default:
                                    break;
                            }

                            if (null != listener)
                                listener.onMining(startDate, miningInfo);
                        }
                    },
                    BitwalkingServer.ME_PATH + "/balance/mining?" + paramsString,
                    BitwalkingServer.HttpMethod.GET,
                    BitwalkingServer.ContentType.JSON,
                    id,
                    secret,
                    null
            );

            BitwalkingServer.INSTANCE.addRequest(req);
        }
    }

    public interface MiningListener {
        void onMining(Date startDate, UserTodayResponse.MiningInfo miningInfo);
    }

    //endregion

    //region Balance

    public static void getBalance(String id, String secret, final OnBalanceListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        BalanceInfo balanceInfo = null;
                        switch (response.getResponseCode()) {
                            case 200:
                                GetUserBalanceResponse res = new GetUserBalanceResponse(response);
                                balanceInfo = res.getBalance();
                                break;
                            case 400: // illegal parameter
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onBalance(balanceInfo);
                    }
                },
                BitwalkingServer.ME_PATH + "/balance",
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface OnBalanceListener {
        void onBalance(BalanceInfo balanceInfo);
    }

    //endregion

    //region Me

    public static void getMe(String id, String secret, final OnMeListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        MeInfo meInfo = null;
                        switch (response.getResponseCode()) {
                            case 200:
                                GetMeResponse res = new GetMeResponse(response);
                                meInfo = res.getMe();
                                break;
                            case 400: // illegal parameter
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onMe(meInfo);
                    }
                },
                BitwalkingServer.ME_PATH,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void putMe(String id, String secret, UpdateMeRequest updateMeRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH,
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                updateMeRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface OnMeListener {
        void onMe(MeInfo meInfo);
    }

    //endregion

    //region Avatar

    public static void getAvatar(String id, String secret, final AvatarListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        if (null != listener) {
                            GetUserAvatarResponse.AvatarInfo avatarInfo = null;

                            switch (response.getResponseCode()) {
                                case 200:
                                    avatarInfo = new UpdateUserAvatarResponse(response).getAvatarInfo();
                                    break;
                                default:
                                    break;
                            }

                            listener.onAvatar(avatarInfo, response.getResponseCode());
                        }
                    }
                },
                BitwalkingServer.ME_PATH + "/avatar",
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void putAvatar(String id, String secret, UpdateUserAvatar updateUserAvatar, final AvatarListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        if (null != listener) {
                            GetUserAvatarResponse.AvatarInfo avatarInfo = null;

                            switch (response.getResponseCode()) {
                                case 200:
                                case 201:
                                    avatarInfo = new UpdateUserAvatarResponse(response).getAvatarInfo();
                                    break;
                                default:
                                    break;
                            }

                            listener.onAvatar(avatarInfo, response.getResponseCode());
                        }
                    }
                },
                BitwalkingServer.ME_PATH + "/avatar",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.DATA + String.format("boundary=%s", DataRequest.BOUNDARY),
                id,
                secret,
                updateUserAvatar
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void deleteAvatar(String id, String secret, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH + "/avatar",
                BitwalkingServer.HttpMethod.DELETE,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface AvatarListener {
        void onAvatar(GetUserAvatarResponse.AvatarInfo avatarInfo, final int code);
    }

    //endregion

    //region Phone

    public static void changePhone(String id, String secret, UpdateUserPhone updatePhonePayload, final PhoneChangeListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        UpdatePhoneResponse.PhoneUpdateInfo phoneUpdateInfo = null;
                        switch (response.getResponseCode()) {
                            case 202:
                                phoneUpdateInfo = new UpdatePhoneResponse(response).getCodeInfo();
                                break;
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onPhoneChange(phoneUpdateInfo, response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH + "/phone",
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                updatePhonePayload
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface PhoneChangeListener {
        void onPhoneChange(UpdatePhoneResponse.PhoneUpdateInfo phoneUpdateInfo, int code);
    }

    //endregion

    //region Session

    public static void checkSession(String id, String secret, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.AUTHENTICATION_PATH + "/session",
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Logs

    public static void putLogs(String id, String secret, UpdateDeviceLogsRequest updateDeviceLogsRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH + "/support/logs",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.DATA + String.format("boundary=%s", DataRequest.BOUNDARY),
                id,
                secret,
                updateDeviceLogsRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Event

    public static void currentEvent(String id, String secret, final CurrentEventListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            CurrentEventInfo eventInfo = null;
                            if (200 == response.getResponseCode())
                                eventInfo = new GetCurrentEventResponse(response).getCurrentEventInfo();
                            listener.onCurrentEvent(eventInfo, response.getResponseCode());
                        }
                    }
                },
                BitwalkingServer.ME_PATH + "/events/current",
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface CurrentEventListener {
        void onCurrentEvent(CurrentEventInfo eventInfo, int code);
    }

    //endregion

    //region Events

    public static void getEvents(String id, String secret, final EventsListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        ArrayList<EventsListResponse.EventInfo> eventsInfo = null;
                        switch (response.getResponseCode()) {
                            case 200:
                                eventsInfo = new EventsListResponse(response).getEvents();
                                break;
                            case 400: // illegal parameter
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onEvents(eventsInfo, response.getResponseCode());
                    }
                },
                BitwalkingServer.EVENTS_PATH,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void getEvent(String id, String secret, String eventId, final EventListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        EventsListResponse.EventInfo eventInfo = null;
                        switch (response.getResponseCode()) {
                            case 200:
                                eventInfo = new EventInfoResponse(response).getEventInfo();
                                break;
                            case 400: // illegal parameter
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onEvent(eventInfo);
                    }
                },
                BitwalkingServer.EVENTS_PATH + "/" + eventId,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static class EventJoinInfo {
        public String id;
        public JoinEventRequest.JoinEventLocation location;
    }

    public static void joinEvent(String id, String secret, EventJoinInfo eventInfo, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            int code = response.getResponseCode();
                            switch (response.getResponseCode()) {
                                case 409: {
                                    if (response.getHeader().code.equalsIgnoreCase("error.event.overlap"))
                                        code = 4091;
                                    else
                                        code = 4092;
                                    break;
                                }
                            }

                            listener.onResponse(code);
                        }
                    }
                },
                BitwalkingServer.EVENTS_ACTION_PATH + "/" + eventInfo.id + "/join",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                new JoinEventRequest(eventInfo.location)
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface EventsListener {
        void onEvents(ArrayList<EventsListResponse.EventInfo> eventsInfo, int code);
    }

    public interface EventListener {
        void onEvent(EventsListResponse.EventInfo eventInfo);
    }

    //endregion

    //region Steps

    public static int sendStepsNow(String id, String secret, UpdateUserSteps updateRequest) {
        int code = 400;

        try {
            BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                    BitwalkingServer.STEPS_PATH,
                    BitwalkingServer.HttpMethod.POST,
                    BitwalkingServer.ContentType.JSON,
                    id,
                    secret,
                    updateRequest
            );

            code = BitwalkingServer.INSTANCE.sendRequestNow(req).getResponseCode();
        } catch (Exception e) {
            BitwalkingApp.getInstance().trackException("sendStepsNow: error", e);
        }

        return code;
    }

    //endregion

    //region Push Token

    public static void pushToken(String id, String secret, String token, final SimpleServerResponseListener listener) {
        HashMap<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Push-Token", token);

        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                0,
                BitwalkingServer.AUTHENTICATION_PATH + "/session",
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                extraHeaders,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Validate Email

    public static void validateEmail(String id, String secret, ValidateEmailRequest validateRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.REGISTRATION_PATH + "/email/validate",
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                validateRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Change Email

    public static void changeEmail(String id, String secret, UpdateUserEmail updateEmailRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH + "/email",
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                updateEmailRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Join

    public static void checkEmailFree(String email, final EmailFreeListener listener) {
        EmailAvailableRequest checkPayload = new EmailAvailableRequest(email);

        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            EmailAvailableResponse.EmailAvailable emailFree = null;
                            if (200 == response.getResponseCode())
                                emailFree = new EmailAvailableResponse(response).getAnswer();
                            listener.onEmailFree(emailFree);
                        }
                    }
                },
                BitwalkingServer.REGISTRATION_PATH + "/email/availability",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                checkPayload
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface EmailFreeListener {
        void onEmailFree(EmailAvailableResponse.EmailAvailable emailFree);
    }

    public static void register(UserRegisterRequest registerRequest, final RegistrationListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        RegistrationResponse.RegistrationPayload registrationPayload = null;
                        switch (response.getResponseCode()) {
                            case 202:
                                RegistrationResponse regRes = new RegistrationResponse(response);
                                registrationPayload = regRes.getRegistrationPayload();
                                break;
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onRegistration(registrationPayload, response.getResponseCode());
                    }
                },
                BitwalkingServer.REGISTRATION_PATH,
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                registerRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void resumeRegister(String sessionId, UserRegisterRequest registerRequest, final RegistrationListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        RegistrationResponse.RegistrationPayload registrationPayload = null;
                        switch (response.getResponseCode()) {
                            case 202:
                                RegistrationResponse regRes = new RegistrationResponse(response);
                                registrationPayload = regRes.getRegistrationPayload();
                                break;
                            default:
                                break;
                        }

                        if (null != listener)
                            listener.onRegistration(registrationPayload, response.getResponseCode());
                    }
                },
                BitwalkingServer.REGISTRATION_PATH + "/" + sessionId,
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                registerRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface RegistrationListener {
        void onRegistration(RegistrationResponse.RegistrationPayload registrationPayload, final int code);
    }

    public static void getRegistrationSession(String sessionId, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.REGISTRATION_PATH + "/session/" + sessionId,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static class PhoneVerifyInfo {
        public String phone;
        public String code;
    }

    public static void verifyCode(PhoneVerifyInfo verifyInfo, final VerificationListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            VerifyPhoneResponse.SessionTokenInfo sessionInfo = null;
                            if (200 == response.getResponseCode())
                                sessionInfo = new VerifyPhoneResponse(response).getSessionInfo();
                            listener.onVerification(sessionInfo, response.getResponseCode());
                        }
                    }
                },
                BitwalkingServer.ACTION_PATH + "/code/" + verifyInfo.phone,
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                new VerifyPhoneRequest(verifyInfo.code, FirebaseInstanceId.getInstance().getToken())
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface VerificationListener {
        void onVerification(VerifyPhoneResponse.SessionTokenInfo sessionInfo, final int code);
    }

    //endregion

    //region Login

    public static void login(LoginRequest loginRequest, final LoginListener listener) {
        HashMap<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Push-Token", FirebaseInstanceId.getInstance().getToken());

        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200:
                                    listener.onLogin(new LoginResponse(response).getInfo());
                                    break;
                                case 412: // phone verification required
                                    listener.onVerificationRequired(new RegistrationResponse(response).getRegistrationPayload());
                                    break;
                                default:
                                    listener.onFailure(response.getResponseCode());
                                    break;
                            }
                        }
                    }
                },
                0,
                BitwalkingServer.AUTHENTICATION_PATH + "/login",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                extraHeaders,
                null,
                null,
                loginRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface LoginListener {
        void onLogin(UserInfo userInfo);
        void onVerificationRequired(RegistrationResponse.RegistrationPayload registrationPayload);
        void onFailure(final int code);
    }

    //endregion

    //region Password

    public static void forgotPassword(ResetPasswordRequest resetRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.AUTHENTICATION_PATH + "/password/request",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                resetRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void resetPassword(ConfirmPasswordResetRequest newPasswordRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.AUTHENTICATION_PATH + "/password/reset",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                null,
                null,
                newPasswordRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void changePassword(String id, String secret, UpdateUserPassword updateRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.ME_PATH + "/password",
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                updateRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void verifyPassword(String id, String secret, VerifyPasswordRequest verifyRequest, final SimpleServerResponseListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.AUTHENTICATION_PATH + "/verify",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                verifyRequest
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region Store

    public static void sendVote(String id, String secret, String voteId, String itemId, final SimpleServerResponseListener listener) {
        UpdateUserVote userVotePayload = new UpdateUserVote(itemId);
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.SURVEY_PATH + String.format("/%s/vote", voteId),
                BitwalkingServer.HttpMethod.PUT,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                userVotePayload
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void getVote(String id, String secret, String voteId, final VoteListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            UserVoteResponse.VoteInfo voteInfo = null;
                            if (200 == response.getResponseCode())
                                voteInfo = new UserVoteResponse(response).getVoteInfo();
                            listener.onVote(voteInfo);
                        }
                    }
                },
                BitwalkingServer.SURVEY_PATH + String.format("/%s/vote", voteId),
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface VoteListener {
        void onVote(UserVoteResponse.VoteInfo voteInfo);
    }

    public static void getSurvey(String id, String secret, final SurveyListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            GetSurveyResponse.SurveyInfo surveyInfo = null;
                            if (200 == response.getResponseCode())
                                surveyInfo = new GetSurveyResponse(response).getSurvey();
                            listener.onSurvey(surveyInfo, response.getResponseCode());
                        }
                    }
                },
                BitwalkingServer.SURVEY_PATH,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface SurveyListener {
        void onSurvey(GetSurveyResponse.SurveyInfo surveyInfo, final int code);
    }

    //endregion

    //region Businesses

    public static void suggestBusiness(String id, String secret, BusinessInviteRequest businessInfo, final SimpleServerResponseListener listener) {

        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener)
                            listener.onResponse(response.getResponseCode());
                    }
                },
                BitwalkingServer.INVITE_PATH + "/business",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                businessInfo
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    //endregion

    //region General

    public interface SimpleServerResponseListener {
        void onResponse(final int code);
    }

    //endregion

    //region Payments


    public static void sendPayment(String id, String secret, PaymentSendRequest requestBody, final PaymentActionListener listener){
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200: listener.onOk(); break;
                                case 404: listener.onNotFound(); break;
                                case 409: listener.onConflict(); break;
                                default: {
                                    Logger.instance().Log(Logger.INFO, TAG, "acceptPayment: unsupported response code = " + response.getResponseCode());
                                    listener.onNotFound();
                                }
                            }
                        }
                    }
                },
                BitwalkingServer.PAYMENTS_REQUESTS_ACTION_PATH +"/payments",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                requestBody
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void acceptPayment(String id, String secret, String paymentId, final PaymentActionListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200: listener.onOk(); break;
                                case 404: listener.onNotFound(); break;
                                case 409: listener.onConflict(); break;
                                default: {
                                    Logger.instance().Log(Logger.INFO, TAG, "acceptPayment: unsupported response code = " + response.getResponseCode());
                                    listener.onNotFound();
                                }
                            }
                        }
                    }
                },
                BitwalkingServer.PAYMENTS_REQUESTS_ACTION_PATH + "/" + paymentId + "/accept",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void rejectPayment(String id, String secret, String paymentId, final PaymentActionListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200: listener.onOk(); break;
                                case 404: listener.onNotFound(); break;
                                case 409: listener.onConflict(); break;
                                default: {
                                    Logger.instance().Log(Logger.INFO, TAG, "rejectPayment: unsupported response code = " + response.getResponseCode());
                                    listener.onNotFound();
                                }
                            }
                        }
                    }
                },
                BitwalkingServer.PAYMENTS_REQUESTS_ACTION_PATH + "/" + paymentId + "/reject",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public static void cancelPayment(String id, String secret, String paymentId, final PaymentActionListener listener) {
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200: listener.onOk(); break;
                                case 404: listener.onNotFound(); break;
                                case 409: listener.onConflict(); break;
                                default: {
                                    Logger.instance().Log(Logger.INFO, TAG, "cancelPayment: unsupported response code = " + response.getResponseCode());
                                    listener.onNotFound();
                                }
                            }
                        }
                    }
                },
                BitwalkingServer.PAYMENTS_REQUESTS_ACTION_PATH + "/" + paymentId + "/cancel",
                BitwalkingServer.HttpMethod.POST,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface PaymentActionListener {
        void onOk();
        void onNotFound();
        void onConflict();
    }

    //endregion


    //notification registration
    public static void getNotifications(String id, String secret,final NotificationsListener listener){
        BitwalkingServer.ServerHttpReqInfo req = new BitwalkingServer.ServerHttpReqInfo(
                new OnServerResponse() {
                    @Override
                    public void onBasicResponse(final BasicServerResponse response, int id) {
                        if (null != listener) {
                            switch (response.getResponseCode()) {
                                case 200: listener.onNotifications(); break;
                                default: {
                                    Logger.instance().Log(Logger.INFO, TAG, "Notification: response code = " + response.getResponseCode());
                                    listener.onError();
                                }
                            }
                        }
                    }
                },
                BitwalkingServer.NOTIFICATION_PATH,
                BitwalkingServer.HttpMethod.GET,
                BitwalkingServer.ContentType.JSON,
                id,
                secret,
                null
        );

        BitwalkingServer.INSTANCE.addRequest(req);
    }

    public interface NotificationsListener{
         void onError();
         void onNotifications();
    }

    //endregion notification
}
