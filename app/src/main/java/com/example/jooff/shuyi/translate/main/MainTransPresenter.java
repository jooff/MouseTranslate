package com.example.jooff.shuyi.translate.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;

import com.example.jooff.shuyi.common.MyApp;
import com.example.jooff.shuyi.constant.AppPref;
import com.example.jooff.shuyi.constant.TransSource;
import com.example.jooff.shuyi.data.AppDbRepository;
import com.example.jooff.shuyi.data.AppDbSource;
import com.example.jooff.shuyi.data.entity.History;
import com.example.jooff.shuyi.data.entity.Translate;

import java.io.IOException;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Created by Jooff on 2017/1/18.
 * Tomorrow is a nice day
 */

public class MainTransPresenter implements MainTransContract.Presenter {

    private String mUsSpeech;

    private String mUkSpeech;

    private TransHolder transHolder;

    private AppDbRepository mAppDbRepository;

    private MainTransContract.View mView;

    public MainTransPresenter(Bundle bundle, AppDbRepository transSource,
                              MainTransContract.View view) {
        transHolder = new TransHolder(bundle.getString(AppPref.ARG_TRANS_URL),
                bundle.getInt(AppPref.ARG_FROM), bundle.getString(AppPref.ARG_ORIGINAL));
        mAppDbRepository = transSource;
        mView = view;
    }

    @Override
    public void loadData() {
        final int transFrom = transHolder.getTransFrom();
        mAppDbRepository.getTrans(transFrom, transHolder.getTransUrl(), new AppDbSource.TranslateCallback() {

            @Override
            public void onResponse(Translate response) {
                if (response == null) {
                    mView.showError();
                    return;
                }
                mView.showCompletedTrans(transFrom, response.getQuery());
                if (transFrom != TransSource.FROM_COLLECT && transFrom != TransSource.FROM_HISTORY) {
                    History history = new History(transHolder.getOriginal(), response.getTranslation(), 0);
                    mAppDbRepository.saveHistory(history);
                }
                if (response.getTranslation() != null) {
                    mView.showResult(response.getTranslation());
                }
                if (response.getExplains() != null) {
                    mView.showExplain(response.getExplains());
                }
                if (response.getExplainsEn() != null) {
                    mView.showExplainEn(response.getExplainsEn());
                }
                if (response.getUkPhonetic() != null) {
                    mView.showPhonetic(response.getUsPhonetic(), response.getUkPhonetic());
                    mUsSpeech = response.getUsSpeech();
                    mUkSpeech = response.getUkSpeech();
                }
                if (response.getOriginal() != null) {
                    StringBuilder mWeb = new StringBuilder();
                    mWeb.append("\n");
                    for (int i = 0; i < response.getOriginal().size(); i++) {
                        mWeb.append(response.getOriginal().get(i))
                                .append("\n")
                                .append(response.getTranslate().get(i))
                                .append("\n")
                                .append("\n");
                    }
                    mView.showWeb(mWeb.toString());
                }
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode) {
                    case 0:
                        break;
                    case 1:
                        mView.showError();
                        break;
                    case 2:
                        mView.showNotSupport();
                        break;
                }
            }
        });
    }

    @Override
    public void playSpeech(int speechFrom) {
        final int from = speechFrom;
        new Thread(() -> {
            MediaPlayer mPlay = new MediaPlayer();
            try {
                if (from == 0) {
                    mPlay.setDataSource(mUsSpeech);
                } else {
                    mPlay.setDataSource(mUkSpeech);
                }
                mPlay.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
            mPlay.start();
        }).start();
    }

    @Override
    public void initTheme() {
        if (!MyApp.sIsNightMode) {
            mView.setAppTheme(MyApp.sColorPrimary);
        }
    }

    @Override
    public void setTextToClip(Context context, String trans) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData result = ClipData.newPlainText("result", trans);
        clipboardManager.setPrimaryClip(result);
        mView.showCopySuccess();
    }

    static class TransHolder {

        private String transUrl;

        private int transFrom;

        private String original;

        public TransHolder(String transUrl, int transFrom, String original) {
            this.transUrl = transUrl;
            this.transFrom = transFrom;
            this.original = original;
        }

        public String getTransUrl() {
            return transUrl;
        }

        public int getTransFrom() {
            return transFrom;
        }

        public String getOriginal() {
            return original;
        }
    }

}
