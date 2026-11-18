package com.xh.hotme.me.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xh.hotme.R;
import com.xh.hotme.account.LoginManager;
import com.xh.hotme.account.RegisterActivity;
import com.xh.hotme.base.BaseActivity;
import com.xh.hotme.base.IEventBus;
import com.xh.hotme.bean.UserAvatarBean;
import com.xh.hotme.bean.UserInfoBean;
import com.xh.hotme.bluetooth.event.LoginEvent;
import com.xh.hotme.bluetooth.event.UpdateNameEvent;
import com.xh.hotme.databinding.ActivityProfileBinding;
import com.xh.hotme.http.ApiUtil;
import com.xh.hotme.listener.IGenericApiCallback;
import com.xh.hotme.me.holder.CommonViewHolder;
import com.xh.hotme.me.shared.SelectPhotoDialog;
import com.xh.hotme.utils.AppTrace;
import com.xh.hotme.utils.ClickGuard;
import com.xh.hotme.utils.MainHandler;
import com.xh.hotme.widget.imagepicker.ImagePickerCallback;
import com.xh.hotme.widget.imagepicker.LetoImagePicker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@IEventBus
public class ProfileActivity extends BaseActivity<ActivityProfileBinding> implements ImagePickerCallback, EasyPermissions.PermissionCallbacks {
    // view type
    private static final int TYPE_AVATAR = 0;
    private static final int TYPE_INFO = 1;

    // request code
    private static final int REQ_CAMERA_ACCESS = 1001;
    private static final int REQ_ALBUM_ACCESS = 1002;

    // image picker
    LetoImagePicker _imagePicker;

    // model
    private UserInfoBean _loginInfo;
    private List<Pair<String, String>> _infos;
    private List<String> _genders;

    // string
    private String _loading;
    private boolean _fromAlbum;

    public static void start(Context context) {
        if(null != context) {
            Intent intent = new Intent(context, ProfileActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    protected void initView() {
        // init
        _loginInfo = LoginManager.getUserLoginInfo();
        _infos = new ArrayList<>();
        buildModel();
        _imagePicker = LetoImagePicker.getInstance(this);

        // load strings
        _loading = getString(R.string.loading);

        _genders = Arrays.asList(
                getString(R.string.sex_secrecy),
                getString(R.string.sex_man),
                getString(R.string.sex_woman)
        );

        // back click
        viewBinding.titlebar.ivBack.setOnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                finish();
                return true;
            }
        });

        // title
        viewBinding.titlebar.tvTitle.setText(getString(R.string.ly_me_info));

        // setup list
        viewBinding.list.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.list.setAdapter(new ProfileAdapter());

        // register context menu
        registerForContextMenu(viewBinding.list);
    }

    @Override
    protected void initData() {

    }

