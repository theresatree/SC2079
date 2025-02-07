package com.example.sc2079;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.gridlayout.widget.GridLayout;


import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeSet;

public class GridFragment extends Fragment {
    public static final String TAG = "GridFragment";
    private TextView textViewDirection;

    private static final int ROWS = 20;
    private static final int COLS = 15;
    private boolean isSettingObstacle = false;
    private boolean isSettingCar = false;
    private boolean isDragging = false;
    private Button draggedObstacle;
    private boolean carSet = false;
    private float initialTouchX, initialTouchY;
    private int originalRow, originalCol;
    private float triangleRotation = 0f;
    private int obstacleCounter = 1;
    private TreeSet<Integer> removedObstacleNumbers = new TreeSet<>();// This is to help us check obstacle number.
    private GridLayout gridLayout;

    // Store the coordinates of the previously placed car's 3x3 grid
    private int[] lastCarCoordinates = null;

    public GridFragment() {
        // Required empty public constructor
    }

    public static GridFragment newInstance(String param1, String param2) {
        GridFragment fragment = new GridFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

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

        textViewDirection = root.findViewById(R.id.textViewDirection);

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
                triangleRotation = (triangleRotation - 90f) % 360f;  // decrement rotation by 90 degrees
                if (triangleRotation < 0) {
                    triangleRotation += 360f;  // Ensure the rotation value stays within 0-360 degrees range
                }
                updateTextView();
                updateTriangleRotation(); // Update the triangle in the grid
            }
        });
        btnRight.setOnClickListener(v -> {
            if (carSet) {
                triangleRotation = (triangleRotation + 90f) % 360f;  // Increment rotation by 90 degrees
                if (triangleRotation >= 360) {
                    triangleRotation -= 360f;  // Ensure the rotation value stays within 0-360 degrees range
                }
                updateTextView();
                updateTriangleRotation(); // Update the triangle in the grid
            }
        });

        gridLayout = root.findViewById(R.id.gridLayout);
        gridLayout.setColumnCount(COLS);
        gridLayout.setRowCount(ROWS);
        gridLayout.setUseDefaultMargins(false);

        populateGrid();

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

        return root;
    }

    private void populateGrid() {
        // Create buttons grid - starting from bottom-left
        for (int row = ROWS - 2; row >= 0; row--){
            for (int col = 0; col < COLS; col++) {
                Button button = new Button(getContext());

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                int cellSize = 22;
                int widthInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);
                int heightInPixels = (int) (getResources().getDisplayMetrics().density * cellSize);
                params.width = widthInPixels;
                params.height = heightInPixels;
                params.setMargins(0, 0, 0, 0);

                params.rowSpec = GridLayout.spec(ROWS - 2 - row);
                params.columnSpec = GridLayout.spec(col);

                button.setLayoutParams(params);
                button.setBackgroundResource(R.drawable.button_border_color);
                button.setPadding(0, 0, 0, 0);

                final int finalRow = row;
                final int finalCol = col;

                button.setTag(new int[]{finalRow, finalCol}); // Store coordinates in button's tag

                button.setOnClickListener(v -> {
                    if (isSettingObstacle) {
                        int[] coords = (int[]) button.getTag();
                        if (!canPlaceObstacle(coords)) {
                            Toast.makeText(getContext(), "Can't place obstacle here", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int currentRow = coords[0];
                        int currentCol = coords[1];
                        Drawable background = button.getBackground();
                        if (background instanceof ColorDrawable) {
                            int currentColor = ((ColorDrawable) background).getColor();
                            if (currentColor == getResources().getColor(android.R.color.black)) {
                                button.setBackgroundResource(R.drawable.button_border_color);
                                button.setText(""); // Clear the number when removing obstacle
                            } else {
                                button.setBackgroundColor(getResources().getColor(android.R.color.black));
                                button.setText(String.valueOf(obstacleCounter++)); // Add number to obstacle
                                button.setTextColor(getResources().getColor(android.R.color.white)); // Make text white for visibility
                            }
                        } else {
                            button.setBackgroundColor(getResources().getColor(android.R.color.black));
                            if (!removedObstacleNumbers.isEmpty()) {
                                button.setText(String.valueOf(removedObstacleNumbers.pollFirst()));
                                Log.d(TAG, String.valueOf(removedObstacleNumbers));
                            } else {
                                button.setText(String.valueOf(obstacleCounter++)); // Add number to obstacle
                            }
                            button.setTextColor(getResources().getColor(android.R.color.white)); // Make text white for visibility
                            String message = String.format("Obstacle %d Placed: (%d,%d)", obstacleCounter-1, currentCol, currentRow);
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    } else if (isSettingCar) {
                        int[] coords = (int[]) button.getTag();
                        if (!canPlaceCar(coords)) {
                            Toast.makeText(getContext(), "Can't place car here (obstacle present)", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        carSet = true;

                        // If there was a previous car placement, reset it
                        if (lastCarCoordinates != null) {
                            resetCarPlacement(lastCarCoordinates);
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
                        if (startRow + 3 > ROWS - 1) {
                            startRow = ROWS - 1 - 3;  // Make sure the grid stays within bounds
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

                        // Optional: Show the coordinates of the car placement in a toast
                        StringBuilder carCoordinates = new StringBuilder();
                        for (int r = 0; r < 3; r++) {
                            for (int c = 0; c < 3; c++) {
                                int targetRow = startRow + r;
                                int targetCol = startCol + c;
                                carCoordinates.append(String.format("(%d,%d) ", targetCol, targetRow));
                            }
                        }
                        String message = "Car placed at: " + carCoordinates.toString();
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
                    message = "North";
                    break;
                case 90:
                    message = "East";
                    break;
                case 180:
                    message = "South";
                    break;
                case 270:
                    message = "West";
                    break;
                default:
                    message = "Unknown";
                    break;
            }

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
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int targetRow = lastCarCoordinates[0] - 1 + r;
                int targetCol = lastCarCoordinates[1] - 1 + c;

                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == targetRow &&
                                buttonCoords[1] == targetCol) {

                            if (r == 1 && c == 1) { // Center cell
                                // Create a *new* LayerDrawable with rotated triangle
                                ShapeDrawable triangle = createTriangleDrawable(child.getWidth(), child.getHeight());
                                ShapeDrawable orangeBackground = new ShapeDrawable();
                                orangeBackground.getPaint().setColor(getResources().getColor(android.R.color.holo_orange_light));

                                Drawable[] layers = new Drawable[]{orangeBackground, triangle};
                                LayerDrawable layerDrawable = new LayerDrawable(layers);

                                child.setBackground(layerDrawable);
                                child.setPadding(0, 0, 0, 0);
                            }
                        }
                    }
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

        // Find drop location
        View newParent = findViewAtPosition(endX, endY);
        if (newParent instanceof Button && newParent != draggedObstacle) {
            Button newButton = (Button) newParent;

            // Check for collisions before moving the obstacle
            if (!isColliding(newButton)) {
                // Move obstacle to new position
                moveObstacleTo(newButton);
            } else {
                // Collision detected, reset the obstacle to its original position
                Toast.makeText(getContext(), "Collision detected! Cannot place obstacle here.", Toast.LENGTH_SHORT).show();
                draggedObstacle.setBackgroundColor(getResources().getColor(android.R.color.black)); // Reset to original state
            }
        } else {
            // Remove if dropped outside grid
            removeObstacle(draggedObstacle);
        }

        draggedObstacle = null;
    }

    private boolean isColliding(Button newButton) {
        int[] newCoords = (int[]) newButton.getTag();
        int newRow = newCoords[0];
        int newCol = newCoords[1];

        // Check if the new position overlaps with any existing obstacles
        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                int targetRow = newRow + r;
                int targetCol = newCol + c;

                // Skip if out of bounds
                if (targetRow < 0 || targetRow >= ROWS - 1 || targetCol < 0 || targetCol >= COLS) {
                    continue;
                }

                // Find the button at the target position
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    View child = gridLayout.getChildAt(i);
                    if (child instanceof Button) {
                        int[] buttonCoords = (int[]) child.getTag();
                        if (buttonCoords != null &&
                                buttonCoords[0] == targetRow &&
                                buttonCoords[1] == targetCol) {

                            // Check if the button is an obstacle or part of the car
                            Drawable background = child.getBackground();
                            if (background instanceof ColorDrawable) {
                                int color = ((ColorDrawable) background).getColor();
                                if (color == getResources().getColor(android.R.color.black) || // Obstacle
                                        color == getResources().getColor(android.R.color.holo_orange_light)) { // Car
                                    return true; // Collision detected
                                }
                            }
                        }
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
        Drawable background = button.getBackground();
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor() == getResources().getColor(android.R.color.black);
        }
        return false;
    }

    private void moveObstacleTo(Button newButton) {
        // Clear original position
        String obstacleNumber = draggedObstacle.getText().toString();
        draggedObstacle.setBackgroundResource(R.drawable.button_border_color);
        draggedObstacle.setText("");
        // Set new position
        newButton.setBackgroundColor(getResources().getColor(android.R.color.black));
        newButton.setText(obstacleNumber);
        newButton.setTextColor(getResources().getColor(android.R.color.white));

        // Send Bluetooth update
        int[] newCoords = (int[]) newButton.getTag();
        sendObstacleUpdate(originalRow, originalCol, newCoords[0], newCoords[1]);
    }

    private void removeObstacle(Button obstacle) {
        String obstacleNumberStr = obstacle.getText().toString();
        int obstacleNumber = Integer.parseInt(obstacleNumberStr);

        // Add the removed obstacle number to the TreeSet
        removedObstacleNumbers.add(obstacleNumber);

        obstacle.setBackgroundResource(R.drawable.button_border_color);
        obstacle.setText("");
        sendObstacleRemoval(originalRow, originalCol);
    }

    private void sendObstacleUpdate(int oldX, int oldY, int newX, int newY) {
        String message = String.format("Obstacle Moved: (%d,%d) to (%d,%d)", oldX, oldY, newX, newY);
        if (getContext() != null) {  // Ensure context is not null
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }        // Implement your Bluetooth sending logic here
    }

    private void sendObstacleRemoval(int x, int y) {
        String message = String.format("Obstacle Removed: (%d,%d)", x, y);
        if (getContext() != null) {  // Ensure context is not null
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        // Implement your Bluetooth sending logic here
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
        if (startRow + 3 > ROWS - 1) {
            startRow = ROWS - 1 - 3;  // Make sure the grid stays within bounds
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
                if (targetRow < 0 || targetRow >= ROWS - 1 || targetCol < 0 || targetCol >= COLS) {
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

        // Check if the 3x3 area around the clicked spot is free from obstacles (black color)
        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                int targetRow = row + r;
                int targetCol = col + c;

                // Skip if out of bounds
                if (targetRow < 0 || targetRow >= ROWS - 1 || targetCol < 0 || targetCol >= COLS) {
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
                            if (background instanceof ColorDrawable) {
                                int color = ((ColorDrawable) background).getColor();
                                if (color == getResources().getColor(android.R.color.black)) {
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


        // Check if the 3x3 area around the clicked spot is free from cars (orange color)
        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                int targetRow = row + r;
                int targetCol = col + c;

                // Skip if out of bounds
                if (targetRow < 0 || targetRow >= ROWS - 1 || targetCol < 0 || targetCol >= COLS) {
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
        } else {
            // Collision detected, show a warning
            Toast.makeText(getContext(), "Cannot move forward (collision detected)", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeCarAt(int row, int col) {
        // Calculate the starting row and column for the 3x3 grid
        int startRow = row - 1;
        int startCol = col - 1;

        // Ensure the grid does not go out of bounds
        if (startRow < 0) startRow = 0;
        if (startCol < 0) startCol = 0;

        if (startRow + 3 > ROWS - 1) {
            startRow = ROWS - 1 - 3;
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
}