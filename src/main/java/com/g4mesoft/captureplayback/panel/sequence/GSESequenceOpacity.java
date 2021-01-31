package com.g4mesoft.captureplayback.panel.sequence;

public enum GSESequenceOpacity {

	OPACITY_25(0, "panel.sequence.opacity25", 0.25f),
	OPACITY_50(1, "panel.sequence.opacity50", 0.50f),
	OPACITY_75(2, "panel.sequence.opacity75", 0.75f),
	OPACITY_90(3, "panel.sequence.opacity90", 0.90f),
	FULLY_OPAQUE(4, "panel.sequence.opacity100", 1.0f);
	
	public static final GSESequenceOpacity[] OPACITIES;
	
	static {
		OPACITIES = new GSESequenceOpacity[values().length];
		for (GSESequenceOpacity opacity : values()) {
			if (OPACITIES[opacity.index] != null)
				throw new ExceptionInInitializerError("Duplicate opacity index");
			OPACITIES[opacity.index] = opacity;
		}
	}
	
	private final int index;
	private final String name;
	private final float opacity;
	
	private GSESequenceOpacity(int index, String name, float opacity) {
		this.index = index;
		this.name = name;
		this.opacity = opacity;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getName() {
		return name;
	}
	
	public float getOpacity() {
		return opacity;
	}
	
	public static GSESequenceOpacity fromIndex(int index) {
		if (index < 0 || index >= OPACITIES.length)
			return null;
		return OPACITIES[index];
	}
}
