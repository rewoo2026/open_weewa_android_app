package com.xh.hotme.record.score;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.xh.hotme.R;
import com.xh.hotme.bean.GameStatusListBean;
import com.xh.hotme.databinding.ScoreCenterLayoutSecondBinding;
import com.xh.hotme.databinding.ScoreCenterScoreViewBinding;
import com.xh.hotme.record.bean.DotStates;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.Constants;
import com.xh.hotme.utils.DialogUtil;
import com.xh.hotme.utils.GsonUtils;
import com.xh.hotme.utils.ScoreCenterUtil;

import java.util.ArrayList;
import java.util.List;


public class HmDotView extends FrameLayout {

    private final static String TAG = "HmScoreView";

    ScoreCenterScoreViewBinding viewBinding;


    List<DotStates> _customDotList = new ArrayList<>();
    List<TextView> _secondHostDotViewList = new ArrayList<>();
    List<TextView> _secondGuestDotViewList = new ArrayList<>();

    ScoreCenterLayoutSecondBinding _secondBinding;

    View.OnClickListener _hostTextViewListener, _hostSecondListener;
    View.OnClickListener _guestTextViewListener, _guestSecondListener;

    CustomDotAddDialog customDotDialog;

    GameStateListDialog stateEditDialog;

    CustomDotEditDialog customDotEditDialog;

    String _hostTeamName, _guestTeamName;

    List<Integer> _statTypeIntList;

    ISyncTimerListener _syncTimerListener;

    IHmStateListener _hmScoreCallback;


    Handler _handler;

    public String _competitionId, _videoId, _videoName;

    public int _dotScene;

    public String _markId;
    public String _markName;

    public String _defaultHostName;
    public String _defaultGuestName;


    private final Runnable _timeOutRunnable = () -> DialogUtil.dismissDialog();


    public HmDotView(Context context) {
        super(context, null);
    }