    private void buildModel() {
        _infos.clear();
        _infos.add(Pair.create(getString(R.string.avatar), _loginInfo.avatar));
        _infos.add(Pair.create(getString(R.string.nickname), _loginInfo.nickName));
        _infos.add(Pair.create(getString(R.string.phone_number), _loginInfo.phone));
        if (_loginInfo != null && _loginInfo.email != null) {
            _infos.add(Pair.create(getString(R.string.email), _loginInfo.email));
        }else{
            _infos.add(Pair.create(getString(R.string.email), ""));
        }
        _infos.add(Pair.create(getString(R.string.reset_pwd),""));

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventNickName(UpdateNameEvent event) {
        MainHandler.runOnUiThread(() -> {
            _loginInfo.nickName = event.nickName;
            buildModel();

            viewBinding.list.getAdapter().notifyDataSetChanged();

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister
        unregisterForContextMenu(viewBinding.list);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        for(String item : _genders) {
            menu.add(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        // save change, no matter success or fail
        int gender = _genders.indexOf(item.getTitle());
//		_loginInfo.setGender(gender);
//		LoginControl.saveLoginInfo(ProfileActivity.this, _loginInfo);

        // modify
        doModifyInfo("", gender);

        // return
        return true;
    }

    private void doModifyInfo(String nickname, int gender) {
//		ApiUtil.modifyUserInfo(LeBoxProfileActivity.this, nickname, "", gender, new HttpCallbackDecode<ModifyUserInfoResultBean>(this, null) {
//			@Override
//			public void onDataSuccess(ModifyUserInfoResultBean data) {
//				if(data != null) {
//					buildModel();
//					_listView.getAdapter().notifyDataSetChanged();
//
//					// trigger event to let outer activity updated
//					EventBus.getDefault().post(new DataRefreshEvent());
//				}
//			}
//		});
    }

    private void showMobile() {
        if(_loginInfo != null) {
            MobileViewActivity.start(ProfileActivity.this, _loginInfo.phone);
        }

    }

    private void showReset() {
        if (_loginInfo != null && _loginInfo.email != null) {
            RegisterActivity.start(ProfileActivity.this,1,_loginInfo.email);
        }else{
            RegisterActivity.start(ProfileActivity.this,1,_loginInfo.phone);
        }

    }

    private void showNickInput() {
        SetNicknameActivity.start(ProfileActivity.this);
    }

    private void showAvatarPicker() {
        SelectPhotoDialog dialog = new SelectPhotoDialog(ProfileActivity.this);
        dialog.setCameraBtnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                pickImage(false);
                return true;
            }
        });
        dialog.setAlbumBtnClickListener(new ClickGuard.GuardedOnClickListener() {
            @Override
            public boolean onClicked() {
                pickImage(true);
                return true;
            }
        });
        dialog.show();
    }

    private void pickImage(boolean fromAlbum) {
        _fromAlbum = fromAlbum;
        if(_fromAlbum) {
            if(!checkStoragePermission(false)) {
                return;
            }
            _imagePicker.pickFromAlbum("avatar.jpg", 256, 256, true, ProfileActivity.this);
        } else {
            if(!checkTakePhotoPermission(false)) {
                return;
            }
            if(!checkStoragePermission(false)) {
                return;
            }
            _imagePicker.pickFromCamera("avatar.jpg", 256, 256, true, false, ProfileActivity.this);
        }
    }

    @Override
    protected void onStoragePermissionGranted() {
        pickImage(_fromAlbum);
    }

    @Override
    protected void onOnePermissionGranted(String perm) {
        if(Manifest.permission.CAMERA.equals(perm)) {
            pickImage(false);
        }
    }
    @Override
    protected void onOnePermissionDenied(String perm) {
        if(Manifest.permission.CAMERA.equals(perm)) {
            List<String> permissionList = new ArrayList<>();
            permissionList.add(perm);
            if (EasyPermissions.somePermissionPermanentlyDenied(this, permissionList)) {
                showConfirmDialog(
                        getString(R.string.permission_deny_title),
                        getString(R.string.permission_deny_take_photo),
                        getString(R.string.setting),
                        getString(R.string.cancel),
                        new ClickGuard.GuardedOnClickListener() {
                            @Override
                            public boolean onClicked() {
                                //跳转应用消息，间接打开应用权限设置-效率高
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", ProfileActivity.this.getPackageName(), null);
                                intent.setData(uri);
                                ProfileActivity.this.startActivity(intent);
                                return true;
                            }
                        },
                        null
                );
            } else {
                AppTrace.d(TAG, "onPermissionsDenied");
                //跳转应用消息，间接打开应用权限设置-效率高
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", ProfileActivity.this.getPackageName(), null);
                intent.setData(uri);
                ProfileActivity.this.startActivity(intent);
            }
        } else if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(perm) || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(perm)) {
            List<String> permissionList = new ArrayList<>();
            permissionList.add(perm);
            if (EasyPermissions.somePermissionPermanentlyDenied(this, permissionList)) {
                showConfirmDialog(
                        getString(R.string.permission_deny_title),
                        getString(R.string.permission_deny_storage),
                        getString(R.string.setting),
                        getString(R.string.cancel),
                        new ClickGuard.GuardedOnClickListener() {
                            @Override
                            public boolean onClicked() {
                                //跳转应用消息，间接打开应用权限设置-效率高
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", ProfileActivity.this.getPackageName(), null);
                                intent.setData(uri);
                                ProfileActivity.this.startActivity(intent);
                                return true;
                            }
                        },
                        null
                );
            } else {
                AppTrace.d(TAG, "onPermissionsDenied");
                //跳转应用消息，间接打开应用权限设置-效率高
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", ProfileActivity.this.getPackageName(), null);
                intent.setData(uri);
                ProfileActivity.this.startActivity(intent);
            }
        }

    }

    @Override
    public void onImagePicked(String path) {
//		try {
        // load avatar jpg file
        File f = new File(path);
//			Uri uri = Uri.fromFile(f);
//			int len = (int) f.length();
//			byte[] buf = new byte[len];
//			ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
//			InputStream fileStream = new FileInputStream(pfd.getFileDescriptor());
//			fileStream.read(buf, 0, buf.length);

        // modify portrait
        doUploadPortrait(f);
//		} catch(IOException e) {
//		}
    }

    @Override
    public void onImagePickingCancelled() {

    }

    private void doUploadPortrait(final File file) {
        showLoading(false, _loading);

        ApiUtil.modifyPortrait(file, new IGenericApiCallback<UserAvatarBean>() {
            @Override
            public void onSuccess(UserAvatarBean data) {
                AppTrace.d("modifyPortrait success");

                _loginInfo.avatar = data.imgUrl;
                UserInfoBean userInfoBean = LoginManager.getUserLoginInfo();
                userInfoBean.avatar = data.imgUrl;

                LoginManager.saveLoginInfo(userInfoBean);

                // reload ui
                buildModel();
                viewBinding.list.getAdapter().notifyDataSetChanged();

                //notify update profile
                EventBus.getDefault().post(new LoginEvent());
            }

            @Override
            public void onFail(String code, String message) {

            }

            @Override
            public void onFinish() {
                dismissLoading();
            }
        });
    }

    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {

    }

    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {

    }

    private class ProfileAdapter extends RecyclerView.Adapter<CommonViewHolder<Pair<String, String>>> {
        @NonNull
        @Override
        public CommonViewHolder<Pair<String, String>> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(viewType == TYPE_AVATAR) {
                return AvatarHolder.create(ProfileActivity.this, parent);
            }
            return SimpleUserInfoHolder.create(ProfileActivity.this, parent);
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0) {
                return TYPE_AVATAR;
            }
            return TYPE_INFO;
        }

        @SuppressLint("RecyclerView")
        @Override
        public void onBindViewHolder(@NonNull CommonViewHolder<Pair<String, String>> holder, final int position) {
            holder.onBind(_infos.get(position), position);
            holder.getItemView().setOnClickListener(new ClickGuard.GuardedOnClickListener() {
                @Override
                public boolean onClicked() {
                    switch(position) {
                        case 0:

                            showAvatarPicker();
                            break;
                        case 1:

                            showNickInput();
                            break;
                        case 2:

//                            showMobile();
                            break;
                        case 3:

                            break;
                        case 4:

                            showReset();
                            break;
                    }
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return _infos.size();
        }
    }
}
