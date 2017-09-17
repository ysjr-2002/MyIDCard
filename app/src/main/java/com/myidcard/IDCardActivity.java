package com.myidcard;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.handheld.IDCard.*;

import java.io.File;
import java.lang.*;

public class IDCardActivity extends Activity implements OnClickListener {

    Button btnOpen;
    Button btnClose;
    Button btnClear;
    Button btnExit;
    TextView edittext_name;
    TextView edittext_sex;
    TextView edittext_nation;
    TextView edittext_year;
    TextView edittext_address;
    TextView edittext_id;
    TextView edittext_validate;

    ImageView imageview;
    Bitmap photoBitmap = null;

    boolean bOpen = false;
    Toast toast = null;

    private IDCardManager manager;
    private ReadThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idcard);

        this.initView();
        this.OpenOrCloseView(false);

        String path = Environment.getExternalStorageDirectory() + "/IDCard";
        File file_paper = new File(path);
        if (!file_paper.exists()) {
            file_paper.mkdir();
        }

        Util.initSoundPool(IDCardActivity.this);
        thread = new ReadThread();
        thread.start();
    }

    private void initView() {

        btnOpen = (Button) findViewById(R.id.button_open);
        btnClose = (Button) findViewById(R.id.button_close);
        btnClear = (Button) findViewById(R.id.button_clear);
        btnExit = (Button) findViewById(R.id.button_finish);

        edittext_name = (TextView) findViewById(R.id.editText_name);
        edittext_sex = (TextView) findViewById(R.id.editText_sex);
        edittext_nation = (TextView) findViewById(R.id.editText_nation);
        edittext_year = (TextView) findViewById(R.id.editText_year);
        imageview = (ImageView) findViewById(R.id.imageview);
        edittext_address = (TextView) findViewById(R.id.editText_address);
        edittext_id = (TextView) findViewById(R.id.editText_id);
        edittext_validate = (TextView) findViewById(R.id.editText_validate);

        btnOpen.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnExit.setOnClickListener(this);

    }

    private void showToast(String info) {
        if (toast == null) {
            toast = Toast.makeText(IDCardActivity.this, info, Toast.LENGTH_SHORT);
        } else {
            toast.setText(info);
        }
        toast.show();
    }

    private void OpenOrCloseView(boolean flag) {
        if (flag) {
            btnOpen.setClickable(false);
            btnClose.setClickable(true);
            btnOpen.setTextColor(Color.GRAY);
            btnClose.setTextColor(Color.BLACK);
        } else {
            btnOpen.setTextColor(Color.BLACK);
            btnOpen.setClickable(true);
            btnClose.setClickable(false);
            btnClose.setTextColor(Color.GRAY);
        }
    }

    private Handler handle = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1:
                    showToast("发现证件");
                    break;
                case 1999:
                    showToast("读取证据，请稍等...");
                    break;
                case 2010:
                    Util.play(1, 0);
                    Bundle bundle = msg.getData();
                    String name = bundle.getString("name");
                    String sex = bundle.getString("sex");
                    String nation = bundle.getString("nation");
                    String year = bundle.getString("year");
                    String month = bundle.getString("month");
                    String day = bundle.getString("day");
                    String address = bundle.getString("address");
                    String id = bundle.getString("id");
//                    String office = bundle.getString("office");
                    String start = bundle.getString("begin");
                    String stop = bundle.getString("end");
//                    String newaddress = bundle.getString("newaddress");
//                    String fp1 = bundle.getString("fp1");
//                    String fp2 = bundle.getString("fp2");

                    edittext_name.setText(name);
                    edittext_sex.setText(sex);
                    edittext_nation.setText(nation);
                    edittext_year.setText(year + "-" + month + "-" + day);
                    imageview.setImageBitmap(photoBitmap);
                    edittext_address.setText(address);
                    edittext_id.setText(id);
                    edittext_validate.setText(start + "-" + stop);
                    break;
                case 2:
                    break;
            }
        }
    };

    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id) {
            case R.id.button_open:
                this.OpenOrCloseView(true);
                try {
                    if (manager == null) {
                        manager = new IDCardManager(IDCardActivity.this);
                    }
                    bOpen = true;

                    Util.play(1,0);
                } catch (Exception ex) {
                    bOpen = false;
                    ex.printStackTrace();
                }
                break;
            case R.id.button_close:
                bOpen = false;
                OpenOrCloseView(false);
                if (manager != null) {
                    manager.close();
                    manager = null;
                }
                break;
            case R.id.button_clear:
                clear();
                break;
            case R.id.button_finish:
                this.finish();
                break;
        }
    }

    boolean runFlag = true;

    private class ReadThread extends Thread {
        @Override
        public void run() {

            while (runFlag) {

                if (bOpen && manager != null) {

                    if (manager.findCard(200)) {

                        handle.sendEmptyMessage(1);
                        IDCardModel model = null;
                        long time = System.currentTimeMillis();
                        if (model != null) {

                        } else {

                            handle.sendEmptyMessage(1999);
                            model = manager.getData(2000);
                            if (model != null) {

                                SendMessage(model);
                            }
                        }
                    }
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private void clear() {

        edittext_name.setText("");
        edittext_sex.setText("");
        edittext_nation.setText("");
        edittext_year.setText("");
        edittext_address.setText("");
        edittext_id.setText("");
        edittext_validate.setText("");
        imageview.setImageResource(R.drawable.photo);
    }

    private void SendMessage(IDCardModel model) {

        Message msg = handle.obtainMessage();
        msg.what = 2010;
        Bundle bundle = new Bundle();
        bundle.putString("name", model.getName());
        bundle.putString("sex", model.getSex());
        bundle.putString("nation", model.getNation());
        bundle.putString("year", model.getYear());
        bundle.putString("month", model.getMonth());
        bundle.putString("day", model.getDay());
        bundle.putString("address", model.getAddress());

        bundle.putString("id", model.getIDCardNumber());
        bundle.putString("begin", model.getBeginTime());
        bundle.putString("end", model.getEndTime());
        photoBitmap = model.getPhotoBitmap();
        msg.setData(bundle);
        handle.sendMessage(msg);
    }

}