    public HmDotView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HmDotView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initUI();
    }

    private void initUI() {
        _handler = new Handler();
        viewBinding = ScoreCenterScoreViewBinding.inflate(LayoutInflater.from(getContext()), this, true);

        _defaultHostName = getContext().getString(R.string.home_team);
        _defaultGuestName = getContext().getString(R.string.guest_team);

        initDotTextViewListener();
        //初始化自定义打点，和监听事件响应
        initCustomDotView();

        viewBinding.ivDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && !_syncTimerListener.isDotEnable()) {
                    return;
                }
                if (_customDotList.size() >= 3) {
                    customDotEditDialog = new CustomDotEditDialog(getContext());
                    customDotEditDialog.setCallback(new CustomDotEditDialog.ICallback() {
                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onConfirm() {
                            resetCustomDotView();
                            initCustomDotView();

                            customDotEditDialog.dismiss();
                        }
                    });
                    customDotEditDialog.show();
                    return;
                }
                if (customDotDialog == null) {
                    customDotDialog = new CustomDotAddDialog(getContext());
                }
                if (customDotDialog != null && customDotDialog.isShowing()) {
                    customDotDialog.dismiss();
                }
                customDotDialog = null;
                customDotDialog = new CustomDotAddDialog(getContext());
                customDotDialog.setCallback(new CustomDotAddDialog.ICallback() {
                    @Override
                    public void onCancel() {
                        customDotDialog.dismiss();
                    }

                    @Override
                    public void onConfirm(String tag) {
                        int size = _customDotList.size();
                        int id = getResources().getIdentifier(String.format("cell_host_custom%d", size + 1), "id", getContext().getPackageName());
                        TextView tv = viewBinding.getRoot().findViewById(id);
                        int guestId = getResources().getIdentifier(String.format("cell_guest_custom%d", size + 1), "id", getContext().getPackageName());
                        TextView guestTv = viewBinding.getRoot().findViewById(guestId);
                        tv.setText(tag);
                        tv.setVisibility(View.VISIBLE);
                        tv.setOnClickListener(_hostTextViewListener);

                        guestTv.setText(tag);
                        guestTv.setVisibility(View.VISIBLE);
                        guestTv.setOnClickListener(_guestTextViewListener);

                        if (size == 0) {
                            DotStates dotStates = new DotStates(Constants.STAT_CUSTOM_1, tag);
                            _customDotList.add(dotStates);

                            tv.setTag(dotStates);
                            guestTv.setTag(dotStates);
                        } else if (size == 1) {
                            DotStates dotStates = new DotStates(Constants.STAT_CUSTOM_2, tag);
                            _customDotList.add(dotStates);

                            tv.setTag(dotStates);
                            guestTv.setTag(dotStates);
                        } else if (size == 2) {
                            DotStates dotStates = new DotStates(Constants.STAT_CUSTOM_3, tag);
                            _customDotList.add(dotStates);

                            tv.setTag(dotStates);
                            guestTv.setTag(dotStates);
                        }

                        ScoreCenterUtil.saveCustomDotRecord(_customDotList);

                        customDotDialog.dismiss();
                    }
                });
                customDotDialog.show();
            }
        });

        viewBinding.ivEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && _syncTimerListener.isDotEnable()) {
                    showStateListDialog();
                }
            }
        });

    }

    public void setSyncTmeListener(ISyncTimerListener listener) {
        this._syncTimerListener = listener;
    }


    private void showStateListDialog() {
        if (stateEditDialog != null && stateEditDialog.isShowing()) {
            stateEditDialog.dismiss();
        }

        stateEditDialog = new GameStateListDialog(getContext());
        stateEditDialog.setName(_hostTeamName, _guestTeamName);
        stateEditDialog.setStateType(_statTypeIntList);
        stateEditDialog.setCustomerList(_customDotList);
        stateEditDialog.setRecordInfo(_competitionId, _dotScene);
        stateEditDialog.setSyncTimerListener(this._syncTimerListener);
        stateEditDialog.setDataList(getStateList());
        stateEditDialog.setCallback(new GameStateListDialog.ICallback() {
            @Override
            public void onCancel() {

            }

            @Override
            public void onConfirm(String tag) {

            }
        });
        stateEditDialog.show();
    }

    private void initCustomDotView() {
        String customDotStr = ScoreCenterUtil.loadCustomDotRecord();
        if (!TextUtils.isEmpty(customDotStr)) {
            _customDotList = GsonUtils.fromJsonList(customDotStr, DotStates.class);

            for (int i = 0; i < _customDotList.size(); i++) {

                int id = getResources().getIdentifier(String.format("cell_host_custom%d", i + 1), "id", getContext().getPackageName());
                TextView tv = viewBinding.getRoot().findViewById(id);
                int guestId = getResources().getIdentifier(String.format("cell_guest_custom%d", i + 1), "id", getContext().getPackageName());
                TextView guestTv = viewBinding.getRoot().findViewById(guestId);
                String tag = _customDotList.get(i).getName();
                tv.setTag(_customDotList.get(i));
                tv.setText(tag);
                tv.setVisibility(View.VISIBLE);
                tv.setOnClickListener(_hostTextViewListener);

                guestTv.setTag(_customDotList.get(i));
                guestTv.setText(tag);
                guestTv.setVisibility(View.VISIBLE);
                guestTv.setOnClickListener(_guestTextViewListener);
            }
        }
    }

    private void resetCustomDotView() {
        for (int i = 0; i < 3; i++) {
            int id = getResources().getIdentifier(String.format("cell_host_custom%d", i + 1), "id", getContext().getPackageName());
            TextView tv = viewBinding.getRoot().findViewById(id);
            int guestId = getResources().getIdentifier(String.format("cell_guest_custom%d", i + 1), "id", getContext().getPackageName());
            TextView guestTv = viewBinding.getRoot().findViewById(guestId);
            tv.setText("");
            tv.setTag(null);
            tv.setVisibility(View.INVISIBLE);
            tv.setOnClickListener(null);

            guestTv.setText("");
            guestTv.setTag(null);
            guestTv.setVisibility(View.INVISIBLE);
            guestTv.setOnClickListener(null);
        }
    }

    private void initDotTextViewListener() {

        _hostTextViewListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && !_syncTimerListener.isDotEnable()) {
                    return;
                }

                TextView tv = (TextView) view;
                if (view.getId() == R.id.cell_host_goal) {
                    resetSecondDotView();
                    showHostSecondViewGoal(true);
                } else if (view.getId() == R.id.cell_host_shoot) {
                    resetSecondDotView();
                    showSecondViewShoot(true);
                } else if (view.getId() == R.id.cell_host_foul) {
                    resetSecondDotView();
                    showSecondViewFoul(true);
                } else if (view.getId() == R.id.cell_host_pass) {
                    resetSecondDotView();
                    showSecondViewPass(true);
                } else if (view.getId() == R.id.cell_host_attack) {
                    resetSecondDotView();
                    showSecondViewAttack(true);
                } else if (view.getId() == R.id.cell_host_defense) {
                    resetSecondDotView();
                    showSecondViewDefense(true);
                } else if (view.getId() == R.id.cell_host_corner) {
                    AppTrace.d(TAG, "click host corner");
                    DotStates dotStates = new DotStates( Constants.STAT_CORNER_BALL, tv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(1, dotStates);
                    }

                } else if (view.getId() == R.id.cell_host_change) {
                    AppTrace.d(TAG, "click host exchage");
                    DotStates dotStates = new DotStates( Constants.STAT_EXCHANGE, tv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(1, dotStates);
                    }
                } else if (view.getId() == R.id.cell_host_ball_control) {
                    AppTrace.d(TAG, "click host control");
                    DotStates dotStates = new DotStates( Constants.STAT_CONTROL, tv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(1, dotStates);
                    }
                } else {
                    AppTrace.d(TAG, "click host custom：" + tv.getText().toString());
                    DotStates dotStates = (DotStates) tv.getTag();
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(1, dotStates);
                    }
                }
            }
        };

        _guestTextViewListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && !_syncTimerListener.isDotEnable()) {
                    return;
                }
                TextView guestTv = (TextView) view;

                if (view.getId() == R.id.cell_guest_goal) {
                    resetSecondDotView();
                    showHostSecondViewGoal(false);
                } else if (view.getId() == R.id.cell_guest_shoot) {
                    resetSecondDotView();
                    showSecondViewShoot(false);
                } else if (view.getId() == R.id.cell_guest_foul) {
                    resetSecondDotView();
                    showSecondViewFoul(false);
                } else if (view.getId() == R.id.cell_guest_pass) {
                    resetSecondDotView();
                    showSecondViewPass(false);
                } else if (view.getId() == R.id.cell_guest_attack) {
                    resetSecondDotView();
                    showSecondViewAttack(false);
                } else if (view.getId() == R.id.cell_guest_defense) {
                    resetSecondDotView();
                    showSecondViewDefense(false);
                } else if (view.getId() == R.id.cell_guest_corner) {
                    AppTrace.d(TAG, "click guest corner");
                    //显示最近的打点
                    DotStates dotStates = new DotStates( Constants.STAT_CORNER_BALL, guestTv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(0, dotStates);
                    }
                } else if (view.getId() == R.id.cell_guest_change) {
                    AppTrace.d(TAG, "click guest exchange");
                    DotStates dotStates = new DotStates( Constants.STAT_EXCHANGE, guestTv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(0, dotStates);
                    }
                } else if (view.getId() == R.id.cell_guest_ball_control) {
                    AppTrace.d(TAG, "click guest control");
                    DotStates dotStates = new DotStates( Constants.STAT_CONTROL, guestTv.getText().toString());
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(0, dotStates);
                    }
                } else {
                    AppTrace.d(TAG, "click guest custom：" + guestTv.getText().toString());
                    DotStates dotStates = (DotStates) guestTv.getTag();
                    if (_hmScoreCallback != null) {
                        _hmScoreCallback.onAddState(0, dotStates);
                    }
                }
            }
        };

        _hostSecondListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && !_syncTimerListener.isDotEnable()) {
                    return;
                }
                TextView textView = (TextView) view;
                DotStates dotStates = (DotStates) textView.getTag();
                String content = dotStates.getName();
                AppTrace.d(TAG, "click host type=" + content);
                _secondBinding.layoutSecond.setVisibility(View.GONE);

                //显示最近的打点
                if (_hmScoreCallback != null) {
                    _hmScoreCallback.onAddState(1, dotStates);
                }
            }
        };

        _guestSecondListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_syncTimerListener != null && !_syncTimerListener.isDotEnable()) {
                    return;
                }
                TextView textView = (TextView) view;
                DotStates dotStates = (DotStates) textView.getTag();
                String content = dotStates.getName();
                AppTrace.d(TAG, "click guest type=" + content);

                _secondBinding.layoutSecond.setVisibility(View.GONE);

                //显示最近的打点
                if (_hmScoreCallback != null) {
                    _hmScoreCallback.onAddState(0, dotStates);
                }
            }
        };
        //设置主队的打点监听
        viewBinding.cellHostGoal.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostShoot.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostChange.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostBallControl.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostCorner.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostPass.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostAttack.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostDefense.setOnClickListener(_hostTextViewListener);
        viewBinding.cellHostFoul.setOnClickListener(_hostTextViewListener);

        //设置客队的打点监听
        viewBinding.cellGuestGoal.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestShoot.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestChange.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestBallControl.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestCorner.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestPass.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestAttack.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestDefense.setOnClickListener(_guestTextViewListener);
        viewBinding.cellGuestFoul.setOnClickListener(_guestTextViewListener);

        //设置覆层的监听以及textview列表
        _secondBinding = ScoreCenterLayoutSecondBinding.bind(viewBinding.getRoot());
        Resources rs = getContext().getResources();
        for (int i = 0; i < 16; i++) {
            int id = rs.getIdentifier(String.format("second_cell_host_%d", i), "id", getContext().getPackageName());
            TextView tv = _secondBinding.getRoot().findViewById(id);
            tv.setOnClickListener(_hostSecondListener);
            _secondHostDotViewList.add(tv);
            int guestId = rs.getIdentifier(String.format("second_cell_guest_%d", i), "id", getContext().getPackageName());
            TextView guestTv = _secondBinding.getRoot().findViewById(guestId);
            guestTv.setOnClickListener(_guestSecondListener);
            _secondGuestDotViewList.add(guestTv);
        }

        _secondBinding.viewSecondBg.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                _secondBinding.layoutSecond.setVisibility(View.GONE);
                return true;
            }
        });
    }

    private void resetSecondDotView() {
        _secondBinding.layoutSecond.setVisibility(View.VISIBLE);
        for (int i = 0; i < 16; i++) {
            _secondHostDotViewList.get(i).setEnabled(true);
            _secondHostDotViewList.get(i).setVisibility(View.INVISIBLE);
            _secondGuestDotViewList.get(i).setVisibility(View.INVISIBLE);
            _secondGuestDotViewList.get(i).setEnabled(true);
            _secondHostDotViewList.get(i).setText("");
            _secondGuestDotViewList.get(i).setText("");
        }
    }

    //进球
    private void showHostSecondViewGoal(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(0), new DotStates(Constants.STAT_GOAL_2, "乌龙"));
            setSecondTextViewDisable(_secondHostDotViewList.get(4), "进球");
            setSecondTextView(_secondHostDotViewList.get(5), new DotStates(Constants.STAT_GOAL_0, "进球"));
            setSecondTextView(_secondHostDotViewList.get(8), new DotStates(Constants.STAT_GOAL_4, "点球"));
            setSecondTextView(_secondHostDotViewList.get(9), new DotStates(Constants.STAT_GOAL_5, "点球射失"));
            setSecondTextView(_secondHostDotViewList.get(12), new DotStates(Constants.STAT_GOAL_1, "任意球"));
            setSecondTextView(_secondHostDotViewList.get(13), new DotStates(Constants.STAT_GOAL_3, "头球"));

        } else {
            setSecondTextView(_secondGuestDotViewList.get(3), new DotStates(Constants.STAT_GOAL_2, "乌龙"));
            setSecondTextView(_secondGuestDotViewList.get(6), new DotStates(Constants.STAT_GOAL_0, "进球"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(7), "进球");
            setSecondTextView(_secondGuestDotViewList.get(10), new DotStates(Constants.STAT_GOAL_4, "点球"));
            setSecondTextView(_secondGuestDotViewList.get(11), new DotStates(Constants.STAT_GOAL_5, "点球射失"));
            setSecondTextView(_secondGuestDotViewList.get(14), new DotStates(Constants.STAT_GOAL_1, "任意球"));
            setSecondTextView(_secondGuestDotViewList.get(15), new DotStates(Constants.STAT_GOAL_3, "头球"));
        }
    }

    //射门
    private void showSecondViewShoot(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(4), new DotStates(Constants.STAT_SHOOT_0, "射正"));
            setSecondTextViewDisable(_secondHostDotViewList.get(8), "射门");
            setSecondTextView(_secondHostDotViewList.get(9), new DotStates(Constants.STAT_SHOOT_1, "门框"));
            setSecondTextView(_secondHostDotViewList.get(12), new DotStates(Constants.STAT_SHOOT_2, "射偏"));

        } else {
            setSecondTextView(_secondGuestDotViewList.get(7), new DotStates(Constants.STAT_SHOOT_0, "射正"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(11), "射门");
            setSecondTextView(_secondGuestDotViewList.get(10), new DotStates(Constants.STAT_SHOOT_1, "门框"));
            setSecondTextView(_secondGuestDotViewList.get(15), new DotStates(Constants.STAT_SHOOT_2, "射偏"));
        }
    }

    private void setSecondTextView(TextView textView, DotStates dotStates) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(dotStates.getName());
        textView.setTag(dotStates);
    }

    private void setSecondTextViewDisable(TextView textView, String text) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(text);
        textView.setEnabled(false);
    }

    //犯规
    private void showSecondViewFoul(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(5), new DotStates(Constants.STAT_FOUL_1, "红牌"));
            setSecondTextView(_secondHostDotViewList.get(8), new DotStates(Constants.STAT_FOUL_3, "越位"));
            setSecondTextViewDisable(_secondHostDotViewList.get(9), "犯规");
            setSecondTextView(_secondHostDotViewList.get(10), new DotStates(Constants.STAT_FOUL_0, "普通犯规"));
            setSecondTextView(_secondHostDotViewList.get(13), new DotStates(Constants.STAT_FOUL_2, "黄牌"));
        } else {
            setSecondTextView(_secondGuestDotViewList.get(6), new DotStates(Constants.STAT_FOUL_1, "红牌"));
            setSecondTextView(_secondGuestDotViewList.get(9), new DotStates(Constants.STAT_FOUL_0, "普通犯规"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(10), "犯规");
            setSecondTextView(_secondGuestDotViewList.get(11), new DotStates(Constants.STAT_FOUL_3, "越位"));
            setSecondTextView(_secondGuestDotViewList.get(14), new DotStates(Constants.STAT_FOUL_2, "黄牌"));
        }
    }

    //进攻
    private void showSecondViewAttack(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(10), new DotStates(Constants.STAT_ATTACK_2, "危险进攻"));
            setSecondTextView(_secondHostDotViewList.get(11), new DotStates(Constants.STAT_ATTACK_0, "过人"));
            setSecondTextView(_secondHostDotViewList.get(13), new DotStates(Constants.STAT_ATTACK_3, "普通进攻"));
            setSecondTextViewDisable(_secondHostDotViewList.get(14), "进攻");
            setSecondTextView(_secondHostDotViewList.get(15), new DotStates(Constants.STAT_ATTACK_1, "尝试过人"));

        } else {
            setSecondTextView(_secondGuestDotViewList.get(8), new DotStates(Constants.STAT_ATTACK_0, "过人"));
            setSecondTextView(_secondGuestDotViewList.get(9), new DotStates(Constants.STAT_ATTACK_2, "危险进攻"));
            setSecondTextView(_secondGuestDotViewList.get(12), new DotStates(Constants.STAT_ATTACK_3, "尝试过人"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(13), "进攻");
            setSecondTextView(_secondGuestDotViewList.get(14), new DotStates(Constants.STAT_ATTACK_1, "普通进攻"));
        }
    }

    //防守
    private void showSecondViewDefense(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(9), new DotStates(Constants.STAT_DEFENSE_3, "解围"));
            setSecondTextView(_secondHostDotViewList.get(10), new DotStates(Constants.STAT_DEFENSE_2, "拦截"));
            setSecondTextView(_secondHostDotViewList.get(11), new DotStates(Constants.STAT_DEFENSE_1, "抢断"));
            setSecondTextView(_secondHostDotViewList.get(13), new DotStates(Constants.STAT_DEFENSE_0, "扑救"));
            setSecondTextView(_secondHostDotViewList.get(14), new DotStates(Constants.STAT_DEFENSE_4, "封堵"));
            setSecondTextViewDisable(_secondHostDotViewList.get(15), "防守");

        } else {
            setSecondTextView(_secondGuestDotViewList.get(8), new DotStates(Constants.STAT_DEFENSE_1, "抢断"));
            setSecondTextView(_secondGuestDotViewList.get(9), new DotStates(Constants.STAT_DEFENSE_2, "拦截"));
            setSecondTextView(_secondGuestDotViewList.get(10), new DotStates(Constants.STAT_DEFENSE_3, "解围"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(12), "防守");
            setSecondTextView(_secondGuestDotViewList.get(13), new DotStates(Constants.STAT_DEFENSE_4, "封堵"));
            setSecondTextView(_secondGuestDotViewList.get(14), new DotStates(Constants.STAT_DEFENSE_0, "扑救"));
        }
    }

    //过人
    private void showSecondViewPass(boolean isHost) {
        if (isHost) {
            setSecondTextView(_secondHostDotViewList.get(9), new DotStates(Constants.STAT_PASS_0, "传球"));
            setSecondTextView(_secondHostDotViewList.get(10), new DotStates(Constants.STAT_PASS_1, "直塞"));
            setSecondTextView(_secondHostDotViewList.get(12), new DotStates(Constants.STAT_PASS_3, "传球失败"));
            setSecondTextViewDisable(_secondHostDotViewList.get(13), "传球");
            setSecondTextView(_secondHostDotViewList.get(14), new DotStates(Constants.STAT_PASS_2, "传中"));

        } else {

            setSecondTextView(_secondGuestDotViewList.get(9), new DotStates(Constants.STAT_PASS_1, "直塞"));
            setSecondTextView(_secondGuestDotViewList.get(10), new DotStates(Constants.STAT_PASS_0, "传球"));
            setSecondTextView(_secondGuestDotViewList.get(13), new DotStates(Constants.STAT_PASS_2, "传中"));
            setSecondTextViewDisable(_secondGuestDotViewList.get(14), "传球");
            setSecondTextView(_secondGuestDotViewList.get(15), new DotStates(Constants.STAT_PASS_3, "传球失败"));

        }
    }

    public int getRecordTime() {
        if (_syncTimerListener != null) {
            return _syncTimerListener.getTime();
        }
        return 0;
    }

    public List<GameStatusListBean> getStateList() {
        if (_syncTimerListener != null) {
            return _syncTimerListener.getStateList();
        }
        return new ArrayList<>();
    }

    public void reloadState(){
        if(stateEditDialog != null && stateEditDialog.isShowing()){
            stateEditDialog.setDataList(getStateList());

            stateEditDialog.reload();
        }
    }

    public void setDotScene(int scene) {
        _dotScene = scene;
    }

    public void setCompetitionId(String id) {
        _competitionId = id;
    }

    public void setVideoId(String id) {
        _videoId = id;
    }


    public void setVideoName(String name) {
        _videoName = name;
    }

    public void setHostTeamName(String name) {
        _hostTeamName = name;
    }

    public void setGuestTeamName(String name) {
        _guestTeamName = name;
    }

    public void setCallback(IHmStateListener callback) {
        _hmScoreCallback = callback;
    }

    public void setStateTypeList(List<Integer> stateTypeList) {
        if (_statTypeIntList == null) {
            _statTypeIntList = new ArrayList<>();
        } else {
            _statTypeIntList.clear();
        }
        _statTypeIntList.addAll(stateTypeList);

    }
    public void setMark(String markName, String id) {
        _markId = id;
        _markName = markName;
    }
    public void setMarkName(String markName) {
        _markName = markName;
    }
}
