package com.sudowilliam.flappypix;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Building {
	Vector2 position = new Vector2();
	TextureRegion image;
	boolean counted;
	
	public Building(float x, float y, TextureRegion image) {
		this.position.x = x;
		this.position.y = y;
		this.image = image;
	}
}
