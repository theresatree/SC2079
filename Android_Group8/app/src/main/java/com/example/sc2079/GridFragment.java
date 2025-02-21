package com.example.sc2079;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.TreeSet;

public class GridFragment extends Fragment {
    public static final String TAG = "GridFragment";
    private TextView textViewDirection;
    private CarLogAdapter carLogAdapter;
    private ButtonLogAdapter btnLogAdapter;

    private static final int ROWS = 20;
    private static final int COLS = 20;
    private boolean isSettingObstacle = false;
    private boolean isSettingCar = false;
    private boolean isDragging = false;
    private Button draggedObstacle;
    private boolean carSet = false;
    private float initialTouchX, initialTouchY;
    private int btnIDCounter=1;

    private int originalRow, originalCol;
    private float triangleRotation = 0f;
    private int obstacleCounter = 1;
    private TreeSet<Integer> removedObstacleNumbers = new TreeSet<>();// This is to help us check obstacle number.
    private GridLayout gridLayout;

    // Store the coordinates of the previously placed car's 3x3 grid
    private int[] lastCarCoordinates = null;

    // Intent used to send instructions to the rpi from the gridfragment
    Intent instructionToSend;
    //Context mContext;

    // Timer variables
    public static long exploreTimer = 0;
    public static Button exploreButton; // Used to toggle the timer to start/stop for task 1
    public boolean exploreStarted = false;
    public static TextView exploreTimerTextView; // Text display for task 1 timer

    public static long fastestTimer = 0;
    public static Button fastestButton; // Used to toggle the timer to start/stop for task 2
    public boolean fastestStarted = false;
    public static TextView fastestTimerTextView; // Text display for task 2 timer
    public static Handler timerHandler = new Handler();

    public GridFragment() {
        // Required empty public constructor
        //this.mContext = getActivity();
    }

