package com.sudowilliam.flappypix;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class FlappyPix extends ApplicationAdapter {
	private static final float PLAYER_JUMP_IMPULSE = 350;
	private static final float PLAYER_VELOCITY_X = 200;
	private static final float PLAYER_START_X = 50;
	private static final float PLAYER_START_Y = 240;
	private static final float GRAVITY = -20;

	private boolean isDown = false;

	ShapeRenderer shapeRenderer;
	SpriteBatch batch;
	OrthographicCamera camera;
	OrthographicCamera uiCamera;

	Texture background;

	TextureRegion ground;
	float groundOffsetX = 0;

	TextureRegion ceilling;
	TextureRegion building;
	
	TextureRegion gameReady;
	TextureRegion gameOver;
	TextureRegion startAgain;

	TextureRegion buildingDown;
	
	BitmapFont font;

	Animation player;
	float playerStateTime = 0;

	Vector2 playerPosition = new Vector2();
	Vector2 playerVelocity = new Vector2();
	Vector2 gravity = new Vector2();

	Array<Building> buildings = new Array<Building>();

	GameState gameState = GameState.Start;
	float gameOverDelay = 0.5f;

	Rectangle playerCollision = new Rectangle();
	Rectangle buildingCollision = new Rectangle();
	
	int score = 0;
	
	Music music;
	Sound jump;

	@Override
	public void create () {
		shapeRenderer = new ShapeRenderer();
		batch = new SpriteBatch();

		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);
		
		uiCamera = new OrthographicCamera();
		uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		uiCamera.update();
		
		font = new BitmapFont(Gdx.files.internal("font.fnt"));

		background = new Texture("background.png");
		ground = new TextureRegion(new Texture("ground.png"));

		building = new TextureRegion(new Texture("building.png"));
		buildingDown = new TextureRegion(building);
		buildingDown.flip(false, true);

		ceilling = new TextureRegion(ground);
		ceilling.flip(true, true);
		
		gameReady = new TextureRegion(new Texture("ready.png"));
		gameOver = new TextureRegion(new Texture("gameover.png"));
		startAgain = new TextureRegion(new Texture("start_again.png"));

		Texture frame1 = new Texture("player1.png");
		frame1.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		Texture frame2 = new Texture("player2.png");
		Texture frame3 = new Texture("player3.png");
		Texture frame4 = new Texture("player4.png");

		player = new Animation(0.5f,
                               new TextureRegion(frame1),
                               new TextureRegion(frame2),
                               new TextureRegion(frame4),
                               new TextureRegion(frame3));

		player.setPlayMode(PlayMode.LOOP);
		
		music = Gdx.audio.newMusic(Gdx.files.internal("gameplay_song.mp3"));
		music.setLooping(true);
		music.setVolume(0.15f);
		music.play();
		
		jump = Gdx.audio.newSound(Gdx.files.internal("flap.wav"));

		resetWorld();
	}
	
	private void resetWorld() {
		score = 0;
		groundOffsetX = 0;

		playerPosition.set(PLAYER_START_X, PLAYER_START_Y);
		playerVelocity.set(0, 0);

		gravity.set(0, GRAVITY);

		camera.position.x = 400;

		buildings.clear();

		for (int i = 0; i < 5; i++) {
			isDown = !isDown;
			buildings.add(new Building(700 + i * 200, isDown ? 480 - building.getRegionHeight() : 0, isDown ? buildingDown : building));
		}
	}
	
	private void updateWorld() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		playerStateTime += deltaTime;

		if (Gdx.input.justTouched()) {
			if (gameState == GameState.Start) {
				gameState = GameState.Running;
			}

			if (gameState == GameState.Running) {
				playerVelocity.set(PLAYER_VELOCITY_X, PLAYER_JUMP_IMPULSE);
				long jumpId = jump.play();
				jump.setVolume(jumpId, 10f);
			}
			
			if (gameState == GameState.GameOver) {
				Timer.schedule(new Task() {
					@Override
					public void run() {
						gameState = GameState.Start;
						resetWorld();
					}
				}, gameOverDelay);
			}
		}
		
		if (gameState != GameState.Start) playerVelocity.add(gravity);

		playerPosition.mulAdd(playerVelocity, deltaTime);

		camera.position.x = playerPosition.x + 350;
		if (camera.position.x - groundOffsetX >  ground.getRegionWidth() + 400) {
			groundOffsetX += ground.getRegionWidth();
		}

		playerCollision.set(playerPosition.x, playerPosition.y,
				  player.getKeyFrames()[0].getRegionWidth(),
				  player.getKeyFrames()[0].getRegionHeight());

		for (Building b : buildings) {
			if (camera.position.x - b.position.x > 400 + b.image.getRegionWidth()) {
				isDown = !isDown;
				b.position.x += 5 * 200;
				b.position.y = isDown ? 480 - building.getRegionHeight() : 0;
				b.image = isDown ? buildingDown : building;
				b.counted = false;
			}
			
			buildingCollision.set(b.position.x + (b.image.getRegionWidth() - 64) / 2, b.position.y, 64, b.image.getRegionHeight());
			if (playerCollision.overlaps(buildingCollision)) {
				gameState = GameState.GameOver;
				playerVelocity.x = 0;
			}
			
			if (b.position.x < playerPosition.x && !b.counted) {
				score++;
				b.counted = true;
			}
		}

		if (playerPosition.y < ground.getRegionHeight() - 5 ||
			playerPosition.y + player.getKeyFrames()[0].getRegionHeight() > 480 - ground.getRegionHeight() + 5) {
			gameState = GameState.GameOver;
			playerVelocity.x = 0;
		}
	}
	
	private void drawWorld() {
		camera.update();

		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		batch.draw(background, camera.position.x - background.getWidth() / 2, 0);

		for (Building building : buildings) {
			batch.draw(building.image, building.position.x, building.position.y);
		}

		batch.draw(ground, groundOffsetX, 0);
		batch.draw(ground, groundOffsetX + ground.getRegionWidth(), 0);

		batch.draw(ceilling, groundOffsetX, 480 - ceilling.getRegionHeight());
		batch.draw(ceilling, groundOffsetX + ceilling.getRegionWidth(), 480 - ceilling.getRegionHeight());

		batch.draw(player.getKeyFrame(playerStateTime), playerPosition.x, playerPosition.y);
		
		batch.end();
		
		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();
		
		if (gameState == GameState.Start) {
			batch.draw(gameReady, 400 - gameReady.getRegionWidth() / 2, 240 - gameReady.getRegionHeight() / 2);
		}
		
		if (gameState == GameState.GameOver) {
			batch.draw(gameOver, uiCamera.position.x - gameOver.getRegionWidth() / 2, 240 - gameOver.getRegionHeight() / 2);
			batch.draw(startAgain, uiCamera.position.x - startAgain.getRegionWidth() / 2, 180 - startAgain.getRegionHeight() / 2);
		}

		if (gameState == GameState.GameOver || gameState == GameState.Running) {
			font.draw(batch, "" + score, uiCamera.position.x, uiCamera.position.y + 150);
		}

		batch.end();
		
		// debugDraw();
	}
	
	private void debugDraw() {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Line);

		shapeRenderer.setColor(Color.RED);

		playerCollision.set(playerPosition.x, playerPosition.y,
				  player.getKeyFrames()[0].getRegionWidth(),
				  player.getKeyFrames()[0].getRegionHeight());

		shapeRenderer.rect(playerCollision.x, playerCollision.y, playerCollision.width, playerCollision.height);

		for (Building b : buildings) {
			buildingCollision.set(b.position.x + (b.image.getRegionWidth() - 64) / 2, b.position.y, 64, b.image.getRegionHeight());
			shapeRenderer.rect(buildingCollision.x, buildingCollision.y, buildingCollision.width, buildingCollision.height);
		}

		shapeRenderer.end();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		updateWorld();
		drawWorld();
	}
}
