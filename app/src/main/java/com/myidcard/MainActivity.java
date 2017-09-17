package com.myidcard;

import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.handheld.IDCard.IDCardManager;
import com.handheld.IDCard.IDCardModel;
import com.pci.pca.readcard.SerialPort;
import com.pci.pca.readcard.Tools;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {


    private EditText editText_name;
    private EditText editText_sex;
    private EditText editText_nation;
    private EditText editText_year;
    private EditText editText_month;
    private EditText editText_day;
    private EditText editText_address;
    private EditText editText_IDCard;
    private EditText editText_office;
    private EditText editText_effective;
    private EditText editText_fingerprint1;
    private EditText editText_fingerprint2;
    private CheckBox checkBox_fp;

    private Button button_open;
    private Button button_clear;
    private Button button_close;
    private Button button_finish;
    private ImageView imageView;
    private Bitmap photoBitmap = null;
    private IDCardManager manager;
    private ReadThread thread;
    private Toast toast;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_open:
                this.OpenOrCloseView(false);
                if (manager == null) {
                    manager = new IDCardManager(MainActivity.this );
                }
                startFlag=true;
                break;
            case R.id.button_clear:
                clear();
                break;
            case R.id.button_close:
                startFlag = false;
                OpenOrCloseView(true);

                if (manager!=null) {
                    manager.close();
                    manager = null;
                }
                break;
            case R.id.button_finish:
                finish();
                break;
            default:
                break;
        }
    }

    private void OpenOrCloseView(boolean flag){
        if (flag) {
            button_close.setClickable(false);
//		button_finish.setClickable(false);
            button_clear.setClickable(false);
            button_clear.setTextColor(Color.GRAY);
//		button_finish.setTextColor(Color.GRAY);
            button_close.setTextColor(Color.GRAY);
            button_open.setClickable(true);
            button_open.setTextColor(Color.BLACK);
        }else {
            button_close.setClickable(true);
            button_finish.setClickable(true);
            button_clear.setClickable(true);
            button_clear.setTextColor(Color.BLACK);
            button_finish.setTextColor(Color.BLACK);
            button_close.setTextColor(Color.BLACK);
            button_open.setClickable(false);
            button_open.setTextColor(Color.GRAY);
        }
    }


    private void showToast(String info){
        if (toast==null) {
            toast = Toast.makeText(MainActivity.this, info, 0);
        }else {
            toast.setText(info);
        }
        toast.show();
    }

    private Handler handler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 2010:
                    Util.play(1, 0);
                    showToast("???????");
                    Bundle bundle = msg.getData();
                    //?????????????????????????????????????????????????????????????
                    String name = bundle.getString("name");
                    String sex = bundle.getString("sex");
                    String nation = bundle.getString("nation");
                    String year = bundle.getString("year");
                    String month = bundle.getString("month");
                    String day = bundle.getString("day");
                    String address = bundle.getString("address");
                    String id = bundle.getString("id");
                    String office = bundle.getString("office");
                    String start = bundle.getString("begin");
                    String stop = bundle.getString("end");
                    String newaddress = bundle.getString("newaddress");
                    String fp1 = bundle.getString("fp1");
                    String fp2 = bundle.getString("fp2");
                    //???????????
                    imageView.setImageBitmap(photoBitmap);
                    editText_name.setText(name);
                    editText_sex.setText(sex);
                    editText_nation.setText(nation);
                    editText_year.setText(year);
                    editText_month.setText(month);
                    editText_day.setText(day);
                    editText_address.setText(address);
                    editText_IDCard.setText(id);
                    editText_office.setText(office);
                    editText_effective.setText(start+"-"+stop);
                    editText_fingerprint1.setText(fp1);
                    editText_fingerprint2.setText(fp2);
                    break;
                case 1:
                    clear();
                    showToast("?????!\n?????????...");
                    break;
                case 2:
                    showToast("");
                    break;
                case 3:

                    break;
                default:
                    break;
            }
        };
    };

    private void clear(){
        editText_name.setText("");
        editText_sex.setText("");
        editText_nation.setText("");
        editText_year.setText("");
        editText_month.setText("");
        editText_day.setText("");
        editText_address.setText("");
        editText_IDCard.setText("");
        editText_office.setText("");
        editText_effective.setText("");
        editText_fingerprint1.setText("");
        editText_fingerprint2.setText("");
        imageView.setImageResource(R.drawable.photo);
    }

    private boolean runFlag =  true;
    private boolean startFlag = false;
    private class ReadThread extends Thread{
        @Override
        public void run() {
            while (runFlag) {
                if (startFlag&&manager!=null) {

                    if (manager.findCard(200)) {
                        handler.sendEmptyMessage(1);
                        IDCardModel model = null;

                        long time = System.currentTimeMillis();
                        if (checkBox_fp.isChecked()) {
                            //?????????????
                            model = manager.getDataFP(2000);
                            Log.e("get data time:", System.currentTimeMillis() - time +"ms");
                        }

                        if (model!=null) {
                            sendMessage(model.getName(), model.getSex(), model.getNation(),
                                    model.getYear(), model.getMonth(), model.getDay(),
                                    model.getAddress(), model.getIDCardNumber(), model.getOffice(),
                                    model.getBeginTime(), model.getEndTime(), model.getOtherData(),
                                    model.getPhotoBitmap(), Tools.Bytes2HexString(model.getFP1(), 512),Tools.Bytes2HexString(model.getFP2(), 512));
                        }else {
                            //??????????
                            model = manager.getData(2000);
                            if (model!=null) {
                                sendMessage(model.getName(), model.getSex(), model.getNation(),
                                        model.getYear(), model.getMonth(), model.getDay(),
                                        model.getAddress(), model.getIDCardNumber(), model.getOffice(),
                                        model.getBeginTime(), model.getEndTime(), model.getOtherData(),
                                        model.getPhotoBitmap(),"???????","???????");
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
            super.run();
        }
        private void sendMessage(String name, String sex, String nation,
                                 String year, String month, String day, String address, String id,
                                 String office, String start, String stop, String newaddress
                ,Bitmap bitmap,String fp1,String fp2) {
            Message message = new Message();
            message.what=2010;
            Bundle bundle = new Bundle();
            bundle.putString("name", name);
            bundle.putString("sex", sex);
            bundle.putString("nation", nation);
            bundle.putString("year", year);
            bundle.putString("month", month);
            bundle.putString("day", day);
            bundle.putString("address", address);
            bundle.putString("id", id);
            bundle.putString("office", office);
            bundle.putString("begin", start);
            bundle.putString("end", stop);
            bundle.putString("newaddress", newaddress);
            bundle.putString("fp1", fp1);
            bundle.putString("fp2", fp2);
            photoBitmap = bitmap;
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Util.initSoundPool(MainActivity.this);
        initView();
        String path = Environment.getExternalStorageDirectory()+"/IDCard";
        File file_paper = new File(path);
        if (!file_paper.exists())
        {
            file_paper.mkdirs();
        }
        thread = new ReadThread();
        thread.start();
    }

    private void initView() {
        // TODO Auto-generated method stub
        editText_name = (EditText) findViewById(R.id.editText_name);
        editText_sex = (EditText) findViewById(R.id.editText_sex);
        editText_nation = (EditText) findViewById(R.id.editText_nation);
        editText_year = (EditText) findViewById(R.id.editText_year);
        editText_month = (EditText) findViewById(R.id.editText_month);
        editText_day = (EditText) findViewById(R.id.editText_day);
        editText_address = (EditText) findViewById(R.id.editText_address);
        editText_IDCard = (EditText) findViewById(R.id.editText_IDCard);
        editText_office = (EditText) findViewById(R.id.editText_office);
        editText_effective = (EditText) findViewById(R.id.editText_effective);
        editText_fingerprint1 = (EditText) findViewById(R.id.editText_fp1);
        editText_fingerprint2 = (EditText) findViewById(R.id.editText_fp2);
        checkBox_fp = (CheckBox) findViewById(R.id.checkbox_fp);
//		editText_info = (EditText) findViewById(R.id.editText_info);
        button_open = (Button) findViewById(R.id.button_open);
        button_clear = (Button) findViewById(R.id.button_clear);
        button_close = (Button) findViewById(R.id.button_close);
        button_finish = (Button) findViewById(R.id.button_finish);
        button_close.setOnClickListener(this);
        button_finish.setOnClickListener(this);
        button_open.setOnClickListener(this);
        button_clear.setOnClickListener(this);
        OpenOrCloseView(true);
        imageView = (ImageView) findViewById(R.id.imageView);
    }
}
