package com.example.xdyblaster.util;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.example.xdyblaster.R;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.realm.Realm;


public class InfoDialog extends DialogFragment {


    @BindView(R.id.iv_logo)
    ImageView ivLogo;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tv_message)
    TextView tvMessage;
    @BindView(R.id.bt_cancel)
    Button btCancel;
    @BindView(R.id.bt_confirm)
    Button btConfirm;
    @BindView(R.id.lt_button)
    LinearLayout ltButton;
    private Unbinder unbinder;
    private OnButtonClickListener onButtonClickListener;
    private String title = "", message = "", edit1 = "";
    private boolean messageEnable = false;
    private boolean edit1Enable = false;
    private boolean btnEnable = false;
    private boolean btn2Enable = false;
    private boolean progressEnable = false;
    private boolean timerEnable = false;
    private boolean autoExit = true;
    private boolean chronometerCountDown = false;
    @BindView(R.id.tv_edit1)
    TextView tvEdit1;
    @BindView(R.id.et_edit1)
    public EditText etEdit1;
    @BindView(R.id.lt_edit1)
    LinearLayout ltEdit1;
    @BindView(R.id.lt_progress)
    public LinearLayout ltProgress;
    @BindView(R.id.progressBar)
    public RoundCornerProgressBar progressBar;
    @BindView(R.id.bt_exit)
    Button btExit;
    @BindView(R.id.lt_button2)
    RelativeLayout ltButton2;
    @BindView(R.id.chronometer)
    public Chronometer chronometer;
    private String etEdit1Str = "";
    private int logoColor = 1;
    private int progressBarMax = 300, current;
    Realm mRealm = Realm.getDefaultInstance();

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_message, container);
        unbinder = ButterKnife.bind(this, view);
        tvMessage.setText(message);
        tvTitle.setText(title);
        tvEdit1.setText(edit1);
//        ivLogo.setImageTintList(ColorStateList.valueOf(0xff0000ff));
//        ivLogo.setImageTintMode(PorterDuff.Mode.DST_OUT);
        if (logoColor == 0)
            ivLogo.setColorFilter(ContextCompat.getColor(Objects.requireNonNull(getActivity()).getApplicationContext(), R.color.blue));
        if (logoColor == 1)
            ivLogo.setColorFilter(ContextCompat.getColor(Objects.requireNonNull(getActivity()).getApplicationContext(), R.color.red));
        if (logoColor == 2)
            ivLogo.setColorFilter(ContextCompat.getColor(Objects.requireNonNull(getActivity()).getApplicationContext(), R.color.green));

        if (messageEnable)
            tvMessage.setVisibility(View.VISIBLE);
        else
            tvMessage.setVisibility(View.GONE);

        if (btnEnable)
            ltButton.setVisibility(View.VISIBLE);
        else
            ltButton.setVisibility(View.GONE);
        if (btn2Enable)
            ltButton2.setVisibility(View.VISIBLE);
        else
            ltButton2.setVisibility(View.GONE);

        if (edit1Enable)
            ltEdit1.setVisibility(View.VISIBLE);
        else
            ltEdit1.setVisibility(View.GONE);
        etEdit1.setText(etEdit1Str);

        if (progressEnable)
            ltProgress.setVisibility(View.VISIBLE);
        else
            ltProgress.setVisibility(View.GONE);
        if (timerEnable) {
            chronometer.setVisibility(View.VISIBLE);
            if (chronometerCountDown) {
                current = 0;
                chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                    @Override
                    public void onChronometerTick(Chronometer chronometer) {
                        current++;
                        if (current > progressBarMax)
                            dismissAllowingStateLoss();
                        progressBar.setProgress(current);

                    }
                });
            }
            chronometer.start();
        } else {
            chronometer.setVisibility(View.GONE);
        }
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(0.0f);
                progressBar.setSecondaryProgress(0.0f);
                progressBar.setMax(progressBarMax);
            }
        });

//        if (!mRealm.isInTransaction()) {
//            mRealm.executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    DailyData dailyData = realm.createObject(DailyData.class, FileFunc.getDate());
//                    dailyData.setAct(title);
//                    dailyData.setMemo(message);
//                }
//            });
//        }

//        try {
//            mRealm.beginTransaction();//????????????
//            DailyData dailyData = mRealm.createObject(DailyData.class, FileFunc.getDate());
//            dailyData.setAct(title);
//            dailyData.setMemo(message);
//            mRealm.commitTransaction();//????????????}
//        }catch (Exception e)
//        {
//            e.printStackTrace();
//        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window win = getDialog().getWindow();
        // ???????????????Background?????????????????????window??????????????????
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //?????????????????????
        WindowManager.LayoutParams layoutParams = win.getAttributes();
        layoutParams.dimAmount = 0.4f;
        win.setAttributes(layoutParams);


        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

        WindowManager.LayoutParams params = win.getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = dm.widthPixels * 6 / 7;
//        // ??????ViewGroup.LayoutParams?????????Dialog ????????????????????????
//        int h = dm.widthPixels / 5 * 2;
//        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//
//        params.height = h;// dm.widthPixels / 3;//ViewGroup.LayoutParams.WRAP_CONTENT;
//      params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        //seekbar.getThumb().setColorFilter(Color.parseColor("#ec6a88"), PorterDuff.Mode.SRC_ATOP);

        params.alpha = 1.0f;//0.7f;f
        win.setAttributes(params);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.bt_cancel, R.id.bt_confirm, R.id.bt_exit})
    public void onViewClicked(View view) {
        if (onButtonClickListener != null) {
            switch (view.getId()) {
                case R.id.bt_cancel:
                    onButtonClickListener.onButtonClick(0, etEdit1.getText().toString());
                    dismissAllowingStateLoss();
                    break;
                case R.id.bt_confirm:
                    onButtonClickListener.onButtonClick(1, etEdit1.getText().toString());
                    if (autoExit)
                        dismiss();
                    break;
                case R.id.bt_exit:
                    onButtonClickListener.onButtonClick(2, etEdit1.getText().toString());
                    dismiss();
                    break;

            }
        } else
            dismissAllowingStateLoss();
    }


    public interface OnButtonClickListener {
        void onButtonClick(int index, String str);
    }

    public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener;
    }

    public void setTitle(String str) {
        title = str;
    }

    public void setMessage(String str) {
        message = str;
        messageEnable = true;

    }

    public void setMessageTxt(String str) {
        message = str;
        tvMessage.setText(str);
    }

    public void setBtnEnable(Boolean b) {
        btnEnable = b;
    }

    public void setBtn2Enable(Boolean b) {
        btn2Enable = b;
    }

    public void setProgressEnable(boolean b) {
        progressEnable = b;
    }

    public void setEdit1(String str) {
        edit1Enable = true;
        edit1 = str;
    }

    public void setEdit1Text(String str) {
        edit1Enable = true;
        etEdit1Str = str;
    }

    public void setChronometerEnable(boolean b) {
        timerEnable = b;
    }

    public void setAutoExit(boolean b) {
        autoExit = b;
    }

    public void setLogoColor(int color) {
        logoColor = color;
    }

    public void setProgressBarMax(int max) {
        progressBarMax = max;
    }

    public void setChronometerCountDown(boolean coutDown) {
        chronometerCountDown = coutDown;
    }
}