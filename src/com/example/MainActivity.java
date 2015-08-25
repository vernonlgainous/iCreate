package com.example;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl.IOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.ButtonSprite.OnClickListener;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.Constants;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;

import android.opengl.GLES20;
import android.widget.Toast;

public class MainActivity extends SimpleBaseGameActivity implements OnClickListener {

	private static final float CAMERA_WIDTH = 480;
	private static final float CAMERA_HEIGHT = 320;
	private BoundCamera mBoundChaseCamera;
	private BuildableBitmapTextureAtlas mBuildableBitmapTextureAtlas;
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private ITiledTextureRegion mPlayerTextureRegion;
	private ITiledTextureRegion mEnemyTextureRegion;
	private ITextureRegion actionButtonTextureRegion;
	private BitmapTextureAtlas mOnScreenControlTexture;
	private TextureRegion mOnScreenControlBaseTextureRegion;
	private TextureRegion mOnScreenControlKnobTextureRegion;
	private TMXTiledMap mTMXTiledMap;

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.mBoundChaseCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mBoundChaseCamera);
		engineOptions.getTouchOptions().setNeedsMultiTouch(true);

		if(MultiTouch.isSupported(this)) {
			if(MultiTouch.isSupportedDistinct(this)) {
				Toast.makeText(this, "MultiTouch detected --> Both controls will work properly!", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "MultiTouch detected, but your device has problems distinguishing between fingers.\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, "Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.", Toast.LENGTH_LONG).show();
		}

		return engineOptions;
	}

	@Override
	public void onClick(ButtonSprite pButtonSprite, float pTouchAreaLocalX,
			float pTouchAreaLocalY) {
		runOnUiThread(new Runnable(){
			@Override
			public void run(){
				Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_LONG).show();
			}
		});
		
	}

	//This method does the job of loading/creating all of the resources that this project will use.
	@Override
	protected void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		//The player resources
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 72, 128, TextureOptions.BILINEAR);
		this.mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "player.png", 0, 0, 3, 4);
		this.mBitmapTextureAtlas.load();
		
		//The enemy resources
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 72, 128);
		this.mEnemyTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "enemy.png", 0, 0, 3, 4);
		this.mBitmapTextureAtlas.load();

		//The analog control stick resources
		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
		this.mOnScreenControlTexture.load();
		
		//The action button resources
		this.mBuildableBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 72, 128);
		this.actionButtonTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBuildableBitmapTextureAtlas, this, "onscreen_control_knob.png");
		
		try {
			this.mBuildableBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 0));
			this.mBuildableBitmapTextureAtlas.load();
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}
		
	}

	@Override
	protected Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());
		final Scene scene = new Scene();
		try {
			final TMXLoader tmxLoader = new TMXLoader(this.getAssets(), this.mEngine.getTextureManager(), TextureOptions.BILINEAR_PREMULTIPLYALPHA, this.getVertexBufferObjectManager(), new ITMXTilePropertiesListener() {
				@Override
				public void onTMXTileWithPropertiesCreated(final TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer, final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
				
				}
			});
			this.mTMXTiledMap = tmxLoader.loadFromAsset("tmx/desert.tmx");

			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "Welcome to a TMXTiled Map... This is the desert, not the dessert. ", Toast.LENGTH_LONG).show();
				}
			});
		} catch (final TMXLoadException e) {
			Debug.e(e);
		}
		final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(0);
		scene.attachChild(tmxLayer);
		
		//Make it so the camera will not exceed the bounds of the map
		this.mBoundChaseCamera.setBounds(0, 0, tmxLayer.getHeight(), tmxLayer.getWidth());
		this.mBoundChaseCamera.setBoundsEnabled(true);
		/* Calculate the coordinates for the player, so its centered on the camera. */
		final float centerX = (CAMERA_WIDTH - this.mPlayerTextureRegion.getWidth()) / 2;
		final float centerY = (CAMERA_HEIGHT - this.mPlayerTextureRegion.getHeight()) / 2;

		/* Create the sprite and add it to the scene. */
		final AnimatedSprite player = new AnimatedSprite(centerX, centerY, this.mPlayerTextureRegion, this.getVertexBufferObjectManager());
		this.mBoundChaseCamera.setChaseEntity(player);
		final PhysicsHandler physicsHandler = new PhysicsHandler(player);
		player.registerUpdateHandler(physicsHandler);
		
		scene.attachChild(player);
		
		//Putting the player on the TMX map and adding controls to the player.
		final DigitalOnScreenControl digitalOnScreenControl = new DigitalOnScreenControl(0, CAMERA_HEIGHT-this.mOnScreenControlBaseTextureRegion.getHeight(), this.mBoundChaseCamera, 
				this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new IOnScreenControlListener(){
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY){
				physicsHandler.setVelocity(pValueX * 100, pValueY * 100);
				player.animate(new long[]{200,200,200},6,8,true);
			}
			/* part of the analog impl.
			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl){
				player.registerEntityModifier(new SequenceEntityModifier(new ScaleModifier(0.25f, 1, 1.5f), new ScaleModifier(0.25f, 1.5f, 1)));
			}
			*/
		});
		
		//TODO: Remove the current tile highlighter for final product.
		final Rectangle currentTileRectangle = new Rectangle(0, 0, this.mTMXTiledMap.getTileWidth(), this.mTMXTiledMap.getTileHeight(), this.getVertexBufferObjectManager());
		currentTileRectangle.setColor(1,0,0,0.25f);
		scene.attachChild(currentTileRectangle);
		scene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				/* Get the scene-coordinates of the players feet. */
				final float[] playerFootCordinates = player.convertLocalToSceneCoordinates(12, 31);

				/* Get the tile the feet of the player are currently waking on. */
				final TMXTile tmxTile = tmxLayer.getTMXTileAt(playerFootCordinates[Constants.VERTEX_INDEX_X], playerFootCordinates[Constants.VERTEX_INDEX_Y]);
				if(tmxTile != null) {
					// tmxTile.setTextureRegion(null); <-- Rubber-style removing of tiles =D
					currentTileRectangle.setPosition(tmxTile.getTileX(), tmxTile.getTileY());
				}
			}
		});
		
		digitalOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		digitalOnScreenControl.getControlBase().setAlpha(0.5f);
		digitalOnScreenControl.getControlBase().setScaleCenter(0, 128);
		digitalOnScreenControl.getControlBase().setScale(1.25f);
		digitalOnScreenControl.getControlKnob().setScale(1.25f);
		digitalOnScreenControl.refreshControlKnobPosition();
		//scene.setChildScene(digitalOnScreenControl);
		
		//TODO modify action button location to not use a hard value position.
		final ButtonSprite actionButton = new ButtonSprite(centerX+200, centerY+110, this.actionButtonTextureRegion, this.getVertexBufferObjectManager(), this);
		/*scene.attach child will make the sprite part of the scene not part of the HUD.
		  scene.attachChild(actionButton);
		  scene.setTouchAreaBindingOnActionDownEnabled(true);*/
		//This is the correct and proper way to add buttons to the user HUD
		HUD gameHud = new HUD();
		gameHud.registerTouchArea(actionButton);
		gameHud.attachChild(actionButton);
		gameHud.setChildScene(digitalOnScreenControl);
		mBoundChaseCamera.setHUD(gameHud);
		
		//TODO: add an enemy or some sort of objective.
		final AnimatedSprite enemySprite = new AnimatedSprite(23, 24, this.mEnemyTextureRegion, this.getVertexBufferObjectManager());
		scene.attachChild(enemySprite);
		
		//adding collision detection to the player and enemy. Now need to clear that collision data
		scene.registerUpdateHandler(new IUpdateHandler(){
			@Override
			public void reset() {}
			
			@Override
			public void onUpdate(final float pSecondsElapsed){
				Color color = player.getColor();
				if (enemySprite.collidesWith(player)){
					//TODO: add functionality for losing health.
				}
				//add the case where the player is pressing the button and it collides with the enemy.
				if (enemySprite.collidesWith(currentTileRectangle)&& actionButton.isPressed()){
					enemySprite.animate(200,3);
				}
				else if (enemySprite.collidesWith(player) && enemySprite.isAnimationRunning() && actionButton.isPressed()){
					enemySprite.stopAnimation();
				}
				if (!enemySprite.collidesWith(player)){
					player.setColor(color);
				}
			}
		});
		
		
		return scene;
	}

}
