package com.sample.argame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Scene scene;
    private Camera camera;
    private ModelRenderable bulletRenderable;
    private boolean timeStart = false;
    private int fireLeft = 20;
    private TextView fireLeftText;
    private Point point;
    private SoundPool soundPool;
    private int sound;

    // UI Elements
    private RelativeLayout gameUI;
    private RelativeLayout startScreen;
    private Button startButton;
    private ImageView fireButton;
    private ImageView exitButton;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize display metrics
        Display display = getWindowManager().getDefaultDisplay();
        point = new Point();
        display.getRealSize(point);

        // Initialize UI components
        initializeUI();

        // Setup AR scene
        setupARScene();

        // Load game assets
        loadGameAssets();

        // Set up button listeners
        setupButtonListeners();
    }

    private void initializeUI() {
        gameUI = findViewById(R.id.gameUI);
        startScreen = findViewById(R.id.startScreen);
        startButton = findViewById(R.id.startButton);
        fireLeftText = findViewById(R.id.countfire);
        fireButton = findViewById(R.id.fire);
        exitButton = findViewById(R.id.exit);

        // Hide game UI initially
        gameUI.setVisibility(View.GONE);
        startScreen.setVisibility(View.VISIBLE);
    }

    private void setupARScene() {
        myFragment fragment = (myFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        scene = fragment.getArSceneView().getScene();
        camera = scene.getCamera();
    }

    private void loadGameAssets() {
        builtBullet();
        loadSoundPool();
    }

    private void setupButtonListeners() {
        startButton.setOnClickListener(view -> startGame());

        fireButton.setOnClickListener(view -> {
            if (!timeStart) {
                startTimer();
                timeStart = true;
            }
            shoot();
        });

        exitButton.setOnClickListener(view -> exitGame());
    }

    private void startGame() {
        // Hide start screen and show game UI
        startScreen.setVisibility(View.GONE);
        gameUI.setVisibility(View.VISIBLE);

        // Spawn enemies
        setEnemyToTheScene();

        // Reset game state
        fireLeft = 20;
        fireLeftText.setText("Left : " + fireLeft);
        timeStart = false;
    }

    private void exitGame() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    private void loadSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build();
        sound = soundPool.load(this, R.raw.blop_sound, 1);
    }

    private void shoot() {
        if (fireLeft <= 0) return;

        Ray ray = camera.screenPointToRay(point.x / 2f, point.y / 2f);
        Node node = new Node();
        node.setRenderable(bulletRenderable);
        scene.addChild(node);

        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                int finalI = i;
                runOnUiThread(() -> {
                    Vector3 v = ray.getPoint(finalI * .2f);
                    node.setWorldPosition(v);
                    Node nodeContact = scene.overlapTest(node);
                    if (nodeContact != null) {
                        fireLeft--;
                        fireLeftText.setText("Left : " + fireLeft);
                        scene.removeChild(nodeContact);
                        scene.removeChild(node);
                        soundPool.play(sound, 1, 1, 1, 0, 1);
                    }
                });
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> scene.removeChild(node));
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void startTimer() {
        new Thread(() -> {
            int seconds = 0;
            while (fireLeft > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;
                // Timer display logic can be added here if needed
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setEnemyToTheScene() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Untitled.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    for (int i = 0; i < 20; i++) {
                        Node node = new Node();
                        node.setRenderable(modelRenderable);
                        Random random = new Random();
                        float x = random.nextInt(8) - 4f;
                        float y = random.nextInt(2);
                        float z = random.nextInt(4);

                        Vector3 position = new Vector3(x, y, -z - 5f);
                        node.setWorldPosition(position);
                        node.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 230));
                        scene.addChild(node);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void builtBullet() {
        Texture.builder()
                .setSource(this, R.drawable.texture)
                .build()
                .thenAccept(texture -> {
                    MaterialFactory.makeOpaqueWithTexture(this, texture)
                            .thenAccept(material -> {
                                bulletRenderable = ShapeFactory.makeSphere(
                                        0.02f,
                                        new Vector3(0f, 0f, 0f),
                                        material
                                );
                            });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}