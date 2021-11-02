package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.panel.scroll.GSAbstractScrollBarModel;

public class GSUnlimitedScrollBarModel extends GSAbstractScrollBarModel {

	private static final float DEFAULT_BLOCK_SCROLL = 20.0f;
	private static final float DEFAULT_MIN_SCROLL   = 0.0f;
	private static final float DEFAULT_MAX_SCROLL   = 100.0f;
	
	private float scroll;
	
	private float minScroll;
	private float maxScroll;
	private float blockScroll;
	
	public GSUnlimitedScrollBarModel() {
		this(DEFAULT_MIN_SCROLL, DEFAULT_MAX_SCROLL);
	}
	
	public GSUnlimitedScrollBarModel(float minScroll, float maxScroll) {
		this(minScroll, minScroll, maxScroll);
	}

	public GSUnlimitedScrollBarModel(float scroll, float minScroll, float maxScroll) {
		this.minScroll = Float.isNaN(minScroll) ? DEFAULT_MIN_SCROLL : minScroll;
		this.maxScroll = Float.isNaN(maxScroll) ? DEFAULT_MAX_SCROLL : maxScroll;
		this.blockScroll = DEFAULT_BLOCK_SCROLL;

		setScroll(scroll);
	}

	@Override
	public float getScroll() {
		return scroll;
	}

	@Override
	public void setScroll(float scroll) {
		if (Float.isNaN(scroll) || scroll < minScroll) {
			this.scroll = minScroll;
		} else {
			this.scroll = scroll;
		}
		
		dispatchScrollChanged(this.scroll);
		dispatchValueChanged();
	}

	@Override
	public float getMinScroll() {
		return minScroll;
	}

	@Override
	public void setMinScroll(float minScroll) {
		setScrollInterval(minScroll, maxScroll);
	}

	@Override
	public float getMaxScroll() {
		// This is where the unlimited comes in. The maximum scroll
		// will follow the actual scroll if it is exceeded by it.
		return Math.max(maxScroll, scroll);
	}

	@Override
	public void setMaxScroll(float maxScroll) {
		setScrollInterval(minScroll, maxScroll);
	}
	
	@Override
	public void setScrollInterval(float minScroll, float maxScroll) {
		this.minScroll = Float.isNaN(minScroll) ? DEFAULT_MIN_SCROLL : minScroll;
		this.maxScroll = Float.isNaN(maxScroll) ? DEFAULT_MAX_SCROLL : maxScroll;
		
		if (this.minScroll > scroll) {
			// Only bound scroll by the minimum scroll, not maximum.
			setScroll(this.minScroll);
		} else {
			// setScroll dispatches a value changed event
			dispatchValueChanged();
		}
	}

	@Override
	public float getBlockScroll() {
		return blockScroll;
	}
	
	@Override
	public void setBlockScroll(float blockScroll) {
		this.blockScroll = Math.max(0.0f, blockScroll);
	}
}
