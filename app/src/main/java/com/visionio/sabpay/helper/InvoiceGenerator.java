package com.visionio.sabpay.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.visionio.sabpay.R;
import com.visionio.sabpay.models.Order;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class InvoiceGenerator {

    // instance variables
    private String file_name;
    private Order order;
    private Context context;
    private File file;
    private PdfDocument pdfDocument;
    private PdfDocument.Page page1;
    private Canvas canvas;

    public InvoiceGenerator(Order order, Context context) {
        this.order = order;
        this.context = context;
        init();
    }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    private void init(){
        pdfDocument = new PdfDocument();
        int page_width = 2480;
        int page_height = 3508;
        PdfDocument.PageInfo page1_info = new PdfDocument.PageInfo
                .Builder(page_width, page_height, 1).create();
        page1 = pdfDocument.startPage(page1_info);
        canvas = page1.getCanvas();

        // static variables
        file_name = order.getOrderId();
        String base_path = "/SabPay-Invoice/";
        String f = Environment.getExternalStorageDirectory().getAbsolutePath()+ base_path;
        File root = new File(f);
        if(!root.exists()){
            root.mkdirs();
        }
        file = new File(f+file_name);

    }
    public boolean generate(){
        writeStatic();
        writeDynamic();
        pdfDocument.finishPage(page1);
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        pdfDocument.close();
        return true;
    }

    private static String getShippingAddress(String address, String userName, String number){
        String[] words = address.split(" ");
        StringBuilder builder = new StringBuilder();
        builder.append(userName).append(" ").append(number).append(", ");
        int i = words.length/4;
        int curr_line = 1;
        for(String word: words){
            if (curr_line == i){
                curr_line = 0;
                builder.append(word).append("\n");
            }else {
                curr_line++;
                builder.append(word).append(" ");
            }
        }
        return builder.toString();
    }
    private String getPaidToAddress(String address, String shopName){
        String[] words = address.split(" ");
        StringBuilder builder = new StringBuilder();
        builder.append(shopName).append(", ");
        int i = words.length/4;
        int curr_line = 1;
        for(String word: words){
            if (curr_line == i){
                curr_line = 0;
                builder.append(word).append("\n");
            }else {
                curr_line++;
                builder.append(word).append(" ");
            }
        }
        return builder.toString();
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    private void writeDynamic(){
        String inv_id = String.format("# %s",order.getInvoiceId());
        String shipping_address = getShippingAddress(order.getAddress(), order.getUserName(), order.getPhone());
        String paid_to = getPaidToAddress(order.shopAddress, order.getFromInventoryName());
        String cost = String.format("₹ %.2f", order.getAmount());

        String oId = order.getOrderId();
        String totItems = String.format("%s",order.getItems().size());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm a");
        String date = simpleDateFormat.format(order.getTimestamp());

        writeText(POSITION.left_x, POSITION.invId_y, SIZE.invId, inv_id, context.getResources().getColor(R.color.light_grey));
        writeText(POSITION.getX_69(cost.length()), POSITION.orderHeader_y, SIZE.ordDet, cost);
        writeMultilineText(POSITION.left_x, POSITION.shipAddVal_y, SIZE.extra_val, 50, shipping_address);
        writeMultilineText(POSITION.left_x, POSITION.paidVal_y, SIZE.extra_val, 50, paid_to);

        writeText(POSITION.ordIdVal_x, POSITION.ordId_y, SIZE.details, oId);
        writeText(POSITION.getX_41(totItems.length()), POSITION.totItems_y, SIZE.details, totItems);
        writeText(POSITION.dateVal_x, POSITION.date_y, SIZE.details, date);
    }
    private void writeStatic(){
        String f1 = "This is system generated invoice which doesn’t require signature.";
        String f2 = "For any query write us at beta.visionio@gmail.com";
        String oId = "Order Id";
        String totItems = "TotalItems";
        String date = "Date";
        writeBorder();
        writeImage();
        writeBoldText(POSITION.left_x, POSITION.title_y, SIZE.title_ty, "INVOICE");
        writeBoldText(POSITION.thank_x, POSITION.thank_y, SIZE.title_ty, "THANK YOU");
        writeText(POSITION.left_x, POSITION.orderHeader_y, SIZE.ordDet, "Order Details");
        writeText(POSITION.left_x, POSITION.shipAdd_y, SIZE.extra_header, "Shipping Address");
        writeText(POSITION.left_x, POSITION.paid_y, SIZE.extra_header, "Paid To");

        writeText(POSITION.left_x, POSITION.ordId_y, SIZE.details, oId);
        writeText(POSITION.left_x, POSITION.totItems_y, SIZE.details, totItems);
        writeText(POSITION.left_x, POSITION.date_y, SIZE.details, date);

        writeText(POSITION.footer1_x, POSITION.footer1_y, SIZE.footer_text, f1, context.getResources().getColor(R.color.light_grey));
        writeText(POSITION.footer2_x, POSITION.footer2_y, SIZE.footer_text, f2, context.getResources().getColor(R.color.light_grey));
        writeLine(POSITION.left_x, POSITION.line1_y, SIZE.line_width, context.getResources().getColor(R.color.colorPrimary));
        writeLine(POSITION.line2_x, POSITION.line2_y, SIZE.line_width, Color.BLACK);
    }

    private void writeText(int x, int y, float size, String text){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(size);
        canvas.drawText(text, x, y, paint);
    }
    private void writeBoldText(int x, int y, float size, String text){
        Paint paint = new Paint();
        Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        paint.setTypeface(typeface);
        paint.setColor(Color.BLACK);
        paint.setTextSize(size);
        canvas.drawText(text, x, y, paint);
    }
    private void writeText(int x, int y, float size, String text, int color){
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(size);
        canvas.drawText(text, x, y, paint);
    }
    private void writeMultilineText(int x, int y, float size, int offset, String text){
        String[] lines = text.split("\n");
        for (int i=0; i<lines.length; i++){
            writeText(x, y+i*offset, size, lines[i]);
        }
    }
    private void writeImage(){
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_playstore);
        Bitmap sc_bm = Bitmap.createScaledBitmap(bm, 690, 690, false);
        canvas.drawBitmap(sc_bm, POSITION.img_x, POSITION.img_y, null);
    }
    private void writeBorder(){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(SIZE.stroke_width);
        paint.setStyle(Paint.Style.STROKE);
        Rect rect = new Rect();
        rect.top = POSITION.rect_top;
        rect.left = POSITION.rect_left;
        rect.bottom = POSITION.rect_bottom;
        rect.right = POSITION.rect_right;
        canvas.drawRect(rect, paint);
    }
    private void writeLine(float x, float y, float width, int color){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(SIZE.line_size);
        canvas.drawLine(x, y, x+width, y, paint);
    }

    private static class POSITION{
        static int left_x = 220;
        private static int title_inv_id_sep = 100;
        static int title_y = 471;
        static int invId_y = title_y+title_inv_id_sep;
        static int img_x = 1513, img_y = 78;
        static int orderHeader_y = 844;
        static int line1_y = 932;
        static int ordId_y = 1097, ordIdVal_x = 1641;
        static int totItems_y = 1244;
        static int date_y = 1391, dateVal_x = 1520;
        private static int details_header_sep = 80;
        private static int ship_and_paid_sep = 500;
        static int shipAdd_y = 1794, shipAddVal_y = shipAdd_y + details_header_sep;
        static int paid_y = 1794 + ship_and_paid_sep, paidVal_y = paid_y + details_header_sep;
        static int thank_x = 1409, thank_y = 1962;
        static int line2_x = 1409, line2_y = 2032;
        static int rect_top = 72, rect_left = 72, rect_right = 2408, rect_bottom = 3218;
        static int footer1_x = 677, footer1_y = 3310;
        static int footer2_x = 803, footer2_y = 3360;
        static int getX_69(int len){
            int end = 2203;
            return end-((len-1)*69);
        }
        static int getX_41(int len){
            int end = 2203;
            return end-((len)*41);
        }
    }
    private static class SIZE {
        static float title_ty = 143;
        static float invId = 62;
        static float ordDet = 124;
        static float details = 74;
        static float extra_header = 80;
        static float extra_val = 44;
        static float footer_text = 38;
        static float stroke_width = 5;
        static float line_width = 814;
        static float line_size = 10;
    }
}