    public static GridFragment newInstance(String param1, String param2) {
        GridFragment fragment = new GridFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisecondsExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisecondsExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;



        }
    };



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_grid, container, false);

        Button btnSetObstacle = root.findViewById(R.id.btnSetObstacle);
        Button btnSetCar = root.findViewById(R.id.btnSetCar);

        Button btnUp = root.findViewById(R.id.btnUp);
        Button btnDown = root.findViewById(R.id.btnDown);
        Button btnRight = root.findViewById(R.id.btnRight);
        Button btnLeft = root.findViewById(R.id.btnLeft);

        RecyclerView rvBtnLog = root.findViewById(R.id.rvBtnLog);
        RecyclerView rvCarLog = root.findViewById(R.id.rvCarLog);
        ArrayList<String> carLogs = new ArrayList<>();
        carLogAdapter = new CarLogAdapter(carLogs, rvCarLog);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        rvCarLog.setLayoutManager(layoutManager);
        rvCarLog.setAdapter(carLogAdapter);

        textViewDirection = root.findViewById(R.id.textViewDirection);

        ArrayList<String> btnLogs = new ArrayList<>();
        btnLogAdapter = new ButtonLogAdapter(btnLogs, rvBtnLog);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
        layoutManager2.setReverseLayout(true);
        layoutManager2.setStackFromEnd(true);
        rvBtnLog.setLayoutManager(layoutManager2);
        rvBtnLog.setAdapter(btnLogAdapter);


        btnUp.setOnClickListener(v -> {
            if (carSet) {
                moveCar(1);
            }
        });
        btnDown.setOnClickListener(v -> {
            if (carSet) {
                moveCar(-1);
            }
        });

        btnLeft.setOnClickListener(v -> {
            if (carSet) {
                rotateCar(-1);
            }
        });
        btnRight.setOnClickListener(v -> {
            if (carSet) {
                rotateCar(1);
            }
        });

        gridLayout = root.findViewById(R.id.gridLayout);
        gridLayout.setColumnCount(COLS);
        gridLayout.setRowCount(ROWS);
        gridLayout.setUseDefaultMargins(false);

        populateGrid();

        int cellSize = 22;
        int widthInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);

        LinearLayout columnText = root.findViewById(R.id.columnText);
        LinearLayout rowText = root.findViewById(R.id.rowText);

        for (int i = 0; i <= COLS-1; i++) {
            TextView textView = new TextView(getContext());
            textView.setText(String.valueOf(i));
            // Set layout parameters to ensure horizontal orientation
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    widthInPixels,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 2,0, 0);  // Add some space between TextViews
            textView.setLayoutParams(params);
            textView.setGravity(Gravity.CENTER);
            // Add TextView to the LinearLayout
            columnText.addView(textView);
        }

        for (int i = ROWS-1; i >=0; i--) {
            TextView textView = new TextView(getContext());
            textView.setText(String.valueOf(i));
            // Set layout parameters to ensure horizontal orientation
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    widthInPixels
            );
            params.setMargins(10, 0,0, 0);  // Add some space between TextViews
            textView.setLayoutParams(params);
            textView.setGravity(Gravity.CENTER);
            // Add TextView to the LinearLayout
            rowText.addView(textView);
        }

        // Toggle obstacle mode when button is clicked
        btnSetObstacle.setOnClickListener(v -> {
            isSettingCar = false;
            isSettingObstacle = !isSettingObstacle; // Toggle mode
            String status = isSettingObstacle ? "Obstacle mode ON" : "Obstacle mode OFF";
            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
        });

        // Toggle car placement mode when button is clicked
        btnSetCar.setOnClickListener(v -> {
            isSettingObstacle = false;
            isSettingCar = !isSettingCar; // Toggle mode
            String status = isSettingCar ? "Placement mode ON" : "Placement mode OFF";
            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
        });

        // ***** TO DO *****
        // Toggle timer for task 1 (Explore Task)
        exploreButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // Show a popup to tell us that Task 1 timer has started
                Toast.makeText(getContext(), "Task 1 Started", Toast.LENGTH_SHORT).show();

                exploreTimer = System.currentTimeMillis(); // Start time of the task
                timerHandler.postDelayed(timerRunnableExplore, 0); // start updating the timer
            }
        });

        // Toggle timer for task 2 (Fastest Task)


        return root;
    }

    private void populateGrid() {
        // Create buttons grid - starting from bottom-left
        for (int row = ROWS - 1; row >= 0; row--) {
            for (int col = 0; col < COLS; col++) {
                Button button = new Button(getContext());

                // Set fixed size for the button
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                int cellSize = 22;
                int widthInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);
                int heightInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);
                params.width = widthInPixels;
                params.height = heightInPixels;
                params.setMargins(0, 0, 0, 0);

                params.rowSpec = GridLayout.spec(ROWS - 1 - row);
                params.columnSpec = GridLayout.spec(col);

                button.setLayoutParams(params);
                button.setBackgroundResource(R.drawable.button_border_color);
                button.setPadding(0, 0, 0, 0);

                // Ensure the text is always centered vertically and horizontally
                button.setGravity(Gravity.CENTER); // Center both vertically and horizontally
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8); // Smaller text size to prevent overflow
                button.setTextColor(getResources().getColor(android.R.color.white));

                // Set a small margin and padding to keep text within bounds
                button.setPadding(0, 0, 0, 0);  // Remove extra padding around text

                final int finalRow = row;
                final int finalCol = col;

                button.setTag(new int[]{finalRow, finalCol, 0, 0, -1, 0});
                // [0] = row
                // [1] = col
                // [2] = status
                // [3] = direction
                // [4] = id
                // [5] = text

                button.setOnClickListener(v -> {
                    if (isSettingObstacle && !isDragging) {
                        int[] coords = (int[]) button.getTag();
                        if (!canPlaceObstacle(coords)) {
                            Toast.makeText(getContext(), "Can't place obstacle here", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int currentRow = coords[0];
                        int currentCol = coords[1];
                        int obstaclePlaced = coords[2];
                        int obstacleDirection = coords[3];

                        if (obstaclePlaced == 0) {
                            // Add obstacle
                            button.setBackground(getResources().getDrawable(R.drawable.custom_obstacle_color));
                            // Use a removed obstacle number if available, otherwise use a new one
                            int obstacleNumber;
                            if (!removedObstacleNumbers.isEmpty()) {
                                obstacleNumber = removedObstacleNumbers.pollFirst(); // Reuse removed obstacle number
                            } else {
                                obstacleNumber = obstacleCounter++; // Use the next available number
                            }

                            // Set the text for the obstacle (ensure it fits within the button without causing layout issues)
                            button.setText(String.valueOf(obstacleNumber));
                            button.setMaxLines(1); // Keep the text in one line to avoid overflow
                            button.setEllipsize(TextUtils.TruncateAt.END); // Ensure no overflow

                            // Keep the text vertically centered and prevent shifting
                            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

                            // Update the button's tag to indicate an obstacle is placed
                            button.setTag(new int[]{currentRow, currentCol, 1, obstacleDirection, btnIDCounter, obstacleNumber}); // Flag set to 1 (obstacle placed)
                            btnLogAdapter.addLog(String.format("Set ID:%d Text: %d [%d,%d]", btnIDCounter, obstacleNumber, currentCol, currentRow));
                            sendObstacleUpdate(button);
                            btnIDCounter++;
                        }
                    } else if (isSettingCar) {
                        int[] coords = (int[]) button.getTag();
                        if (!canPlaceCar(coords)) {
                            Toast.makeText(getContext(), "Can't place car here (obstacle present)", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!carSet){
                            updateTextView();
                        }

                        carSet = true;

                        // If there was a previous car placement, reset it
                        if (lastCarCoordinates != null) {
                            resetCarPlacement(lastCarCoordinates);

                        }

                        switch (Math.floorMod(Math.round(triangleRotation), 360)) {
                            case 0: // North
                                instructionToSend = new Intent("SendInstruction");
                                instructionToSend.putExtra("type", "MOVE-CAR");
                                instructionToSend.putExtra("direction", "NORTH");
                                instructionToSend.putExtra("carXAxis", String.valueOf(coords[1]));
                                instructionToSend.putExtra("carYAxis", String.valueOf(coords[0]));
                                // TO ADD X AND Y COORDS INTO THE JSON TO SEND ALSO
                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                                break;
                            case 90: // East
                                instructionToSend = new Intent("SendInstruction");
                                instructionToSend.putExtra("type", "MOVE-CAR");
                                instructionToSend.putExtra("direction", "EAST");
                                instructionToSend.putExtra("carXAxis", String.valueOf(coords[1]));
                                instructionToSend.putExtra("carYAxis", String.valueOf(coords[0]));
                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                                break;
                            case 180: // South
                                instructionToSend = new Intent("SendInstruction");
                                instructionToSend.putExtra("type", "MOVE-CAR");
                                instructionToSend.putExtra("direction", "SOUTH");
                                instructionToSend.putExtra("carXAxis", String.valueOf(coords[1]));
                                instructionToSend.putExtra("carYAxis", String.valueOf(coords[0]));
                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                                break;
                            case 270: // West
                                instructionToSend = new Intent("SendInstruction");
                                instructionToSend.putExtra("type", "MOVE-CAR");
                                instructionToSend.putExtra("direction", "WEST");
                                instructionToSend.putExtra("carXAxis", String.valueOf(coords[1]));
                                instructionToSend.putExtra("carYAxis", String.valueOf(coords[0]));
                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                                break;
                        }

                        // Get the actual coordinates from the button's tag
                        int currentRow = coords[0];
                        int currentCol = coords[1];

                        // Determine where to place the 3x3 grid
                        int startRow = currentRow - 1;  // Top to bottom
                        int startCol = currentCol - 1;  // Left to right

                        // Ensure the grid does not go out of bounds (row and column check)
                        if (startRow < 0) startRow = 0;
                        if (startCol < 0) startCol = 0;

                        // Check if the 3x3 grid can fit within the bounds of the grid layout
                        if (startRow + 3 > ROWS) {
                            startRow = ROWS - 3;  // Make sure the grid stays within bounds
                        }
                        if (startCol + 3 > COLS) {
                            startCol = COLS - 3;  // Make sure the grid stays within bounds
                        }

                        // Loop through a 3x3 area around the clicked cell and color the buttons
                        for (int r = 0; r < 3; r++) {
                            for (int c = 0; c < 3; c++) {
                                int targetRow = startRow + r;
                                int targetCol = startCol + c;

                                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                                    View child = gridLayout.getChildAt(i);
                                    if (child instanceof Button) {
                                        int[] buttonCoords = (int[]) child.getTag();
                                        if (buttonCoords != null &&
                                                buttonCoords[0] == targetRow &&
                                                buttonCoords[1] == targetCol) {

                                            if (r == 1 && c == 1) { // Center cell
                                                // 1. Create the orange background
                                                ShapeDrawable orangeBackground = new ShapeDrawable();
                                                orangeBackground.getPaint().setColor(getResources().getColor(android.R.color.holo_orange_light));

                                                // 2. Create the triangle drawable
                                                ShapeDrawable triangle = createTriangleDrawable(child.getWidth(), child.getHeight());

                                                // 3. Create a LayerDrawable to combine them
                                                Drawable[] layers = new Drawable[]{orangeBackground, triangle};
                                                LayerDrawable layerDrawable = new LayerDrawable(layers);

                                                child.setBackground(layerDrawable); // Set the LayerDrawable as the background
                                                child.setPadding(0, 0, 0, 0); // No padding for the combined drawable
                                            } else { // Other cells
                                                child.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));

                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Store the current car placement coordinates
                        lastCarCoordinates = new int[]{currentRow, currentCol};

                        carLogAdapter.addLog(String.format("Car placed at [%d,%d]", lastCarCoordinates[0], lastCarCoordinates[1]));

                    } else {
                        Toast.makeText(getContext(),
                                "Clicked on cell (" + finalCol + ", " + finalRow + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                button.setOnTouchListener((v, event) -> {
                    if (isSettingObstacle) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                // Start dragging if this is an obstacle
                                if (isObstacle(button)) {
                                    startDragging(button, event.getRawX(), event.getRawY());
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (isDragging) {
                                    updateDraggingPosition(event.getRawX(), event.getRawY());
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                if (isDragging) {
                                    finishDragging(event.getRawX(), event.getRawY());
                                    return true;
                                }
                                break;
                        }
                    }
                    return false;
                });
                gridLayout.addView(button);
            }
        }
    }

    private void updateTextView() {
        if (textViewDirection != null) {
            String message;
            int roundedRotation = Math.floorMod(Math.round(triangleRotation), 360);

            switch (roundedRotation) {
                case 0:
                    message = "NORTH";
                    break;
                case 90:
                    message = "EAST";
                    break;
                case 180:
                    message = "SOUTH";
                    break;
                case 270:
                    message = "WEST";
                    break;
                default:
                    message = "Unknown";
                    break;
            }
            carLogAdapter.addLog(String.format("Car facing %s", message));
            String rotationMessage = String.format("Direction: %s", message);
            textViewDirection.setText(rotationMessage);  // Set the text
        }
    }


    private ShapeDrawable createTriangleDrawable(int width, int height) {
        Shape triangleShape = new Shape() {
            @Override
            public void draw(Canvas canvas, Paint paint) {
                canvas.save();
                canvas.rotate(triangleRotation, width / 2f, height / 2f);

                Path path = new Path();
                path.moveTo(width / 2f, 0f);
                path.lineTo(width, height);
                path.lineTo(0f, height);
                path.close();
                canvas.drawPath(path, paint);

                canvas.restore();
            }
        };

        ShapeDrawable drawable = new ShapeDrawable(triangleShape);
        drawable.setIntrinsicWidth(width);
        drawable.setIntrinsicHeight(height);
        drawable.getPaint().setColor(getResources().getColor(android.R.color.holo_blue_bright));

        return drawable;
    }

    private void updateTriangleRotation() {
        // Get the exact coordinates of the center of the 3x3 grid
        int targetRow = lastCarCoordinates[0];
        int targetCol = lastCarCoordinates[1];

        // Find the button at the exact target position
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof Button) {
                int[] buttonCoords = (int[]) child.getTag();
                if (buttonCoords != null &&
                        buttonCoords[0] == targetRow &&
                        buttonCoords[1] == targetCol) {

                    // Create the background and triangle for the center cell
                    ShapeDrawable orangeBackground = new ShapeDrawable();
                    orangeBackground.getPaint().setColor(getResources().getColor(android.R.color.holo_orange_light));

                    // Create the triangle, ensuring it fits within the current cell
                    ShapeDrawable triangle = createTriangleDrawable(child.getWidth(), child.getHeight());

                    // Set both the background and the triangle as a layered drawable
                    Drawable[] layers = new Drawable[]{orangeBackground, triangle};
                    LayerDrawable layerDrawable = new LayerDrawable(layers);

                    // Set the layered drawable as the background for the button
                    child.setBackground(layerDrawable);
                    child.setPadding(0, 0, 0, 0);
                }
            }
        }
    }


    private void startDragging(Button obstacle, float touchX, float touchY) {
        isDragging = true;
        draggedObstacle = obstacle;
        initialTouchX = touchX;
        initialTouchY = touchY;

        // Store original position
        int[] coords = (int[]) obstacle.getTag();
        originalRow = coords[0];
        originalCol = coords[1];

        // Visual feedback
        obstacle.setAlpha(0.5f);
    }

    private void updateDraggingPosition(float currentX, float currentY) {
        if (draggedObstacle != null) {
            // Update visual position (you'll need a custom view for proper dragging)
            float dx = currentX - initialTouchX;
            float dy = currentY - initialTouchY;
            draggedObstacle.setTranslationX(dx);
            draggedObstacle.setTranslationY(dy);
        }
    }

    private void finishDragging(float endX, float endY) {
        isDragging = false;
        draggedObstacle.setAlpha(1f);
        draggedObstacle.setTranslationX(0);
        draggedObstacle.setTranslationY(0);

        View newParent = findViewAtPosition(endX, endY);
        Button newButton = (Button) newParent;

        if (newParent != null) {
            int[] originalCoords = (int[]) draggedObstacle.getTag();
            int[] newCoords = (int[]) newButton.getTag();

            if (originalCoords[0] == newCoords[0] && originalCoords[1] == newCoords[1]) {
                // Same coordinates: ROTATE
                int rotation = newCoords[3];
                rotation = (rotation +1) %4;
                newButton.setTag(new int[]{originalCoords[0], originalCoords[1], 1, rotation, originalCoords[4],originalCoords[5]});
                String message="";
                switch (rotation) {
                    case 0: // North
                        message = "NORTH";
                        break;
                    case 1: // East
                        message = "EAST";
                        break;
                    case 2: // South
                        message = "SOUTH";
                        break;
                    case 3: // West
                        message = "WEST";
                        break;
                }
                btnLogAdapter.addLog(String.format("Rotate ID:%d to %s", originalCoords[4], message));
                rotateObstacle(newButton,rotation);
                sendObstacleUpdate(newButton);

            } else {
                // Different coordinates: MOVE
                if (!isColliding(newButton)) {
                    moveObstacleTo(newButton);
                    rotateObstacle(newButton,originalCoords[3]);
                } else {
                    Toast.makeText(getContext(), "Collision detected! Cannot place obstacle here.", Toast.LENGTH_SHORT).show();
                    draggedObstacle.setBackground(getResources().getDrawable(R.drawable.custom_obstacle_color)); // Reset to original state
                }
            }
        } else {
            // Dropped outside grid: REMOVE
            int[] originalCoords = (int[]) draggedObstacle.getTag();
            removeObstacle(originalCoords);
        }

        draggedObstacle = null;
    }

    private boolean isColliding(Button newButton) {
        int[] newCoords = (int[]) newButton.getTag();
        int newRow = newCoords[0];
        int newCol = newCoords[1];
        int obstaclePlaced = newCoords[2];


                // Skip if out of bounds
                if (newRow < 0 || newRow >= ROWS || newCol < 0 || newCol >= COLS) {
                    return true;
                }

                // Find the button at the target position
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == newRow &&
                                buttonCoords[1] == newCol) {

                            // Check if the button is an obstacle or part of the car
                            Drawable background = child.getBackground();
                            if (background instanceof ColorDrawable) {
                                int color = ((ColorDrawable) background).getColor();
                                if (1 == obstaclePlaced || // Obstacle
                                        color == getResources().getColor(android.R.color.holo_orange_light)) { // Car
                                    return true; // Collision detected
                                }
                            }
                            else if (buttonCoords[2]==1){
                                return true;
                            }
                        }
                    }
                }



        return false; // No collision detected
    }

    private View findViewAtPosition(float x, float y) {
        // Convert screen coordinates to grid coordinates
        int[] location = new int[2];
        gridLayout.getLocationOnScreen(location);
        int gridX = (int) (x - location[0]);
        int gridY = (int) (y - location[1]);

        // Iterate through all child views in the grid
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                // Get the bounds of the child view
                int[] childLocation = new int[2];
                child.getLocationOnScreen(childLocation);
                int left = childLocation[0];
                int top = childLocation[1];
                int right = left + child.getWidth();
                int bottom = top + child.getHeight();

                // Check if the touch coordinates are within the child's bounds
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    return child;
                }
            }
        }
        return null; // No view found at the given position
    }

    private boolean isObstacle(Button button) {
        int[] coords = (int[]) button.getTag();
        int obstaclePlaced = coords[2];
        return obstaclePlaced == 1;
    }

    private void moveObstacleTo(Button newButton) {
        // Clear original position
        String obstacleNumber = draggedObstacle.getText().toString();
        draggedObstacle.setBackgroundResource(R.drawable.button_border_color);
        draggedObstacle.setText("");

        // Preserve original text size before moving
        float originalTextSizePx = draggedObstacle.getTextSize(); // Get text size in PX
        float originalTextSizeSp = originalTextSizePx / draggedObstacle.getResources().getDisplayMetrics().scaledDensity; // Convert to SP

        // Set new button properties
        newButton.setBackground(getResources().getDrawable(R.drawable.custom_obstacle_color));
        newButton.setTextColor(getResources().getColor(android.R.color.white));

        // Apply the same text size as the original button
        newButton.post(() -> {
            // Preserve the original width and height of the button
            int fixedWidth = newButton.getWidth();
            int fixedHeight = newButton.getHeight();

            // Set the text and apply the correct text size
            newButton.setText(obstacleNumber);

            if (originalTextSizeSp == 8f) {
                // For 8sp, use custom layout for the button
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                int cellSize = 22; // You can adjust this size if necessary
                int widthInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);
                int heightInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);

                params.width = widthInPixels;
                params.height = heightInPixels;
                params.setMargins(0, 0, 0, 0);

                newButton.setLayoutParams(params);
                newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8); // Smaller text size for 8sp

                newButton.setPadding(0, 0, 0, 0); // Set padding to 0 for no overflow
                newButton.setGravity(Gravity.CENTER); // Center the text vertically and horizontally
            } else if (originalTextSizeSp == 11f) {
                // For 11sp, keep the layout as before
                newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11); // Apply 11sp text size

                // Lock size to prevent resizing
                ViewGroup.LayoutParams params = newButton.getLayoutParams();
                params.width = fixedWidth;
                params.height = fixedHeight; // Keep the original fixed height
                newButton.setLayoutParams(params);

                // Prevent shrinking of button by setting the minimum size
                newButton.setMinimumHeight(fixedHeight);  // Prevent shrinking height
                newButton.setMinimumWidth(fixedWidth);   // Prevent shrinking width

                newButton.setIncludeFontPadding(false); // Ensure font padding doesn't cause overflow
                newButton.setGravity(Gravity.CENTER); // Center the text
            }

            // Force layout recalculation
            newButton.requestLayout(); // Forces a layout pass
            newButton.invalidate(); // Ensures a fresh redraw after layout changes
        });

        // Update button position tags
        int[] originalCoords = (int[]) draggedObstacle.getTag();
        draggedObstacle.setTag(new int[]{originalCoords[0], originalCoords[1], 0, originalCoords[3], originalCoords[4], originalCoords[5]}); // Clear old position
        int[] newCoords = (int[]) newButton.getTag();
        newButton.setTag(new int[]{newCoords[0], newCoords[1], 1, originalCoords[3], originalCoords[4], originalCoords[5]}); // Set new position

        btnLogAdapter.addLog(String.format("Move ID:%d [%d,%d]>[%d,%d]",
                originalCoords[4], originalCoords[1], originalCoords[0], newCoords[1], newCoords[0]));

        // Send Bluetooth update
        sendObstacleUpdate(newButton);
    }

    private void removeObstacle(int[] originalCoords){
        String obstacleNumberStr = draggedObstacle.getText().toString();
        int obstacleNumber = Integer.parseInt(obstacleNumberStr);

        // Add the removed obstacle number to the TreeSet
        removedObstacleNumbers.add(obstacleNumber);

        draggedObstacle.setBackgroundResource(R.drawable.button_border_color);
        draggedObstacle.setText("");
        draggedObstacle.setTag(new int[]{-1, -1, 0, originalCoords[3], originalCoords[4], originalCoords[5]});
        btnLogAdapter.addLog(String.format("Remove ID:%d [%d,%d]", originalCoords[4], originalCoords[1], originalCoords[0]));

        sendObstacleUpdate(draggedObstacle);
    }

    private void rotateObstacle(Button button, int rotation) {
        Drawable background = button.getBackground();
        LayerDrawable layerDrawable;


        // If it's the first rotation, it will be a LayerDrawable
        // If it's already been rotated, it will be a RotateDrawable
        if (background instanceof LayerDrawable) {
            layerDrawable = (LayerDrawable) background;
        } else if (background instanceof RotateDrawable) {
            // If it's already a RotateDrawable, we just update the rotation
            RotateDrawable rotateDrawable = (RotateDrawable) background;
            rotateDrawable.setLevel((int)((rotation * 90f / 360f) * 10000));
            return;
        } else {
            return; // Invalid drawable type
        }

        // Create new RotateDrawable for first rotation
        RotateDrawable rotateDrawable = new RotateDrawable();
        rotateDrawable.setDrawable(layerDrawable);
        rotateDrawable.setFromDegrees(0);
        rotateDrawable.setToDegrees(360);
        rotateDrawable.setLevel((int)((rotation * 90f / 360f) * 10000));

        button.setBackground(rotateDrawable);
    }


    private void sendObstacleUpdate(Button button) {
        int[] data = (int[]) button.getTag();
        int currentRow = data[0];
        int currentCol = data[1];
        int obstaclePlaced = data[2];
        int obstacleDirection = data[3];
        int obstacleID = data[4];
        int obstacleText = data[5];

        Intent intent = new Intent("SendInstruction");  // Set the action to "OBSTACLE"

        // Add the data to the intent
        intent.putExtra("type", "OBSTACLE");  // Set type to OBSTACLE
        intent.putExtra("obstacleID", String.valueOf(obstacleID));
        intent.putExtra("obstacleXAxis", String.valueOf(currentCol));
        intent.putExtra("obstacleYAxis", String.valueOf(currentRow));
        intent.putExtra("obstacleText", String.valueOf(obstacleText));
        intent.putExtra("obstacleDirection", String.valueOf(obstacleDirection));
        intent.putExtra("obstacleStatus", String.valueOf(obstaclePlaced));

        // Send the broadcast
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }


    // Method to reset the previous car's 3x3 grid
    private void resetCarPlacement(int[] carCoordinates) {
        int row = carCoordinates[0];
        int col = carCoordinates[1];

        // Calculate the starting row and column for the 3x3 grid
        int startRow = row - 1;
        int startCol = col - 1;

        // Ensure the grid does not go out of bounds (row and column check)
        if (startRow < 0) startRow = 0;
        if (startCol < 0) startCol = 0;

        // Check if the 3x3 grid can fit within the bounds of the grid layout
        if (startRow + 3 > ROWS) {
            startRow = ROWS - 3;  // Make sure the grid stays within bounds
        }
        if (startCol + 3 > COLS) {
            startCol = COLS - 3;  // Make sure the grid stays within bounds
        }

        // Loop through the previous 3x3 area and reset the buttons' background
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int targetRow = startRow + r;
                int targetCol = startCol + c;

                // Skip if out of bounds
                if (targetRow < 0 || targetRow >= ROWS || targetCol < 0 || targetCol >= COLS) {
                    continue;
                }

                // Reset the button's background
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == targetRow &&
                                buttonCoords[1] == targetCol) {
                            child.setBackgroundResource(R.drawable.button_border_color); // Reset background
                        }
                    }
                }
            }
        }
    }

    // Method to check if the car can be placed at the given coordinates
    private boolean canPlaceCar(int[] carCoordinates) {
        int row = carCoordinates[0];
        int col = carCoordinates[1];

        if (row <= 0 || col <= 0 || row >= ROWS - 1 || col >= COLS - 1) {
            return false; // Can't place car near the edge of the grid (3x3 area won't fit)
        }
        // Check if the 3x3 area around the clicked spot is free from obstacles (black color)
        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                int targetRow = row + r;
                int targetCol = col + c;

                // Skip if out of bounds
                if (targetRow < 0 || targetRow >= ROWS || targetCol < 0 || targetCol >= COLS) {
                    continue;
                }

                // Find the button and check its background color
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == targetRow &&
                                buttonCoords[1] == targetCol) {

                            // Check if the button's background is black (obstacle)
                            Drawable background = child.getBackground();
                            if (!(background instanceof ColorDrawable)) {
                                if (buttonCoords[2]==1) {
                                    return false; // Can't place car if there's an obstacle
                                }
                            }
                        }
                    }
                }
            }
        }
        return true; // If no obstacles were found, return true
    }

    // Method to check if the obstacle can be placed at the given coordinates
    private boolean canPlaceObstacle(int[] obstacleCoordinates) {
        int row = obstacleCoordinates[0];
        int col = obstacleCoordinates[1];


        // Skip if out of bounds
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return false;
        }

        // Find the button and check its background color
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child instanceof Button) {
                int[] buttonCoords = (int[]) child.getTag();
                if (buttonCoords != null &&
                        buttonCoords[0] == row &&
                        buttonCoords[1] == col) {

                    // Check if the button's background is orange (car)
                    Drawable background = child.getBackground();
                    if (background instanceof ColorDrawable) {
                        int color = ((ColorDrawable) background).getColor();
                        if (color == getResources().getColor(android.R.color.holo_orange_light)) {
                            return false; // Can't place obstacle if there's a car
                        }
                    }
                }
            }
        }


        return true; // If no cars were found, return true
    }

    private void moveCar(int direction) {
        if (lastCarCoordinates == null) {
            return; // No car placed yet
        }

        int currentRow = lastCarCoordinates[0];
        int currentCol = lastCarCoordinates[1];


        // Calculate the new position based on the rotation
        int newRow = currentRow;
        int newCol = currentCol;

        switch (Math.floorMod(Math.round(triangleRotation), 360)) {
            case 0: // North
                newRow = currentRow + direction;
                break;
            case 90: // East
                newCol = currentCol + direction;
                break;
            case 180: // South
                newRow = currentRow - direction;
                break;
            case 270: // West
                newCol = currentCol - direction;
                break;
        }

        // Check if the new position is valid using canPlaceCar()
        int[] newCarCoordinates = {newRow, newCol};
        if (canPlaceCar(newCarCoordinates)) {
            // Reset the current car placement
            resetCarPlacement(lastCarCoordinates);

            // Update the car's position
            lastCarCoordinates[0] = newRow;
            lastCarCoordinates[1] = newCol;

            // Place the car at the new position
            placeCarAt(newRow, newCol);

            // Optional: Show a toast with the new position
            String message = String.format("Car moved to: (%d, %d)", newCol, newRow);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

            // Send a message to the rpi with the move instruction
            switch (Math.floorMod(Math.round(triangleRotation), 360)) {
                case 0: // North
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "NORTH");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    // TO ADD X AND Y COORDS INTO THE JSON TO SEND ALSO
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 90: // East
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "EAST");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 180: // South
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "SOUTH");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 270: // West
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "WEST");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
            }
            carLogAdapter.addLog(String.format("Move to [%d,%d]", newCol, newRow));
        } else {
            // Collision detected, show a warning
            Toast.makeText(getContext(), "Cannot move forward (collision detected)", Toast.LENGTH_SHORT).show();
        }
    }

    private void rotateCar(int turnDirection) {
        if (lastCarCoordinates == null) {
            return; // No car placed yet
        }

        // Validate turnDirection input
        if (turnDirection != 1 && turnDirection != -1) {
            throw new IllegalArgumentException("Invalid turn direction. Use 1 for right or -1 for left.");
        }

        int currentRow = lastCarCoordinates[0];
        int currentCol = lastCarCoordinates[1];

        // Calculate the new position based on the turn direction and current orientation
        int newRow = currentRow;
        int newCol = currentCol;

        switch (Math.floorMod(Math.round(triangleRotation), 360)) {
            case 0: // North
                if (turnDirection == 1) {
                    newRow += 1; // Turn right (East)
                    newCol += 1;
                } else {
                    newRow += 1; // Turn left (West)
                    newCol -= 1;
                }
                break;
            case 90: // East
                if (turnDirection == 1) {
                    newRow -= 1; // Turn right (South)
                    newCol += 1;
                } else {
                    newRow += 1; // Turn left (North)
                    newCol += 1;
                }
                break;
            case 180: // South
                if (turnDirection == 1) {
                    newRow -= 1; // Turn right (West)
                    newCol -= 1;
                } else {
                    newRow -= 1; // Turn left (East)
                    newCol += 1;
                }
                break;
            case 270: // West
                if (turnDirection == 1) {
                    newRow += 1; // Turn right (North)
                    newCol -= 1;
                } else {
                    newRow -= 1; // Turn left (South)
                    newCol -= 1;
                }
                break;
        }

        // Check if the new position is valid using canPlaceCar()
        int[] newCarCoordinates = {newRow, newCol};
        if (canPlaceCar(newCarCoordinates)) {
            // Reset the current car placement
            resetCarPlacement(lastCarCoordinates);

            // Update the car's position
            lastCarCoordinates[0] = newRow;
            lastCarCoordinates[1] = newCol;

            // Update the car's rotation
            triangleRotation += (turnDirection == 1) ? 90 : -90; // Turn right or left
            triangleRotation = Math.floorMod(Math.round(triangleRotation), 360); // Normalize rotation

            // Place the car at the new position
            placeCarAt(newRow, newCol);

            // Update the triangle rotation visual
            updateTriangleRotation();

            // Update the TextView with the new direction
            updateTextView();

            switch (Math.floorMod(Math.round(triangleRotation), 360)) {
                case 0: // North
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "NORTH");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    // TO ADD X AND Y COORDS INTO THE JSON TO SEND ALSO
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 90: // East
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "EAST");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 180: // South
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "SOUTH");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
                case 270: // West
                    instructionToSend = new Intent("SendInstruction");
                    instructionToSend.putExtra("type", "MOVE-CAR");
                    instructionToSend.putExtra("direction", "WEST");
                    instructionToSend.putExtra("carXAxis", String.valueOf(newCol));
                    instructionToSend.putExtra("carYAxis", String.valueOf(newRow));
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);
                    break;
            }

