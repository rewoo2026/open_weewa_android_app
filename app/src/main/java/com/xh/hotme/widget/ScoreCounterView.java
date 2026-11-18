package com.xh.hotme.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.utils.ClickGuard;


public class ScoreCounterView extends FrameLayout {

    ImageView addScoreView;
    ImageView removeScoreView;
    TextView teamNameView;
    TextView scoreNumberView;

    int scoreNumber;

    String teamName;

    ICallBack _callBack;


    public ScoreCounterView(Context context) {
        super(context, null);
    }

    public ScoreCounterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoreCounterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initUI();
    }

    private void initUI() {
        LayoutInflater.from(getContext()).inflate(R.layout.score_controll_view, this, true);
        addScoreView = findViewById(R.id.iv_add_score);
        removeScoreView = findViewById(R.id.iv_remove_score);
        teamNameView = findViewById(R.id.tv_team_name);
        scoreNumberView = findViewById(R.id.tv_score_number);

        scoreNumberView.setText(String.valueOf(scoreNumber));

        addScoreView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(_callBack != null) {
                    _callBack.onAddScore(scoreNumber);
                }
                return true;
            }
        });

        removeScoreView.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                if(scoreNumber <= 0) {
                    scoreNumber = 0;
                    return true;
                }

                if(_callBack != null) {
                    _callBack.onRemoveScore(scoreNumber);
                }

                return true;
            }
        });
    }

    public void setTeamName(String name) {
        teamName = name;
        teamNameView.setText(name);
    }


    public String getTeamName() {
        return teamName;
    }

    public int getScoreNumber() {
        return scoreNumber;
    }

    public void setScoreNumber(int score) {
        scoreNumber = score;
        scoreNumberView.setText(String.valueOf(scoreNumber));
    }

    public void addScore() {
        scoreNumber++;
        scoreNumberView.setText(String.valueOf(scoreNumber));
    }

    public void removeScore() {
        scoreNumber--;
        if(scoreNumber < 0) {
            scoreNumber = 0;
        }
        scoreNumberView.setText(String.valueOf(scoreNumber));
    }

    public void setCallBack(ICallBack callBack) {
        _callBack = callBack;
    }

    public interface ICallBack {
        void onAddScore(int score);

        void onRemoveScore(int score);
    }
}
