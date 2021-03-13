package com.hlag.colorcompare;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/*
    Path in order for checking purchase
    path in order for buying - uses functions before
 */

class BillingHelper implements PurchasesUpdatedListener, BillingClientStateListener {

    interface BillingListener {
        void attention_toast(String message);

        Activity getActivity();

        void setupAds();
    }

    private static BillingHelper ourInstance;

    private static final String TAG = "BillingHelper";
    private final String prvNyckel = "ÉÍÍÆÍîÅÊÆãïõìïíÃ½ó´ÆÅÕÁÂÅÅËÇÅÕ¼ÅÉÍÍÆÇãÏÇÅÕÁÅð³¯íÓÃ¯ÐËçé½ÐêïàÆ·ÇòìöÔÍÑÈåîå´µþã²÷þÜÊÂÕµììÃ²µÇÞüêÔÂô¯ÅüÉüïÒ¶¶Ý¶à½áÇÂèÅÎÜâ·ÒõðâËÅ±ÀýÂÓÅüÖü¼×ñíêÎÔÒËçÁðÓÝãñÔÂ±òÎî×áòíÜ½ÊÍ³àÒÖÇ«ÆÏÉÜÁÅâòÈôô´éÜÏþÎòË¯´Î÷µÐìÃëÆÐå±òÆ¼«ÅÅÊ×ÐÝÎ±üÊå·ÐéÜÍåãÜþÊôÔýýÇ¶·àÉÎ¼ëÜ·ÐÜÀ÷ÀÐîÃÞçïýÁ÷ç×ý½âÐÜþèãíïþ´î³Ñâã÷çÒò½·ÍÖÔýõËÖÆýðÁÅâòÁýëýò¼ôèÊÓÍÜüÌîÞÁêÏæóÂóÞÉàþþË²ÖõÔÂíÔçê¶ðà¶þÊÈÃðÉÝÂ°ÊÞõ³ÝâÌôþÏ·ÎÕÊð«¶ÓÇ×ó³²¶ëöþôÍðèãÞôÜ×ñîÍìÕÍÀÅÕÅÆ";
    private BillingListener listener;
    private BillingClient billingClient;
    private List<String> skuList = new ArrayList<>();

    static BillingHelper getInstance(BillingListener billingListener) {
        if (ourInstance == null) {
            ourInstance = new BillingHelper(billingListener);
        }
        return ourInstance;
    }

    private BillingHelper(BillingListener billingListener) {
        listener = billingListener;
        billingClient = BillingClient.newBuilder(listener.getActivity()).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(this);
    }


    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            queryPurchases();
        } else {
            listener.setupAds();
        }
    }


    private void queryPurchases() {
        final Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchasesResult.getPurchasesList().size() != 0) {
                for (Purchase purchase : purchasesResult.getPurchasesList()) {
                    handlePurchase(purchase);
                }
            }
        } else {
            toast(listener.getActivity().getString(R.string.failed_query));
        }
        if (skuList.size() > 0) {
            skuQuery();
        }

        listener.setupAds();
    }


    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!validSignature(purchase)) {
                toast(listener.getActivity().getString(R.string.invalid_signature));
                return;
            }
            if (purchase.getSku().equals("com.hlag.colorcompare.premium")) {
                ((MainActivity) listener.getActivity()).disableAds();
            }

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {

                });
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            toast(listener.getActivity().getString(R.string.purchase_pending));
        }
    }


    private boolean validSignature(Purchase purchase) {
        final String signature = purchase.getSignature();
        try {
            final PublicKey pkey = getAPKKey("RSA");
            final Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(pkey);
            sig.update(purchase.getOriginalJson().getBytes());

            return sig.verify(Base64.decode(signature, Base64.DEFAULT));
        } catch (Exception ignored) {
            toast(listener.getActivity().getString(R.string.failed_verification));
        }

        return false;
    }

    private PublicKey getAPKKey(String keyFactoryAlgorithm) throws Exception {
        final byte[] decodedKey = Base64.decode(stringTransform(prvNyckel, 0x84), Base64.DEFAULT);
        final KeyFactory keyFactory = KeyFactory.getInstance(keyFactoryAlgorithm);
        return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
    }

    private String stringTransform(String s, int i) {
        final char[] chars = s.toCharArray();
        for (int j = 0; j < chars.length; j++)
            chars[j] = (char) (chars[j] ^ i);
        return String.valueOf(chars);
    }


    void buy(ArrayList<String> skuList) {
        this.skuList = skuList;
        if (billingClient.isReady()) {
            skuQuery();
        } else {
            listener.attention_toast(listener.getActivity().getString(R.string.please_wait));
            billingClient.startConnection(this);
        }
    }

    private void skuQuery() {
        final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    skuList.clear();
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        startPurchase(skuDetailsList.get(0));
                    }
                });
    }

    private void startPurchase(SkuDetails skuDetails) {
        final BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        billingClient.launchBillingFlow(listener.getActivity(), flowParams);
    }


    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        final int resCode = billingResult.getResponseCode();

        if (purchases != null) {
            if (resCode == BillingClient.BillingResponseCode.OK) {
                toast(listener.getActivity().getString(R.string.thanks));

            } else if (resCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                toast(listener.getActivity().getString(R.string.had_premium));
            }

            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (resCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            toast(listener.getActivity().getString(R.string.purchase_canceled));
        } else {
            toast(listener.getActivity().getString(R.string.purchase_failed));
        }
    }


    @Override
    public void onBillingServiceDisconnected() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (listener != null && !billingClient.isReady()) {
                    billingClient.startConnection(ourInstance);
                }
            }
        }, 30000);
        listener.setupAds();
    }


    private void toast(String message) {
        listener.attention_toast(message);
    }

    void close() {
        billingClient.endConnection();
        billingClient = null;
        listener = null;
        ourInstance = null;
    }
}