//            // Send a message to the RPi with the rotation instruction
//            Intent instructionToSend = new Intent("SendInstruction");
//            instructionToSend.putExtra("type", "ROTATE-CAR");
//            instructionToSend.putExtra("direction", (turnDirection == 1) ? "RIGHT" : "LEFT");
//            instructionToSend.putExtra("rotation", triangleRotation);
//            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(instructionToSend);

            // Log the rotation
            carLogAdapter.addLog(String.format("Rotated to [%d,%d]", newCol, newRow));
        } else {
            // Collision detected, show a warning
            Toast.makeText(getContext(), "Cannot rotate (collision detected)", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeCarAt(int row, int col) {
        // Calculate the starting row and column for the 3x3 grid
        int startRow = row - 1;
        int startCol = col - 1;

        // Ensure the grid does not go out of bounds
        if (startRow < 0) startRow = 0;
        if (startCol < 0) startCol = 0;

        if (startRow + 3 > ROWS) {
            startRow = ROWS - 3;
        }
        if (startCol + 3 > COLS) {
            startCol = COLS - 3;
        }


        // Loop through the 3x3 area and update the buttons' background
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int targetRow = startRow + r;
                int targetCol = startCol + c;

                // Find the button at the target position
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == targetRow &&
                                buttonCoords[1] == targetCol) {

                            if (r == 1 && c == 1) { // Center cell
                                // Create the orange background and triangle
                                ShapeDrawable orangeBackground = new ShapeDrawable();
                                orangeBackground.getPaint().setColor(getResources().getColor(android.R.color.holo_orange_light));

                                ShapeDrawable triangle = createTriangleDrawable(child.getWidth(), child.getHeight());

                                Drawable[] layers = new Drawable[]{orangeBackground, triangle};
                                LayerDrawable layerDrawable = new LayerDrawable(layers);

                                child.setBackground(layerDrawable);
                                child.setPadding(0, 0, 0, 0);

                            } else { // Other cells
                                child.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                            }
                        }
                    }
                }
            }
        }
    }

    private BroadcastReceiver mObstacleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve the obstacle data from the received intent
            int obstacleID = intent.getIntExtra("obstacleID", -1);  // Default value is -1 if no data is found
            String obstacleText = intent.getStringExtra("obstacleText");

            // Log the received obstacle data
            Log.d(TAG, "Received OBSTACLE-INCOMING - ID: " + obstacleID + ", Text: " + obstacleText);

            // Update the UI or process the data as needed
            getObstacleUpdate(obstacleID, obstacleText);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // Register the receiver to listen for the "obstacleUpdate" intent
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mObstacleUpdateReceiver, new IntentFilter("obstacleUpdate"));
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister the receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mObstacleUpdateReceiver);
    }

    private void getObstacleUpdate(int obstacleID, String obstacleText) {
        // Update the UI with the received obstacle data
        Log.d(TAG, "Updating UI with ID: " + obstacleID + ", Text: " + obstacleText);
        Button button = findButtonByObstacleID(obstacleID);
        if (button != null) {
            int fixedWidth = button.getWidth();
            int fixedHeight = button.getHeight();

            button.setText(obstacleText);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11); // Set text size in SP

            // Prevent text wrapping
            button.setSingleLine(true);
            button.setEllipsize(TextUtils.TruncateAt.END);

            // Lock the button's size to prevent resizing
            ViewGroup.LayoutParams params = button.getLayoutParams();
            params.width = fixedWidth;
            params.height = fixedHeight;
            button.setLayoutParams(params);

            // Ensure text is centered
            button.setIncludeFontPadding(false);
            button.setGravity(Gravity.CENTER);

            // Log update
            btnLogAdapter.addLog(String.format("ID: %d, Text Change to: %s", obstacleID, obstacleText));

        } else {
            Log.d(TAG, "Button with obstacle ID " + obstacleID + " not found.");
        }
    }

    private Button findButtonByObstacleID(int obstacleNumber) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View view = gridLayout.getChildAt(i);
            if (view instanceof Button) {
                Button button = (Button) view;
                // Retrieve the tag and check the obstacle number (index 5 in the tag)
                int[] tag = (int[]) button.getTag();
                if (tag != null && tag.length > 5 && tag[5] == obstacleNumber) {
                    return button; // Return the button with the matching obstacle number
                }
            }
        }
        return null; // Return null if no button with the given obstacle number is found
    }

    private void startRobotTask()
    {

    }
}