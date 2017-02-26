package com.spelder.particle;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;


/**
 * Created by Seth on 2/22/2017.
 */

public class ParticleView extends SurfaceView implements SurfaceHolder.Callback {

    SurfaceHolder mSurfaceHolder;
    DrawThread mThread;

    //Paint object used to define how the particles are drawn.
    Paint mPaint;
    Paint mAlphaPaint;
    boolean recalculateTextSize = true;
    String mText = "61°F";
    boolean doArrive = true;
    boolean doText = true;

    //Variables to deal with calculated points

    //list of points that are displayed on the screen
    ArrayList<Point> mRay;

    //width and height of the entire screen
    int mWidth, mHeight;

    //variables used for transformation
    int xOff, yOff;

    //variables used in touch events
    float touched_x, touched_y;
    boolean touched = false;

    boolean doGrid = false;
    Noise pGen;
    double zPerlOff;
    int gridWidth;
    int gridHeight;
    int cellWidth;
    int cellHeight;
    float[][] angles;
    float paintAngleOffset = 0;


    //region Constructors

    public ParticleView(Context context) {
        super(context);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mRay = new ArrayList<Point>();
    }

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mRay = new ArrayList<Point>();
    }

    public ParticleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mRay = new ArrayList<Point>();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ParticleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mRay = new ArrayList<Point>();
    }
    //endregion

    //region Callbacks

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawThread(mSurfaceHolder);
        mThread.setRunning(true);
        mThread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Instantiate the offsets for x and y coordinates for use with x
        mWidth = width;
        mHeight = height;
        mPaint = new Paint();
        //mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setStrokeWidth(10);
        mPaint.setColor(Color.WHITE);
        recalculateTextSize = true;

        mAlphaPaint = new Paint();
        mAlphaPaint.setColor(Color.BLACK);
        mAlphaPaint.setAlpha(1);

        createGrid();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        mThread.setRunning(false);
        while (retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {

            }
        }
    }
    //endregion

    //region Dot Word Functions

    public ArrayList<Point> calculateWord(String s, Canvas c, Rect r) {
        ArrayList<Point> cRay = new ArrayList<>();
        String displayString = s;
        mPaint.setTextSize(calculateFontSize(new Rect(), r, displayString, mPaint));
        Path path = new Path();
        mPaint.getTextPath(displayString, 0, displayString.length(), r.left, r.bottom, path);
        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pLength = pathMeasure.getLength();
        float dDist = 10;
        float distanceTraveled = 0;
        boolean nextCountour = true;
        while (distanceTraveled < pLength && nextCountour) {
            float[] coors = new float[2];
            pathMeasure.getPosTan(distanceTraveled, coors, null);
            Point poi = new Point(Math.round(coors[0]), Math.round(coors[1]));
            float xPos = map((float) Math.random(), 0, 1, 50, mWidth -50);
            float yPos = map((float) Math.random(), 0, 1, 50, mHeight -50);
            poi.setPosition(xPos, (yPos));
            cRay.add(poi);

            distanceTraveled += dDist;

            if (distanceTraveled >= pLength) {
                if ((nextCountour = pathMeasure.nextContour()) == true) {
                    pLength = pathMeasure.getLength();
                    distanceTraveled = 0;
                }
            }
        }
        return cRay;
    }
    private static float calculateFontSize(Rect textBounds, Rect textContainer, String text, Paint textPaint) {

        // Further optimize this method by passing in a reference of the Paint object
        // instead of instantiating it with every call.

        int stage = 1;
        float textSize = 0;

        while (stage < 3) {
            if (stage == 1) textSize += 10;
            else if (stage == 2) textSize -= 1;

            textPaint.setTextSize(textSize);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            textBounds.offsetTo(textContainer.left, textContainer.top);

            boolean fits = textContainer.contains(textBounds);
            if (stage == 1 && !fits) stage++;
            else if (stage == 2 && fits) stage++;
        }

        return Math.round(textSize*0.9);
    }
    //endregion

    public void createGrid()
    {
        gridWidth = 10;
        //do proportion to determine grid size
        //width/height = 10/x
        gridHeight = Math.round(((float)mHeight) * 10 / mWidth);
        cellWidth = mWidth/gridWidth;
        cellHeight = mHeight/gridHeight;
        zPerlOff = 0;
        angles = new float[gridWidth][gridHeight];
    }
    public void calculateGrid(Canvas c)
    {
//        c.drawLine(0, 0, mWidth, 0, mPaint);
//        c.drawLine(0, 0, 0, mHeight, mPaint);
        for(int i = 0; i < gridWidth; i++)
        {
            for(int j = 0; j < gridHeight; j++)
            {

                double pVal = pGen.noise(i * 0.07 , j * 0.07, zPerlOff);
                //Log.d("pVal",pVal + "");
                angles[i][j] = map((float) pVal, -1, 1, 0, 360*3);
//                c.save();
//                c.translate(cellWidth*(i+1), cellHeight*(j+1));
//                c.drawLine(-cellWidth, 0, 0, 0, mPaint);
//                c.drawLine(0, -cellHeight, 0, 0, mPaint);
//                c.translate(-cellWidth/2, -cellHeight/2);
//                c.rotate(angles[i][j]);
//                c.drawLine(-cellWidth/2 + 10, 0, cellWidth/2 -10, 0, mPaint);
//                c.translate(cellWidth/2 - 10, 0);
//                c.rotate(135);
//                c.drawLine(0, 0, 50, 0, mPaint);
//                c.rotate(-270);
//                c.drawLine(0, 0, 50, 0, mPaint);
//                c.restore();
                zPerlOff += 0.000025;
            }
        }
    }

    public void doDraw(Canvas c) {
        if (c != null) {
            c.drawPaint(mAlphaPaint);

//            if(doGrid)
//            {
                calculateGrid(c);
                paintAngleOffset += .3;
                paintAngleOffset = paintAngleOffset % 360;
//            }


            if(doText) {
                if (recalculateTextSize) {
                    Rect temp = new Rect(0, 0, mWidth, mHeight/4);
                    mRay.addAll(calculateWord("Weather is sunny", c, temp));
                    temp = new Rect(mWidth/2, mHeight/4, mWidth, mHeight/2);
                    mRay.addAll(calculateWord("61°F", c, temp));
                    recalculateTextSize = false;
                }


                for (Point point : mRay) {
                    point.draw(c);
                }
            }
        }
    }

    //region Helper Functions
    public void translate(int x, int y) {
        xOff = x;
        yOff = y;
    }


    public static float map(float n, float start1, float stop1, float start2, float stop2) {
        return ((n - start1) / (stop1 - start1)) * (stop2 - start2) + start2;
    }
    //endregion




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub

        touched_x = event.getX();
        touched_y = event.getY();

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touched = true;
                break;
            case MotionEvent.ACTION_MOVE:
                touched = true;
                break;
            case MotionEvent.ACTION_UP:
                touched = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                touched = false;
                break;
            case MotionEvent.ACTION_OUTSIDE:
                touched = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                synchronized (mSurfaceHolder) {
                    doArrive = doArrive ? false : true;
                    doGrid = !doArrive;
                }
                touched = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                touched = false;
                break;
            default:
        }
        return true; //processed
    }

    public class Point {
        float xCurPos, yCurPos;
        float xPos, yPos;
        float xVel, yVel;
        float xAcc, yAcc;
        float maxFleeSpeed = -15;
        float maxArriveSpeed = 5;
        float maxFieldSpeed = 4;

        public Point(float x, float y) {
            xPos = x;
            yPos = y;
            xCurPos = x;
            yCurPos = y;
            xVel = map((float) Math.random(), 0, 1, -5, 5);
            yVel = map((float) Math.random(), 0, 1, -5, 5);
            xAcc = 0;
            yAcc = 0;
        }

        public void setPosition(float newXPos, float newYPos) {
            xCurPos = newXPos;
            yCurPos = newYPos;
        }

        public void checkCollisions() {
            if (xCurPos < 0) {
                xCurPos = mWidth;
            }
            if (xCurPos > mWidth) {
                xCurPos = 0;
            }
            if (yCurPos < 0) {
                yCurPos = mHeight;
            }
            if (yCurPos > mHeight) {
                yCurPos = 0;
            }
        }

        public void update() {


            xVel += xAcc;
            yVel += yAcc;
            if(doGrid) {
                float[] lim = setMagnitude(xVel, yVel, maxFieldSpeed);
                xVel = lim[0];
                yVel = lim[1];
            }
            xCurPos += xVel;
            yCurPos += yVel;
            xAcc = 0;
            yAcc = 0;

        }

        public void gridForces()
        {
            int xIndex = (int) (xCurPos / cellWidth);
            int yIndex = (int) (yCurPos / cellHeight);
            if(xIndex >= angles.length)
                xIndex = angles.length - 1;
            if(yIndex >= angles[0].length)
                yIndex = angles[0].length - 1;
            float angle = angles[xIndex][yIndex];

            xAcc += Math.cos(Math.toRadians(angle)) * 0.1;
            yAcc += Math.sin(Math.toRadians(angle)) * 0.1;
        }

        public void draw(Canvas c)
        {
            checkCollisions();
            if (doArrive) {
                arrive();
                setPaint(xCurPos, yCurPos);
            }
            else if(doGrid)
            {
                gridForces();
                setPaint(xVel, yVel);

            }

            update();
            c.drawCircle(xCurPos, yCurPos, 10, mPaint);
        }

        public void setPaint(float x, float y)
        {
            double angle = Math.atan2(y, x);
            angle = Math.toDegrees(angle);
            angle = (angle + 360) + paintAngleOffset;
            angle = angle % 360;
            mPaint.setColor(Color.HSVToColor(new float[]{(float) angle, 1, 1}));
        }

        public void arrive() {
            float dX = xPos - xCurPos;
            float dY = yPos - yCurPos;
            float distance = (float) Math.sqrt(dX * dX + dY * dY);
            float speed = maxArriveSpeed;
            if (distance < 500) {
                speed = map(distance, 0, 500, 0, maxArriveSpeed);
            }
            float[] norm = setMagnitude(dX, dY, speed);
            dX = norm[0];
            dY = norm[1];
            float sX = dX - xVel;
            float sY = dY - yVel;
            xAcc += sX;
            yAcc += sY;
//            Log.d("xAcc", "" + xAcc);
//            Log.d("yAcc", "" + yAcc);
        }

        public void flee(float x, float y) {
            float dX = x - xCurPos;
            float dY = y - yCurPos;
            float distance = (float) Math.sqrt(dX * dX + dY * dY);
            float speed = maxFleeSpeed;
            speed = map(distance, 0, (float) Math.sqrt(mWidth * mWidth + mHeight * mHeight), maxFleeSpeed, 0);
            float[] norm = setMagnitude(dX, dY, speed);
            dX = norm[0];
            dY = norm[1];
            float sX = dX - xVel;
            float sY = dY - yVel;
            xAcc += sX;
            yAcc += sY;
//          Log.d("xAcc", "" + xAcc);
//          Log.d("yAcc", "" + yAcc);
        }

        public float[] setMagnitude(float x, float y, float newMag) {
            double mag = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            float[] temp = new float[2];
            if (mag == 0) {
                temp[0] = 0;
                temp[1] = 0;
            } else {
                temp[0] = (float) (x * newMag / mag);
                temp[1] = (float) (y * newMag / mag);
            }
            return temp;
        }
    }

    public class DrawThread extends Thread {
        SurfaceHolder surfaceHolder;
        boolean runFlag;

        public DrawThread(SurfaceHolder _sh) {
            surfaceHolder = _sh;
        }

        public void setRunning(boolean run) {
            runFlag = run;
        }

        @Override
        public void run() {
            Log.d("Thread", "Running");
            Canvas c;
            while (runFlag) {
                c = null;

                try {
                    c = surfaceHolder.lockCanvas();
                    synchronized (this.surfaceHolder) {
                        doDraw(c);
                    }
                } finally {
                    if (c != null)
                        this.surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }


    }
}
