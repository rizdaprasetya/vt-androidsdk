package id.co.veritrans.sdk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;

import id.co.veritrans.sdk.R;
import id.co.veritrans.sdk.callbacks.TokenCallBack;
import id.co.veritrans.sdk.callbacks.TransactionCallback;
import id.co.veritrans.sdk.core.Constants;
import id.co.veritrans.sdk.core.Logger;
import id.co.veritrans.sdk.core.SdkUtil;
import id.co.veritrans.sdk.core.StorageDataHandler;
import id.co.veritrans.sdk.core.VeritransSDK;
import id.co.veritrans.sdk.fragments.PaymentTransactionStatusFragment;
import id.co.veritrans.sdk.fragments.SavedCardFragment;
import id.co.veritrans.sdk.models.BillingAddress;
import id.co.veritrans.sdk.models.CardPaymentDetails;
import id.co.veritrans.sdk.models.CardTokenRequest;
import id.co.veritrans.sdk.models.CardTransfer;
import id.co.veritrans.sdk.models.CustomerDetails;
import id.co.veritrans.sdk.models.ShippingAddress;
import id.co.veritrans.sdk.models.TokenDetailsResponse;
import id.co.veritrans.sdk.models.TransactionDetails;
import id.co.veritrans.sdk.models.TransactionResponse;
import id.co.veritrans.sdk.models.UserAddress;
import id.co.veritrans.sdk.models.UserDetail;

