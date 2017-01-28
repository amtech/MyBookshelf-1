package com.smartjinyu.mybookshelf;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.zxing.Result;

import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by smartjinyu on 2017/1/20.
 * Scan barcode of a single book
 */

public class SingleAddScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{
    private static final String TAG = "SingleAddScanActivity";

    private static final int CAMERA_PERMISSION = 1;


    private static final String FLASH_STATE = "FLASH_STATE";
    private ZXingScannerView mScannerView;
    private boolean mFlash;
    private Toolbar mToolbar;

    public static Intent newIntent(Context context){
        /** startMode is a number of 0,1,2
         * 0: start without a camera
         * 1: start with camera in single book mode
         * 2: start with camera in batch mode
         */
        Intent intent = new Intent(context,SingleAddScanActivity.class);
        return intent;
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }

        if(savedInstanceState != null) {
            mFlash = savedInstanceState.getBoolean(FLASH_STATE, false);
        } else {
            mFlash = false;
        }
        setContentView(R.layout.activity_single_add_scan);

        mToolbar = (Toolbar) findViewById(R.id.singleScanToolbar);
        mToolbar.setTitle(R.string.single_scan_toolbar);
        setSupportActionBar(mToolbar);
        final ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.singleScanFrame);
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.setAutoFocus(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuItem menuItem;

        if(mFlash) {
            menuItem = menu.add(Menu.NONE, R.id.menu_simple_add_flash, 0, R.string.menu_single_add_flash_on);
            menuItem.setIcon(R.drawable.ic_flash_on);
        } else {
            menuItem = menu.add(Menu.NONE, R.id.menu_simple_add_flash, 0, R.string.menu_single_add_flash_off);
            menuItem.setIcon(R.drawable.ic_flash_off);
        }

        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menuItem = menu.add(Menu.NONE,R.id.menu_simple_add_manually,0,R.string.menu_single_add_manually);
        MenuItemCompat.setShowAsAction(menuItem,MenuItemCompat.SHOW_AS_ACTION_NEVER);




        return super.onCreateOptionsMenu(menu);

    }
    public void resumeCamera(){
        //mScannerView.resumeCameraPreview(SingleAddScanActivity.this);
        mScannerView.setResultHandler(this);
        mScannerView.setAutoFocus(true);
        mScannerView.setFlash(mFlash);
        mScannerView.startCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.setAutoFocus(true);
        mScannerView.setFlash(mFlash);
        mScannerView.startCamera();

    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_simple_add_flash:
                mFlash = !mFlash;
                if(mFlash) {
                    item.setTitle(R.string.menu_single_add_flash_on);
                    item.setIcon(R.drawable.ic_flash_on);
                } else {
                    item.setTitle(R.string.menu_single_add_flash_off);
                    item.setIcon(R.drawable.ic_flash_off);}
                mScannerView.setFlash(mFlash);
                return true;
            case R.id.menu_simple_add_manually:
                mScannerView.stopCamera();
                MaterialDialog dialog = new MaterialDialog.Builder(this)
                        .title(R.string.input_isbn_manually_title)
                        .content(R.string.input_isbn_manually_content)
                        .positiveText(R.string.input_isbn_manually_positive)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                addBook(dialog.getInputEditText().getText().toString());
                            }
                        })
                        .negativeText(android.R.string.cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                resumeCamera();
                            }
                        })
                        .alwaysCallInputCallback()
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input(R.string.input_isbn_manually_edit_text,0, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                int length = dialog.getInputEditText().getText().length();
                                if(length == 10 || length == 13){
                                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                }else{
                                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                                }
                            }
                        })
                        .show();

        }
        return super.onOptionsItemSelected(item);
    }

    private void addBook(final String isbn){
        final Context context = this;
        mScannerView.stopCamera();
        BookLab bookLab = BookLab.get(this);
        List<Book> mBooks = bookLab.getBooks();
        boolean isExist = false;
        for(Book book:mBooks){
            if (book.getIsbn().equals(isbn)){
                isExist = true;
                break;
            }
        }

        if(isExist){//The book is already in the list
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.book_duplicate_dialog_title)
                    .content(R.string.book_duplicate_dialog_content)
                    .positiveText(R.string.book_duplicate_dialog_positive)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            DoubanFetcher fetcher = new DoubanFetcher();
                            fetcher.getBookInfo(context,isbn);
                        }
                    })
                    .negativeText(android.R.string.cancel)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show();
        }else{
            DoubanFetcher fetcher = new DoubanFetcher();
            fetcher.getBookInfo(this,isbn);
        }
    }



    @Override
    public void handleResult(Result rawResult){
        Log.i(TAG,"ScanResult Contents = " + rawResult.getText() + ", Format = " + rawResult.getBarcodeFormat().toString());
        addBook(rawResult.getText());


        // Note:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(SingleAddScanActivity.this);
            }
        }, 2000);
        */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this,R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                    Log.e(TAG,"Camera Permission Denied");
                    finish();
                }
        }
    }






}
