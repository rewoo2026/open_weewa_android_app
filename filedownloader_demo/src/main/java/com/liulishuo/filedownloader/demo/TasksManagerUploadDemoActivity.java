/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadConnectListener;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadSampleListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.demo.databinding.ActivityTasksManagerUploadDemoBinding;
import com.liulishuo.filedownloader.demo.databinding.ItemTasksManagerBinding;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jacksgong on 1/9/16.
 */
public class TasksManagerUploadDemoActivity extends BaseActivity<ActivityTasksManagerUploadDemoBinding> {
    private TaskItemAdapter adapter;


    @Override
    protected void initView() {
        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.recyclerView.setAdapter(adapter = new TaskItemAdapter());
    }

    @Override
    protected void initData() {
        TaskManager.getImpl().onCreate(new WeakReference<>(this));
        postNotifyDataChanged();
    }

    public void postNotifyDataChanged() {
        if(adapter != null) {
            runOnUiThread(() -> {
                if(adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        TaskManager.getImpl().onDestroy();
        adapter = null;
        FileDownloader.getImpl().pauseAll();
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(!hasPermission()) {
            applyPermission();
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ;
        }
    }

    private void applyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constant.REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    // ============================================================================ view adapter ===

    private static class TaskItemViewHolder extends RecyclerView.ViewHolder {
        ItemTasksManagerBinding binding;

        public TaskItemViewHolder(View itemView) {
            super(itemView);
            binding = ItemTasksManagerBinding.bind(itemView);
        }

        /**
         * viewHolder position
         */
        private int position;
        /**
         * download id
         */
        private int id;

        public void update(final int id, final int position) {
            this.id = id;
            this.position = position;
        }

        public void updateUploaded() {
            binding.taskPb.setMax(1);
            binding.taskPb.setProgress(1);

            binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_upload_completed);
            binding.taskActionBtn.setText(R.string.finished);
        }

        public void updateNotUploaded(final int status, final long sofar, final long total) {
            if(sofar > 0 && total > 0) {
                final float percent = sofar
                    / (float) total;
                binding.taskPb.setMax(100);
                binding.taskPb.setProgress((int) (percent * 100));
            } else {
                binding.taskPb.setMax(1);
                binding.taskPb.setProgress(0);
            }

            switch(status) {
                case FileDownloadStatus.error:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_error);
                    break;
                case FileDownloadStatus.paused:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_paused);
                    break;
                default:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_not_uploaded);
                    break;
            }
            binding.taskActionBtn.setText(R.string.start);
        }

        public void updateDownloading(final int status, final long sofar, final long total) {
            final float percent = sofar
                / (float) total;
            binding.taskPb.setMax(100);
            binding.taskPb.setProgress((int) (percent * 100));

            switch(status) {
                case FileDownloadStatus.pending:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_pending);
                    break;
                case FileDownloadStatus.started:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_started);
                    break;
                case FileDownloadStatus.connected:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_connected);
                    break;
                case FileDownloadStatus.progress:
                    binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_progress);
                    break;
                default:
                    binding.taskStatusTv.setText(DemoApplication.CONTEXT.getString(
                        R.string.tasks_manager_demo_status_uploading, status));
                    break;
            }

            binding.taskActionBtn.setText(R.string.pause);
        }
    }

    private static class TaskItemAdapter extends RecyclerView.Adapter<TaskItemViewHolder> {

        private final FileDownloadListener taskDownloadListener = new FileDownloadSampleListener() {

            private TaskItemViewHolder checkCurrentHolder(final BaseDownloadTask task) {
                final TaskItemViewHolder tag = (TaskItemViewHolder) task.getTag();
                if(tag.id != task.getId()) {
                    return null;
                }

                return tag;
            }

            @Override
            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.pending(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.pending, soFarBytes
                    , totalBytes);
                tag.binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_pending);
            }

            @Override
            protected void started(BaseDownloadTask task) {
                super.started(task);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_started);
            }

            @Override
            protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                super.connected(task, etag, isContinue, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.connected, soFarBytes
                    , totalBytes);
                tag.binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_connected);
            }

            @Override
            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.progress(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateDownloading(FileDownloadStatus.progress, soFarBytes
                    , totalBytes);
            }

            @Override
            protected void error(BaseDownloadTask task, Throwable e) {
                super.error(task, e);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateNotUploaded(FileDownloadStatus.error, task.getLargeFileSoFarBytes()
                    , task.getLargeFileTotalBytes());
                TaskManager.getImpl().removeTaskForViewHolder(task.getId());
            }

            @Override
            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                super.paused(task, soFarBytes, totalBytes);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateNotUploaded(FileDownloadStatus.paused, soFarBytes, totalBytes);
                tag.binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_paused);
                TaskManager.getImpl().removeTaskForViewHolder(task.getId());
            }

            @Override
            protected void completed(BaseDownloadTask task) {
                super.completed(task);
                final TaskItemViewHolder tag = checkCurrentHolder(task);
                if(tag == null) {
                    return;
                }

                tag.updateUploaded();
                TaskManager.getImpl().removeTaskForViewHolder(task.getId());
            }
        };
        private final View.OnClickListener taskActionOnClickListener = v -> {
            if(v.getTag() == null) {
                return;
            }

            TaskItemViewHolder holder = (TaskItemViewHolder) v.getTag();

            CharSequence action = ((TextView) v).getText();
            if(action.equals(v.getResources().getString(R.string.pause))) {
                // to pause
                if(FileDownloader.getImpl().pause(holder.id) <= 0) {
                    final TaskModel model = TaskManager.getImpl().get(holder.position);
                    holder.updateNotUploaded(
                        FileDownloadStatus.pending,
                        TaskManager.getImpl().getSoFar(model.getId()),
                        TaskManager.getImpl().getTotal(model.getId())
                    );
                }
            } else if(action.equals(v.getResources().getString(R.string.start))) {
                // to start
                // to start
                final TaskModel model = TaskManager.getImpl().get(holder.position);
                final BaseDownloadTask task = FileDownloader.getImpl()
                    .create(model.getUrl())
                    .setPath(model.getPath())
                    .setUpload(true)
                    .setEtag(FileDownloadUtils.getDefaultObjectKey(model.getPath()))
                    .setCallbackProgressTimes(100)
                    .setListener(taskDownloadListener);

                TaskManager.getImpl()
                    .addTaskForViewHolder(task);

                TaskManager.getImpl()
                    .updateViewHolder(holder.id, holder);

                task.start();
            }
        };

        @Override
        public TaskItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskItemViewHolder holder = new TaskItemViewHolder(
                LayoutInflater.from(
                        parent.getContext())
                    .inflate(R.layout.item_tasks_manager, parent, false));

            holder.binding.taskActionBtn.setOnClickListener(taskActionOnClickListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(TaskItemViewHolder holder, int position) {
            final TaskModel model = TaskManager.getImpl().get(position);

            holder.update(model.getId(), position);
            holder.binding.taskActionBtn.setTag(holder);
            holder.binding.taskNameTv.setText(model.getName());

            TaskManager.getImpl()
                .updateViewHolder(holder.id, holder);

            holder.binding.taskActionBtn.setEnabled(true);


            if(TaskManager.getImpl().isReady()) {
                final int status = TaskManager.getImpl().getStatus(model.getId());
                if(status == FileDownloadStatus.pending || status == FileDownloadStatus.started ||
                    status == FileDownloadStatus.connected) {
                    // start task, but file not created yet
                    holder.updateDownloading(status, TaskManager.getImpl().getSoFar(model.getId())
                        , TaskManager.getImpl().getTotal(model.getId()));
                } else if(TaskManager.getImpl().isCompleted(status)) {
                    // already downloaded and exist
                    holder.updateUploaded();
                } else if(status == FileDownloadStatus.progress) {
                    // downloading
                    holder.updateDownloading(status, TaskManager.getImpl().getSoFar(model.getId())
                        , TaskManager.getImpl().getTotal(model.getId()));
                } else {
                    // not start
                    holder.updateNotUploaded(status, TaskManager.getImpl().getSoFar(model.getId())
                        , TaskManager.getImpl().getTotal(model.getId()));
                }
            } else {
                holder.binding.taskStatusTv.setText(R.string.tasks_manager_demo_status_loading);
                holder.binding.taskActionBtn.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return TaskManager.getImpl().getTaskCounts();
        }
    }


    // ============================================================================ controller ====

    public static class TaskManager {
        private final static class HolderClass {
            private final static TaskManager INSTANCE = new TaskManager();
        }

        public static TaskManager getImpl() {
            return HolderClass.INSTANCE;
        }

        private final TasksManagerDBController dbController;
        private final List<TaskModel> modelList;

        private TaskManager() {
            dbController = new TasksManagerDBController();
            modelList = dbController.getAllTasks();

            initDemo();
        }

        private void initDemo() {
            if(modelList.size() <= 0) {
                final int demoSize = Constant.SDCARD_BIG_FILES.length;
                for(int i = 0; i < demoSize; i++) {
                    final String url = Constant.SDCARD_BIG_FILES[i];
                    addTask(url);
                }
            }
        }

        private final SparseArray<BaseDownloadTask> taskSparseArray = new SparseArray<>();

        public void addTaskForViewHolder(final BaseDownloadTask task) {
            taskSparseArray.put(task.getId(), task);
        }

        public void removeTaskForViewHolder(final int id) {
            taskSparseArray.remove(id);
        }

        public void updateViewHolder(final int id, final TaskItemViewHolder holder) {
            final BaseDownloadTask task = taskSparseArray.get(id);
            if(task == null) {
                return;
            }

            task.setTag(holder);
        }

        public void releaseTask() {
            taskSparseArray.clear();
        }

        private FileDownloadConnectListener listener;

        private void registerServiceConnectionListener(final WeakReference<TasksManagerUploadDemoActivity>
                                                           activityWeakReference) {
            if(listener != null) {
                FileDownloader.getImpl().removeServiceConnectListener(listener);
            }

            listener = new FileDownloadConnectListener() {

                @Override
                public void connected() {
                    if(activityWeakReference == null
                        || activityWeakReference.get() == null) {
                        return;
                    }

                    activityWeakReference.get().postNotifyDataChanged();
                }

                @Override
                public void disconnected() {
                    if(activityWeakReference == null
                        || activityWeakReference.get() == null) {
                        return;
                    }

                    activityWeakReference.get().postNotifyDataChanged();
                }
            };

            FileDownloader.getImpl().addServiceConnectListener(listener);
        }

        private void unregisterServiceConnectionListener() {
            FileDownloader.getImpl().removeServiceConnectListener(listener);
            listener = null;
        }

        public void onCreate(final WeakReference<TasksManagerUploadDemoActivity> activityWeakReference) {
            if(!FileDownloader.getImpl().isServiceConnected()) {
                FileDownloader.getImpl().bindService();
                registerServiceConnectionListener(activityWeakReference);
            }
        }

        public void onDestroy() {
            unregisterServiceConnectionListener();
            releaseTask();
        }

        public boolean isReady() {
            return FileDownloader.getImpl().isServiceConnected();
        }

        public TaskModel get(final int position) {
            return modelList.get(position);
        }

        public TaskModel getById(final int id) {
            for(TaskModel model : modelList) {
                if(model.getId() == id) {
                    return model;
                }
            }

            return null;
        }

        /**
         * @param status Download Status
         * @return has already downloaded
         * @see FileDownloadStatus
         */
        public boolean isCompleted(final int status) {
            return status == FileDownloadStatus.completed;
        }

        public int getStatus(final int id) {
            return FileDownloader.getImpl().getStatus(id);
        }

        public long getTotal(final int id) {
            return FileDownloader.getImpl().getTotal(id);
        }

        public long getSoFar(final int id) {
            return FileDownloader.getImpl().getSoFar(id);
        }

        public int getTaskCounts() {
            return modelList.size();
        }

        public TaskModel addTask(final String url) {
            return addTask(url, url);
        }

        public TaskModel addTask(final String url, final String path) {
            if(TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
                return null;
            }

            final int id = FileDownloadUtils.generateId(url, path);
            TaskModel model = getById(id);
            if(model != null) {
                return model;
            }
            final TaskModel newModel = dbController.addTask(url, path);
            if(newModel != null) {
                modelList.add(newModel);
            }

            return newModel;
        }
    }

    private static class TasksManagerDBController {
        public final static String TABLE_NAME = "tasksmanger";
        private final SQLiteDatabase db;

        private TasksManagerDBController() {
            TasksManagerDBOpenHelper openHelper = new TasksManagerDBOpenHelper(DemoApplication.CONTEXT);

            db = openHelper.getWritableDatabase();
        }

        @SuppressLint("Range")
        public List<TaskModel> getAllTasks() {
            final Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

            final List<TaskModel> list = new ArrayList<>();
            try {
                if(!c.moveToLast()) {
                    return list;
                }

                do {
                    TaskModel model = new TaskModel();
                    model.setId(c.getInt(c.getColumnIndex(TaskModel.ID)));
                    model.setName(c.getString(c.getColumnIndex(TaskModel.NAME)));
                    model.setUrl(c.getString(c.getColumnIndex(TaskModel.URL)));
                    model.setPath(c.getString(c.getColumnIndex(TaskModel.PATH)));
                    list.add(model);
                } while(c.moveToPrevious());
            } finally {
                if(c != null) {
                    c.close();
                }
            }

            return list;
        }

        public TaskModel addTask(final String url, final String path) {
            if(TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
                return null;
            }

            // have to use FileDownloadUtils.generateId to associate TasksManagerModel with FileDownloader
            final int id = FileDownloadUtils.generateId(url, path);

            TaskModel model = new TaskModel();
            model.setId(id);
            model.setName(DemoApplication.CONTEXT.getString(R.string.tasks_manager_demo_name, id));
            model.setUrl(url);
            model.setPath(path);

            final boolean succeed = db.insert(TABLE_NAME, null, model.toContentValues()) != -1;
            return succeed ? model : null;
        }


    }

    // ----------------------- model
    private static class TasksManagerDBOpenHelper extends SQLiteOpenHelper {
        public final static String DATABASE_NAME = "tasksmanager_up.db";
        public final static int DATABASE_VERSION = 1;

        public TasksManagerDBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "
                + TasksManagerDBController.TABLE_NAME
                + String.format(
                "("
                    + "%s INTEGER PRIMARY KEY, " // id, download id
                    + "%s VARCHAR, " // name
                    + "%s VARCHAR, " // url
                    + "%s VARCHAR " // path
                    + ")"
                , TaskModel.ID
                , TaskModel.NAME
                , TaskModel.URL
                , TaskModel.PATH

            ));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.delete(TasksManagerDBController.TABLE_NAME, null, null);
        }
    }

    private static class TaskModel {
        public final static String ID = "id";
        public final static String NAME = "name";
        public final static String URL = "url";
        public final static String PATH = "path";

        private int id;
        private String name;
        private String url;
        private String path;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public ContentValues toContentValues() {
            ContentValues cv = new ContentValues();
            cv.put(ID, id);
            cv.put(NAME, name);
            cv.put(URL, url);
            cv.put(PATH, path);
            return cv;
        }
    }


}