public class CreditDebitCardFlowActivity extends AppCompatActivity implements TokenCallBack,
        TransactionCallback {
    private static final int PAYMENT_WEB_INTENT = 100;
    private static final int GET_TOKEN = 50;
    private static final int PAY_USING_CARD = 51;
    private Toolbar toolbar;
    private String currentFragmentName;
    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private VeritransSDK veritransSDK;
    private float cardWidth;
    private UserDetail userDetail;
    private int currentApiCallNumber;
    private TokenDetailsResponse tokenDetailsResponse;
    private CardTokenRequest cardTokenRequest;
    private CardTransfer cardTransfer;
    private StorageDataHandler storageDataHandler;
    private ArrayList<CardTokenRequest> creditCards = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storageDataHandler = new StorageDataHandler();
        getCreditCards();
        try {
            userDetail = (UserDetail) storageDataHandler.readObject(this, Constants.USER_DETAILS);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_credit_debit_card_flow);
        veritransSDK = VeritransSDK.getVeritransSDK();
        fragmentManager = getSupportFragmentManager();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        calculateScreenWidth();
        SavedCardFragment savedCardFragment = SavedCardFragment.newInstance();
        replaceFragment(savedCardFragment, true, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 1) {
            finish();
        } else {
            if (currentFragmentName.equalsIgnoreCase(PaymentTransactionStatusFragment.class
                    .getName())) {
                finish();
            } else {
                super.onBackPressed();
            }
            /*if (currentFragmentName.equalsIgnoreCase(WebviewFragment.class.getName())) {
                if (((WebviewFragment) currentFragment).webView.canGoBack()) {
                    ((WebviewFragment) currentFragment).webviewBackPressed();
                } else {
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }*/
        }
    }

    public void getToken(CardTokenRequest cardTokenRequest) {
        SdkUtil.showProgressDialog(this, false);
        this.cardTokenRequest = cardTokenRequest;
        //this.cardTokenRequest.setSavedTokenId("");
        veritransSDK.getToken(CreditDebitCardFlowActivity.this, this.cardTokenRequest, this);
    }

    public void getToken() {
        if (cardTokenRequest != null) {
            this.cardTokenRequest.setGrossAmount(veritransSDK.getTransactionRequest().getAmount());
            SdkUtil.showProgressDialog(this, false);
            veritransSDK.getToken(CreditDebitCardFlowActivity.this, this.cardTokenRequest, this);
        } else {
            SdkUtil.showSnackbar(this, getString(R.string.card_details_error_message));
        }
    }

    public void payUsingCard() {
        SdkUtil.showProgressDialog(this, false);
        CustomerDetails customerDetails = new CustomerDetails(userDetail.getUserFullName(), "",
                userDetail.getEmail(), userDetail.getPhoneNumber());

        ArrayList<UserAddress> userAddresses = userDetail.getUserAddresses();
        TransactionDetails transactionDetails = new TransactionDetails("" + veritransSDK.
                getTransactionRequest().getAmount(),
                veritransSDK.getTransactionRequest().getOrderId());
        if (userAddresses != null && !userAddresses.isEmpty()) {
            ArrayList<BillingAddress> billingAddresses = new ArrayList<>();
            ArrayList<ShippingAddress> shippingAddresses = new ArrayList<>();

            for (int i = 0; i < userAddresses.size(); i++) {
                UserAddress userAddress = userAddresses.get(i);
                if (userAddress.getAddressType() == Constants.ADDRESS_TYPE_BILLING ||
                        userAddress.getAddressType() == Constants.ADDRESS_TYPE_BOTH) {
                    BillingAddress billingAddress;
                    billingAddress = new BillingAddress();
                    billingAddress.setCity(userAddress.getCity());
                    billingAddress.setFirstName(userDetail.getUserFullName());
                    billingAddress.setLastName("");
                    billingAddress.setPhone(userDetail.getPhoneNumber());
                    billingAddress.setCountryCode(userAddress.getCountry());
                    billingAddress.setPostalCode(userAddress.getZipcode());
                    billingAddresses.add(billingAddress);
                }
                if (userAddress.getAddressType() == Constants.ADDRESS_TYPE_SHIPPING ||
                        userAddress.getAddressType() == Constants.ADDRESS_TYPE_BOTH) {
                    ShippingAddress shippingAddress;
                    shippingAddress = new ShippingAddress();
                    shippingAddress.setCity(userAddress.getCity());
                    shippingAddress.setFirstName(userDetail.getUserFullName());
                    shippingAddress.setLastName("");
                    shippingAddress.setPhone(userDetail.getPhoneNumber());
                    shippingAddress.setCountryCode(userAddress.getCountry());
                    shippingAddress.setPostalCode(userAddress.getZipcode());
                    shippingAddresses.add(shippingAddress);
                }

            }

            CardPaymentDetails cardPaymentDetails = null;
            try {
                Logger.i("savetokenid:" + cardTokenRequest.getSavedTokenId());
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            //for one click
            if (veritransSDK.getTransactionRequest().getCardClickType().equalsIgnoreCase
                    (Constants.CARD_CLICK_TYPE_ONE_CLICK) &&
                    !TextUtils.isEmpty(cardTokenRequest.getSavedTokenId())) {
                cardPaymentDetails = new CardPaymentDetails(Constants.BANK_NAME,
                        cardTokenRequest.getSavedTokenId(), cardTokenRequest.isSaved());
            } else if (tokenDetailsResponse != null) {
                cardPaymentDetails = new CardPaymentDetails(Constants.BANK_NAME,
                        tokenDetailsResponse.getTokenId(), cardTokenRequest.isSaved());
            } else {
                return;
            }
            try {
                cardTransfer = new CardTransfer(cardPaymentDetails, transactionDetails,
                        null, billingAddresses, shippingAddresses, customerDetails);
                currentApiCallNumber = PAY_USING_CARD;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        veritransSDK.paymentUsingCard(this, this.cardTransfer, this);
    }

    public VeritransSDK getVeritransSDK() {
        return veritransSDK;
    }

    public UserDetail getUserDetail() {
        return userDetail;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    private void calculateScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;
        cardWidth = outMetrics.widthPixels;
        cardWidth = cardWidth - ((2 * getResources().getDimension(R.dimen.sixteen_dp)) / density);
    }

    public float getScreenWidth() {
        return cardWidth;
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack, boolean clearBackStack) {
        if (fragment != null) {
            Logger.i("replace freagment");
            boolean fragmentPopped = false;
            String backStateName = fragment.getClass().getName();

            if (clearBackStack) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                fragmentPopped = fragmentManager.popBackStackImmediate(backStateName, 0);
            }

            if (!fragmentPopped) { //fragment not in back stack, create it.
                Logger.i("fragment not in back stack, create it");
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.card_container, fragment, backStateName);
                if (addToBackStack) {
                    ft.addToBackStack(backStateName);
                }
                ft.commit();
                currentFragmentName = backStateName;
                currentFragment = fragment;
            }
        }
    }

    //onSuccess for get token
    @Override
    public void onSuccess(TokenDetailsResponse tokenDetailsResponse) {
        SdkUtil.hideProgressDialog();
        this.tokenDetailsResponse = tokenDetailsResponse;
        Logger.i("token suc:" + tokenDetailsResponse.getTokenId() + ","
                + veritransSDK.getTransactionRequest().isSecureCard());
        if (veritransSDK.getTransactionRequest().isSecureCard()) {
            if (!TextUtils.isEmpty(tokenDetailsResponse.getRedirectUrl())) {
                Intent intentPaymentWeb = new Intent(this, PaymentWebActivity.class);
                intentPaymentWeb.putExtra(Constants.WEBURL, tokenDetailsResponse.getRedirectUrl());
                startActivityForResult(intentPaymentWeb, PAYMENT_WEB_INTENT);
            }
        } else {
            payUsingCard();
        }

    }

    @Override
    public void onFailure(String errorMessage, TokenDetailsResponse tokenDetailsResponse) {
        SdkUtil.hideProgressDialog();
        SdkUtil.showApiFailedMessage(this, errorMessage);
    }

    //onfailure for both token and transaction api call
    @Override
    public void onFailure(String errorMessage, TransactionResponse transactionResponse) {
        PaymentTransactionStatusFragment paymentTransactionStatusFragment =
                PaymentTransactionStatusFragment.newInstance(transactionResponse);
        replaceFragment(paymentTransactionStatusFragment, true, false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(getString(R.string.title_payment_failed));

    }

    //onSuccess for transaction api call
    @Override
    public void onSuccess(TransactionResponse cardPaymentResponse) {
        SdkUtil.hideProgressDialog();
        Logger.i("cardPaymentResponse:" + cardPaymentResponse.getStatusCode());
        if (cardPaymentResponse.getStatusCode().equalsIgnoreCase(Constants.SUCCESS_CODE_200) ||
                cardPaymentResponse.getStatusCode().equalsIgnoreCase(Constants.SUCCESS_CODE_201)) {
            PaymentTransactionStatusFragment paymentTransactionStatusFragment =
                    PaymentTransactionStatusFragment.newInstance(cardPaymentResponse);
            replaceFragment(paymentTransactionStatusFragment, true, false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(getString(R.string.title_payment_successful));
            if (cardTokenRequest.isSaved()) {
                if (!creditCards.isEmpty()) {
                    int position = -1;
                    for (int i = 0; i < creditCards.size(); i++) {
                        CardTokenRequest card = creditCards.get(i);
                        if (card.getCardNumber().equalsIgnoreCase(cardTokenRequest.getCardNumber
                                ())) {
                            position = i;
                            break;
                        }
                    }
                    if (position >= 0) {
                        creditCards.remove(position);
                    }
                }
                cardTokenRequest.setCardCVV(0);
                cardTokenRequest.setClientKey("");
                cardTokenRequest.setGrossAmount(0);
                if (cardTokenRequest.isSaved() && !TextUtils.isEmpty(cardPaymentResponse
                        .getSavedTokenId())) {
                    cardTokenRequest.setSavedTokenId(cardPaymentResponse.getSavedTokenId());
                }
                Logger.i("Card:" + cardTokenRequest.getString());
                creditCards.add(cardTokenRequest);
                try {
                    storageDataHandler.writeObject(this, Constants.USERS_SAVED_CARD, creditCards);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("reqCode:" + requestCode + ",res:" + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == PAYMENT_WEB_INTENT) {
                payUsingCard();
            }
        }
    }

    public ArrayList<CardTokenRequest> getCreditCards() {
        try {
            ArrayList<CardTokenRequest> cards = (ArrayList<CardTokenRequest>) storageDataHandler
                    .readObject(this, Constants.USERS_SAVED_CARD);
            if (cards != null) {
                creditCards.clear();
                creditCards.addAll(cards);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return creditCards;
    }

    public void oneClickPayment(CardTokenRequest cardDetail) {
        this.cardTokenRequest = cardDetail;
        payUsingCard();
    }

    public void twoClickPayment(CardTokenRequest cardDetail) {
        SdkUtil.showProgressDialog(this, false);
        this.cardTokenRequest = cardDetail;
        this.cardTokenRequest.setTwoClick(true);
        this.cardTokenRequest.setClientKey(veritransSDK.getClientKey());
        veritransSDK.getToken(CreditDebitCardFlowActivity.this, this.cardTokenRequest, this);
        //getToken(cardDetail);
    }
}