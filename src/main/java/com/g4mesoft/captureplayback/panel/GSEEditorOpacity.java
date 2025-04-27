package com.g4mesoft.captureplayback.panel;

public enum GSEEditorOpacity {

	OPACITY_25(0, "panel.opacity25", 0.25f),
	OPACITY_50(1, "panel.opacity50", 0.50f),
	OPACITY_75(2, "panel.opacity75", 0.75f),
	OPACITY_90(3, "panel.opacity90", 0.90f),
	FULLY_OPAQUE(4, "panel.opacity100", 1.0f);
	
	public static final GSEEditorOpacity[] OPACITIES;
	
	static {
		OPACITIES = new GSEEditorOpacity[values().length];
		for (GSEEditorOpacity opacity : values()) {
			if (OPACITIES[opacity.index] != null)
				throw new ExceptionInInitializerError("Duplicate opacity index");
			OPACITIES[opacity.index] = opacity;
		}
	}
	
	private final int index;
	private final String name;
	private final float opacity;
	
	private GSEEditorOpacity(int index, String name, float opacity) {
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
	
	public static GSEEditorOpacity fromIndex(int index) {
		if (index < 0 || index >= OPACITIES.length)
			return null;
		return OPACITIES[index];
	}
}
